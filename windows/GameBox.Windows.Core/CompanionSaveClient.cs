using System.Net;
using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Text.Json;

namespace GameBox.Windows.Core;

public sealed record CompanionSaveDownload(
    string GameId,
    long UpdatedAtMillis,
    string Sha256,
    byte[] Bytes);

public sealed record CompanionSaveUploadResult(
    string GameId,
    long UpdatedAtMillis,
    string Sha256,
    bool ConflictPreserved);

public sealed class CompanionSaveClient
{
    public const int MaxSaveBytes = 16 * 1024 * 1024;
    private const int MaxEnvelopeBytes = 24 * 1024 * 1024;
    private readonly HttpClient client;
    private readonly TimeSpan timeout;

    public CompanionSaveClient(HttpClient client, TimeSpan? timeout = null)
    {
        this.client = client ?? throw new ArgumentNullException(nameof(client));
        this.timeout = timeout ?? TimeSpan.FromSeconds(60);
        if (this.timeout <= TimeSpan.Zero) throw new ArgumentOutOfRangeException(nameof(timeout));
    }

    public async Task<CompanionSaveDownload> DownloadAsync(
        string host,
        int port,
        string pairingSecret,
        string gameId,
        CancellationToken cancellationToken = default)
    {
        var path = SavePath(gameId);
        using var request = CreateRequest(HttpMethod.Get, host, port, pairingSecret, path);
        using var timeoutSource = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        timeoutSource.CancelAfter(timeout);
        using var response = await client.SendAsync(
            request,
            HttpCompletionOption.ResponseHeadersRead,
            timeoutSource.Token).ConfigureAwait(false);
        if (response.StatusCode == HttpStatusCode.NotFound)
            throw new FileNotFoundException("GameBox has no managed save for this game.");
        await EnsureSuccessAsync(response).ConfigureAwait(false);
        var envelope = await ReadBoundedAsync(response, timeoutSource.Token).ConfigureAwait(false);
        using var document = ParseEnvelope(envelope);
        var root = document.RootElement;
        RequireProtocol(root);
        var returnedGameId = root.GetProperty("gameId").GetString();
        var hash = root.GetProperty("sha256").GetString();
        var base64 = root.GetProperty("payloadBase64").GetString();
        if (returnedGameId != gameId || hash is null || hash.Length != 64 || base64 is null)
            throw new InvalidDataException("GameBox returned an invalid save envelope.");
        byte[] bytes;
        try { bytes = Convert.FromBase64String(base64); }
        catch (FormatException exception) { throw new InvalidDataException("GameBox returned invalid save bytes.", exception); }
        if (bytes.Length is <= 0 or > MaxSaveBytes || !Sha256(bytes).Equals(hash, StringComparison.OrdinalIgnoreCase))
            throw new InvalidDataException("GameBox save checksum verification failed.");
        return new CompanionSaveDownload(
            gameId,
            root.GetProperty("updatedAtMillis").GetInt64(),
            hash.ToLowerInvariant(),
            bytes);
    }

    public async Task<CompanionSaveUploadResult> UploadAsync(
        string host,
        int port,
        string pairingSecret,
        string gameId,
        byte[] bytes,
        CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(bytes);
        if (bytes.Length is <= 0 or > MaxSaveBytes)
            throw new InvalidDataException("Save must contain 1 byte to 16 MiB.");
        var path = SavePath(gameId);
        using var request = CreateRequest(
            HttpMethod.Put,
            host,
            port,
            pairingSecret,
            path,
            Sha256(bytes));
        request.Content = new ByteArrayContent(bytes);
        request.Content.Headers.ContentType = new MediaTypeHeaderValue("application/octet-stream");
        using var timeoutSource = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        timeoutSource.CancelAfter(timeout);
        using var response = await client.SendAsync(
            request,
            HttpCompletionOption.ResponseHeadersRead,
            timeoutSource.Token).ConfigureAwait(false);
        await EnsureSuccessAsync(response).ConfigureAwait(false);
        var envelope = await ReadBoundedAsync(response, timeoutSource.Token).ConfigureAwait(false);
        using var document = ParseEnvelope(envelope);
        var root = document.RootElement;
        RequireProtocol(root);
        var returnedGameId = root.GetProperty("gameId").GetString();
        var hash = root.GetProperty("sha256").GetString();
        if (returnedGameId != gameId || !Sha256(bytes).Equals(hash, StringComparison.OrdinalIgnoreCase))
            throw new InvalidDataException("GameBox did not confirm the uploaded save checksum.");
        return new CompanionSaveUploadResult(
            gameId,
            root.GetProperty("updatedAtMillis").GetInt64(),
            hash!.ToLowerInvariant(),
            root.GetProperty("conflictPreserved").GetBoolean());
    }

