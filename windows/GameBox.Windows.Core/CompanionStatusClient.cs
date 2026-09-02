using System.Net;
using System.Text.Json;

namespace GameBox.Windows.Core;

public sealed record CompanionDeviceStatus(int ProtocolVersion, string DeviceName, string Status);

/// <summary>
/// Authenticated client for the Android GameBox LAN companion status endpoint.
/// Pairing material is supplied by the caller and is never persisted or logged here.
/// </summary>
public sealed class CompanionStatusClient
{
    private const int MaxResponseBytes = 32 * 1024;
    private readonly HttpClient client;
    private readonly TimeSpan timeout;

    public CompanionStatusClient(HttpClient client, TimeSpan? timeout = null)
    {
        this.client = client ?? throw new ArgumentNullException(nameof(client));
        this.timeout = timeout ?? TimeSpan.FromSeconds(10);
        if (this.timeout <= TimeSpan.Zero) throw new ArgumentOutOfRangeException(nameof(timeout));
    }

    public async Task<CompanionDeviceStatus> GetStatusAsync(
        string host,
        int port,
        string pairingSecret,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(host)) throw new ArgumentException("Host is required.", nameof(host));
        if (port is < 10240 or > 65535) throw new ArgumentOutOfRangeException(nameof(port));

        var requestPath = "/v1/status";
        var timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        var uri = CreateEndpointUri(host, port, requestPath);
        using var request = new HttpRequestMessage(HttpMethod.Get, uri);
        request.Headers.TryAddWithoutValidation(
            CompanionProtocol.AuthorizationHeader,
            CompanionProtocol.CreateAuthorization(pairingSecret, "GET", requestPath, timestamp));

        using var timeoutSource = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        timeoutSource.CancelAfter(timeout);
        using var response = await client.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, timeoutSource.Token)
            .ConfigureAwait(false);
        if (response.StatusCode == HttpStatusCode.Unauthorized)
            throw new UnauthorizedAccessException("GameBox rejected the pairing secret.");
        response.EnsureSuccessStatusCode();

        await using var stream = await response.Content.ReadAsStreamAsync(timeoutSource.Token).ConfigureAwait(false);
        var bytes = await ReadBoundedAsync(stream, timeoutSource.Token).ConfigureAwait(false);
        try
        {
            using var document = JsonDocument.Parse(bytes);
            var root = document.RootElement;
            var version = root.GetProperty("protocolVersion").GetInt32();
            var deviceName = root.GetProperty("deviceName").GetString();
            var status = root.GetProperty("status").GetString();
            if (version != CompanionProtocol.Version)
                throw new InvalidDataException($"Unsupported GameBox companion protocol v{version}.");
            if (string.IsNullOrWhiteSpace(deviceName) || string.IsNullOrWhiteSpace(status))
                throw new InvalidDataException("GameBox returned an incomplete device status.");
            return new CompanionDeviceStatus(version, deviceName, status);
        }
        catch (JsonException exception)
        {
            throw new InvalidDataException("GameBox returned invalid device status JSON.", exception);
        }
    }

    private static Uri CreateEndpointUri(string host, int port, string path)
    {
        var normalized = host.Trim().Trim('[', ']');
        if (normalized.Contains('/') || normalized.Contains('\\') || normalized.Contains('?') || normalized.Contains('#'))
            throw new ArgumentException("Host must not include a scheme, path, or query.", nameof(host));
        return new UriBuilder(Uri.UriSchemeHttp, normalized, port, path).Uri;
    }

    private static async Task<byte[]> ReadBoundedAsync(Stream stream, CancellationToken cancellationToken)
    {
        using var output = new MemoryStream();
        var buffer = new byte[4096];
        while (true)
        {
            var read = await stream.ReadAsync(buffer, cancellationToken).ConfigureAwait(false);
            if (read == 0) return output.ToArray();
            if (output.Length + read > MaxResponseBytes)
                throw new InvalidDataException("GameBox status response exceeds 32 KiB.");
            await output.WriteAsync(buffer.AsMemory(0, read), cancellationToken).ConfigureAwait(false);
        }
    }
}
