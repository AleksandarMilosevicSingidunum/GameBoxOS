using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace GameBox.Windows.Core;

public sealed record CompanionDeviceConfiguration(
    bool ReducedMotion,
    bool ShowUnavailableGames,
    bool ShowUnavailableShortcuts,
    bool DownloadsUnmeteredOnly);

/// <summary>Authenticated read/write access to user-visible Android GameBox preferences.</summary>
public sealed class CompanionConfigurationClient
{
    public const string ConfigurationHeader = "X-GameBox-Configuration";
    public const string ContentSha256Header = "X-GameBox-Content-SHA256";
    private const string RequestPath = "/v1/config";
    private const int MaxResponseBytes = 32 * 1024;
    private readonly HttpClient client;
    private readonly TimeSpan timeout;

    public CompanionConfigurationClient(HttpClient client, TimeSpan? timeout = null)
    {
        this.client = client ?? throw new ArgumentNullException(nameof(client));
        this.timeout = timeout ?? TimeSpan.FromSeconds(10);
        if (this.timeout <= TimeSpan.Zero) throw new ArgumentOutOfRangeException(nameof(timeout));
    }

    public Task<CompanionDeviceConfiguration> GetWithRetryAsync(
        string host, int port, string pairingSecret, CancellationToken cancellationToken = default) =>
        CompanionRetryPolicy.ExecuteAsync(
            token => GetAsync(host, port, pairingSecret, token), cancellationToken);

    public async Task<CompanionDeviceConfiguration> GetAsync(
        string host, int port, string pairingSecret, CancellationToken cancellationToken = default)
    {
        using var request = CreateRequest(HttpMethod.Get, host, port, pairingSecret, null);
        return await SendAsync(request, cancellationToken).ConfigureAwait(false);
    }

    public async Task<CompanionDeviceConfiguration> PutAsync(
        string host, int port, string pairingSecret, CompanionDeviceConfiguration configuration,
        CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(configuration);
        var flags = Encode(configuration);
        using var request = CreateRequest(HttpMethod.Put, host, port, pairingSecret, flags);
        var confirmed = await SendAsync(request, cancellationToken).ConfigureAwait(false);
        if (confirmed != configuration)
            throw new InvalidDataException("GameBox returned a different device configuration.");
        return confirmed;
    }

    private static HttpRequestMessage CreateRequest(
        HttpMethod method, string host, int port, string pairingSecret, string? flags)
    {
        if (string.IsNullOrWhiteSpace(host)) throw new ArgumentException("Host is required.", nameof(host));
        if (port is < 10240 or > 65535) throw new ArgumentOutOfRangeException(nameof(port));
        var normalized = host.Trim().Trim('[', ']');
        if (normalized.Contains('/') || normalized.Contains('\\') || normalized.Contains('?') || normalized.Contains('#'))
            throw new ArgumentException("Host must not include a scheme, path, or query.", nameof(host));
        var request = new HttpRequestMessage(method, new UriBuilder(Uri.UriSchemeHttp, normalized, port, RequestPath).Uri);
        string? hash = null;
        if (flags is not null)
        {
            hash = Convert.ToHexString(SHA256.HashData(Encoding.ASCII.GetBytes(flags))).ToLowerInvariant();
            request.Headers.TryAddWithoutValidation(ConfigurationHeader, flags);
            request.Headers.TryAddWithoutValidation(ContentSha256Header, hash);
        }
        request.Headers.TryAddWithoutValidation(
            CompanionProtocol.AuthorizationHeader,
            CompanionProtocol.CreateAuthorization(
                pairingSecret, method.Method, RequestPath,
                DateTimeOffset.UtcNow.ToUnixTimeSeconds(), hash));
        return request;
    }

    private async Task<CompanionDeviceConfiguration> SendAsync(
        HttpRequestMessage request, CancellationToken cancellationToken)
    {
        using var timeoutSource = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        timeoutSource.CancelAfter(timeout);
        using var response = await client.SendAsync(
            request, HttpCompletionOption.ResponseHeadersRead, timeoutSource.Token).ConfigureAwait(false);
        if (response.StatusCode == HttpStatusCode.Unauthorized)
            throw new UnauthorizedAccessException("GameBox rejected the pairing secret.");
        if (!response.IsSuccessStatusCode)
            throw new InvalidDataException("GameBox rejected the device configuration request (" +
                (int)response.StatusCode + ").");
        await using var stream = await response.Content.ReadAsStreamAsync(timeoutSource.Token).ConfigureAwait(false);
        var bytes = await ReadBoundedAsync(stream, timeoutSource.Token).ConfigureAwait(false);
        try
        {
            using var document = JsonDocument.Parse(bytes);
            var root = document.RootElement;
            if (root.GetProperty("protocolVersion").GetInt32() != CompanionProtocol.Version)
                throw new InvalidDataException("GameBox returned an incompatible configuration protocol.");
            return new CompanionDeviceConfiguration(
                root.GetProperty("reducedMotion").GetBoolean(),
                root.GetProperty("showUnavailableGames").GetBoolean(),
                root.GetProperty("showUnavailableShortcuts").GetBoolean(),
                root.GetProperty("downloadsUnmeteredOnly").GetBoolean());
        }
        catch (JsonException exception)
        {
            throw new InvalidDataException("GameBox returned invalid configuration JSON.", exception);
        }
    }

    private static string Encode(CompanionDeviceConfiguration value) =>
        (value.ReducedMotion ? "1" : "0") +
        (value.ShowUnavailableGames ? "1" : "0") +
        (value.ShowUnavailableShortcuts ? "1" : "0") +
        (value.DownloadsUnmeteredOnly ? "1" : "0");

    private static async Task<byte[]> ReadBoundedAsync(Stream stream, CancellationToken cancellationToken)
    {
        using var output = new MemoryStream();
        var buffer = new byte[4096];
        while (true)
        {
            var read = await stream.ReadAsync(buffer, cancellationToken).ConfigureAwait(false);
            if (read == 0) return output.ToArray();
            if (output.Length + read > MaxResponseBytes)
                throw new InvalidDataException("GameBox configuration response exceeds 32 KiB.");
            await output.WriteAsync(buffer.AsMemory(0, read), cancellationToken).ConfigureAwait(false);
        }
    }
}
