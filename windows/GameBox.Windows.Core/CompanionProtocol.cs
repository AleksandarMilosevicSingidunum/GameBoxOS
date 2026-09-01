using System.Security.Cryptography;
using System.Text;

namespace GameBox.Windows.Core;

/// <summary>
/// Versioned, authenticated request primitives shared by the Windows companion's
/// future device client. Pairing secrets are never serialized with the device profile.
/// </summary>
public static class CompanionProtocol
{
    public const int Version = 1;
    public const string AuthorizationHeader = "X-GameBox-Authorization";

    public static string CreatePairingSecret()
        => Convert.ToHexString(RandomNumberGenerator.GetBytes(32)).ToLowerInvariant();

    public static string CreateAuthorization(string pairingSecret, string method, string requestPath, long unixTimeSeconds)
    {
        if (!IsValidSecret(pairingSecret)) throw new ArgumentException("Pairing secret is invalid.", nameof(pairingSecret));
        if (string.IsNullOrWhiteSpace(method)) throw new ArgumentException("Method is required.", nameof(method));
        if (!requestPath.StartsWith('/', StringComparison.Ordinal) || requestPath.Contains("..", StringComparison.Ordinal))
            throw new ArgumentException("Request path must be absolute and traversal-free.", nameof(requestPath));
        if (unixTimeSeconds <= 0) throw new ArgumentOutOfRangeException(nameof(unixTimeSeconds));

        var payload = $"v{Version}\n{method.Trim().ToUpperInvariant()}\n{requestPath}\n{unixTimeSeconds}";
        var key = Convert.FromHexString(pairingSecret);
        var hash = HMACSHA256.HashData(key, Encoding.UTF8.GetBytes(payload));
        return $"v{Version}:{unixTimeSeconds}:{Convert.ToHexString(hash).ToLowerInvariant()}";
    }

    public static bool VerifyAuthorization(
        string pairingSecret,
        string method,
        string requestPath,
        string? authorization,
        long nowUnixTimeSeconds,
        long allowedSkewSeconds = 120)
    {
        if (!IsValidSecret(pairingSecret) || string.IsNullOrWhiteSpace(authorization) || allowedSkewSeconds < 0) return false;
        var parts = authorization.Split(':');
        if (parts.Length != 3 || parts[0] != $"v{Version}" || !long.TryParse(parts[1], out var timestamp)) return false;
        if (Math.Abs(nowUnixTimeSeconds - timestamp) > allowedSkewSeconds) return false;
        try
        {
            var expected = CreateAuthorization(pairingSecret, method, requestPath, timestamp);
            var expectedBytes = Encoding.UTF8.GetBytes(expected);
            var actualBytes = Encoding.UTF8.GetBytes(authorization);
            return expectedBytes.Length == actualBytes.Length && CryptographicOperations.FixedTimeEquals(expectedBytes, actualBytes);
        }
        catch (ArgumentException) { return false; }
    }

    private static bool IsValidSecret(string value) =>
        value.Length == 64 && value.All(static c => char.IsAsciiHexDigit(c));
}

