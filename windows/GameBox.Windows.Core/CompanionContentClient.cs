using System.Net;
using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace GameBox.Windows.Core;

public sealed record CompanionContentUploadResult(
    string GameId,
    string FileName,
    long SizeBytes,
    string Sha256);

public sealed class CompanionContentClient
{
    public const long MaxContentBytes = 64L * 1024 * 1024 * 1024;
    private const int MaxResponseBytes = 64 * 1024;
    private readonly HttpClient client;
    private readonly TimeSpan timeout;

    public CompanionContentClient(HttpClient client, TimeSpan? timeout = null)
    {
        this.client = client ?? throw new ArgumentNullException(nameof(client));
        this.timeout = timeout ?? TimeSpan.FromHours(6);
        if (this.timeout <= TimeSpan.Zero) throw new ArgumentOutOfRangeException(nameof(timeout));
    }

    public async Task<CompanionContentUploadResult> UploadAsync(
        string host,
        int port,
        string pairingSecret,
        string gameId,
        string filePath,
        CancellationToken cancellationToken = default)
    {
        var file = new FileInfo(filePath ?? throw new ArgumentNullException(nameof(filePath)));
        if (!file.Exists) throw new FileNotFoundException("Selected game content does not exist.", file.FullName);
        if (file.Length is <= 0 or > MaxContentBytes)
            throw new InvalidDataException("Game content must contain 1 byte to 64 GiB.");

        var path = ContentPath(gameId);
        string hash;
        await using (var hashStream = new FileStream(
            file.FullName, FileMode.Open, FileAccess.Read, FileShare.Read,
            1024 * 1024, FileOptions.Asynchronous | FileOptions.SequentialScan))
        {
            hash = Convert.ToHexString(
                await SHA256.HashDataAsync(hashStream, cancellationToken).ConfigureAwait(false))
                .ToLowerInvariant();
        }

        using var request = new HttpRequestMessage(
            HttpMethod.Put,
            BuildUri(host, port, path));
        request.Headers.TryAddWithoutValidation(
            CompanionProtocol.AuthorizationHeader,
            CompanionProtocol.CreateAuthorization(
                pairingSecret, "PUT", path,
                DateTimeOffset.UtcNow.ToUnixTimeSeconds(), hash));
        request.Headers.TryAddWithoutValidation("X-GameBox-Content-SHA256", hash);
        request.Headers.TryAddWithoutValidation(
            "X-GameBox-File-Name",
            Convert.ToBase64String(Encoding.UTF8.GetBytes(file.Name)));

        await using var contentStream = new FileStream(
            file.FullName, FileMode.Open, FileAccess.Read, FileShare.Read,
            1024 * 1024, FileOptions.Asynchronous | FileOptions.SequentialScan);
        request.Content = new StreamContent(contentStream, 1024 * 1024);
        request.Content.Headers.ContentType = new MediaTypeHeaderValue("application/octet-stream");
        request.Content.Headers.ContentLength = file.Length;

        using var timeoutSource = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        timeoutSource.CancelAfter(timeout);
        using var response = await client.SendAsync(
            request, HttpCompletionOption.ResponseHeadersRead, timeoutSource.Token).ConfigureAwait(false);
        if (response.StatusCode == HttpStatusCode.Unauthorized)
            throw new UnauthorizedAccessException("GameBox rejected the pairing secret.");
        if (response.StatusCode == HttpStatusCode.RequestEntityTooLarge)
            throw new InvalidDataException("GameBox rejected content larger than 64 GiB.");
        if (!response.IsSuccessStatusCode)
            throw new InvalidDataException("GameBox rejected the game content (" + (int)response.StatusCode + ").");

        var declaredResponse = response.Content.Headers.ContentLength;
        if (declaredResponse is < 0 or > MaxResponseBytes)
            throw new InvalidDataException("GameBox returned an oversized content response.");
        var bytes = await response.Content.ReadAsByteArrayAsync(timeoutSource.Token).ConfigureAwait(false);
        if (bytes.Length > MaxResponseBytes)
            throw new InvalidDataException("GameBox returned an oversized content response.");
        JsonDocument document;
        try { document = JsonDocument.Parse(bytes); }
        catch (JsonException exception) { throw new InvalidDataException("GameBox returned invalid content JSON.", exception); }
        using (document)
        {
            var root = document.RootElement;
            if (root.GetProperty("protocolVersion").GetInt32() != CompanionProtocol.Version ||
                root.GetProperty("gameId").GetString() != gameId ||
                root.GetProperty("fileName").GetString() != file.Name ||
                root.GetProperty("sizeBytes").GetInt64() != file.Length ||
                !string.Equals(root.GetProperty("sha256").GetString(), hash, StringComparison.OrdinalIgnoreCase))
                throw new InvalidDataException("GameBox content confirmation did not match the selected file.");
        }
        return new CompanionContentUploadResult(gameId, file.Name, file.Length, hash);
    }

    private static string ContentPath(string gameId)
    {
        if (string.IsNullOrWhiteSpace(gameId) || gameId.Length > 96 ||
            !char.IsLetterOrDigit(gameId[0]) ||
            gameId.Any(static c => !(char.IsLower(c) || char.IsDigit(c) || c == '-')))
            throw new ArgumentException("Game ID is invalid.", nameof(gameId));
        return "/v1/content/" + gameId;
    }

    private static Uri BuildUri(string host, int port, string path)
    {
        if (string.IsNullOrWhiteSpace(host)) throw new ArgumentException("Host is required.", nameof(host));
        if (port is < 10240 or > 65535) throw new ArgumentOutOfRangeException(nameof(port));
        var normalized = host.Trim().Trim('[', ']');
        if (normalized.Contains('/') || normalized.Contains('\\') || normalized.Contains('?') || normalized.Contains('#'))
            throw new ArgumentException("Host must not include a scheme, path, or query.", nameof(host));
        return new UriBuilder(Uri.UriSchemeHttp, normalized, port, path).Uri;
    }
}