    private HttpRequestMessage CreateRequest(
        HttpMethod method,
        string host,
        int port,
        string pairingSecret,
        string path,
        string? bodySha256 = null)
    {
        if (string.IsNullOrWhiteSpace(host)) throw new ArgumentException("Host is required.", nameof(host));
        if (port is < 10240 or > 65535) throw new ArgumentOutOfRangeException(nameof(port));
        var normalized = host.Trim().Trim('[', ']');
        if (normalized.Contains('/') || normalized.Contains('\\') || normalized.Contains('?') || normalized.Contains('#'))
            throw new ArgumentException("Host must not include a scheme, path, or query.", nameof(host));
        var request = new HttpRequestMessage(method, new UriBuilder(Uri.UriSchemeHttp, normalized, port, path).Uri);
        request.Headers.TryAddWithoutValidation(
            CompanionProtocol.AuthorizationHeader,
            CompanionProtocol.CreateAuthorization(
                pairingSecret,
                method.Method,
                path,
                DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                bodySha256));
        return request;
    }

    private static async Task EnsureSuccessAsync(HttpResponseMessage response)
    {
        if (response.StatusCode == HttpStatusCode.Unauthorized)
            throw new UnauthorizedAccessException("GameBox rejected the pairing secret.");
        if (!response.IsSuccessStatusCode)
        {
            var status = response.StatusCode;
            throw new InvalidDataException("GameBox rejected the save transfer (" + (int)status + ").");
        }
        await Task.CompletedTask;
    }

    private static async Task<byte[]> ReadBoundedAsync(
        HttpResponseMessage response,
        CancellationToken cancellationToken)
    {
        var declared = response.Content.Headers.ContentLength;
        if (declared is < 0 or > MaxEnvelopeBytes)
            throw new InvalidDataException("GameBox save response exceeds the transfer limit.");
        await using var stream = await response.Content.ReadAsStreamAsync(cancellationToken).ConfigureAwait(false);
        using var output = new MemoryStream();
        var buffer = new byte[8192];
        while (true)
        {
            var read = await stream.ReadAsync(buffer, cancellationToken).ConfigureAwait(false);
            if (read == 0) return output.ToArray();
            if (output.Length + read > MaxEnvelopeBytes)
                throw new InvalidDataException("GameBox save response exceeds the transfer limit.");
            await output.WriteAsync(buffer.AsMemory(0, read), cancellationToken).ConfigureAwait(false);
        }
    }

    private static JsonDocument ParseEnvelope(byte[] bytes)
    {
        try { return JsonDocument.Parse(bytes); }
        catch (JsonException exception) { throw new InvalidDataException("GameBox returned invalid save JSON.", exception); }
    }

    private static void RequireProtocol(JsonElement root)
    {
        if (root.GetProperty("protocolVersion").GetInt32() != CompanionProtocol.Version)
            throw new InvalidDataException("GameBox returned an incompatible save protocol.");
    }

    private static string SavePath(string gameId)
    {
        if (string.IsNullOrWhiteSpace(gameId) || gameId.Length > 96 ||
            !char.IsLetterOrDigit(gameId[0]) ||
            gameId.Any(static c => !(char.IsLetterOrDigit(c) || c is '.' or '_' or '-')))
            throw new ArgumentException("Game ID is invalid.", nameof(gameId));
        return "/v1/saves/" + gameId;
    }

    private static string Sha256(byte[] bytes) =>
        Convert.ToHexString(SHA256.HashData(bytes)).ToLowerInvariant();
}
