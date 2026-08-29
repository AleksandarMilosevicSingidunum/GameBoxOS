using System.Net;
using System.Net.Http.Json;
using System.Text.Json;

namespace GameBox.Windows.Core;

public sealed record CatalogSyncResult(
    IReadOnlyList<GameEntry> Entries,
    int UpdatedCount,
    int IgnoredRemoteCount);

public sealed class WindowsCatalogSyncClient
{
    private const int MaxResponseBytes = 1_048_576;
    private static readonly JsonSerializerOptions JsonOptions = new() {
        PropertyNameCaseInsensitive = true
    };
    private readonly HttpClient _client;
    private readonly TimeSpan _requestTimeout;

    public WindowsCatalogSyncClient(HttpClient client, TimeSpan? requestTimeout = null)
    {
        _client = client;
        _requestTimeout = requestTimeout ?? TimeSpan.FromSeconds(15);
        if (_requestTimeout <= TimeSpan.Zero)
            throw new ArgumentOutOfRangeException(nameof(requestTimeout), "Catalog timeout must be positive.");
    }

    public async Task<CatalogSyncResult> EnrichExistingAsync(
        IEnumerable<GameEntry> existing,
        string endpoint,
        CancellationToken cancellationToken = default)
    {
        var uri = ValidateEndpoint(endpoint);
        using var timeoutCts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        timeoutCts.CancelAfter(_requestTimeout);
        var requestToken = timeoutCts.Token;
        using var response = await _client.GetAsync(uri, HttpCompletionOption.ResponseHeadersRead, requestToken);
        if (response.RequestMessage?.RequestUri is not null &&
            response.RequestMessage.RequestUri != uri)
            throw new InvalidDataException("Catalog redirects are not accepted.");
        if (!response.IsSuccessStatusCode)
            throw new HttpRequestException("Catalog request failed with HTTP " + (int)response.StatusCode);
        var declaredLength = response.Content.Headers.ContentLength;
        if (declaredLength is > MaxResponseBytes)
            throw new InvalidDataException("Catalog response is too large.");
        await using var stream = await response.Content.ReadAsStreamAsync(requestToken);
        using var buffer = new MemoryStream();
        await stream.CopyToAsync(buffer, requestToken);
        if (buffer.Length > MaxResponseBytes)
            throw new InvalidDataException("Catalog response is too large.");
        var envelope = JsonSerializer.Deserialize<RemoteEnvelope>(buffer.ToArray(), JsonOptions)
            ?? throw new InvalidDataException("Catalog response is empty.");
        var remote = envelope.Games ?? throw new InvalidDataException("Catalog games are missing.");
        var merged = GameLibrary.Normalize(existing).ToDictionary(x => x.Id, StringComparer.OrdinalIgnoreCase);
        var updated = 0;
        var ignored = 0;
        foreach (var item in remote)
        {
            ValidateRemote(item);
            if (!merged.TryGetValue(item.Id, out var local))
            {
                ignored++;
                continue;
            }
            var replacement = local with {
                Title = item.Title.Trim(),
                Platform = item.Platform.Trim()
            };
            if (replacement != local)
            {
                merged[item.Id] = replacement;
                updated++;
            }
        }
        return new CatalogSyncResult(GameLibrary.Normalize(merged.Values), updated, ignored);
    }

    private static Uri ValidateEndpoint(string endpoint)
    {
        if (!Uri.TryCreate(endpoint.Trim(), UriKind.Absolute, out var uri) ||
            !uri.Scheme.Equals(Uri.UriSchemeHttps, StringComparison.OrdinalIgnoreCase) ||
            string.IsNullOrWhiteSpace(uri.Host) || !string.IsNullOrEmpty(uri.UserInfo))
            throw new ArgumentException("Catalog endpoint must be HTTPS without embedded credentials.", nameof(endpoint));
        return uri;
    }

    private static void ValidateRemote(RemoteCatalogGame item)
    {
        if (string.IsNullOrWhiteSpace(item.Id) || string.IsNullOrWhiteSpace(item.Title) ||
            string.IsNullOrWhiteSpace(item.Platform))
            throw new InvalidDataException("Catalog entries require ID, title, and platform.");
        if (item.SourceUrl is not null &&
            (!Uri.TryCreate(item.SourceUrl, UriKind.Absolute, out var uri) ||
             !uri.Scheme.Equals(Uri.UriSchemeHttps, StringComparison.OrdinalIgnoreCase) ||
             !string.IsNullOrEmpty(uri.UserInfo)))
            throw new InvalidDataException("Catalog source URLs must use HTTPS without credentials.");
    }

    private sealed record RemoteEnvelope(List<RemoteCatalogGame>? Games);
    private sealed record RemoteCatalogGame(string Id, string Title, string Platform, string? SourceUrl = null);
}

public sealed class StubHttpMessageHandler : HttpMessageHandler
{
    private readonly Func<HttpRequestMessage, HttpResponseMessage> _handler;
    public StubHttpMessageHandler(Func<HttpRequestMessage, HttpResponseMessage> handler) => _handler = handler;
    protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken) =>
        Task.FromResult(_handler(request));
}
