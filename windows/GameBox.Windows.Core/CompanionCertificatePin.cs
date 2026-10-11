using System.Net.Http;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;

namespace GameBox.Windows.Core;

public static class CompanionCertificatePin
{
    public static string Normalize(string fingerprint)
    {
        var normalized = (fingerprint ?? "").Replace(":", "", StringComparison.Ordinal)
            .Replace(" ", "", StringComparison.Ordinal).Trim().ToUpperInvariant();
        if (normalized.Length != 64 || normalized.Any(static c => !char.IsAsciiHexDigit(c)))
            throw new ArgumentException("Enter the 64-digit SHA-256 certificate fingerprint shown in GameBox Settings.", nameof(fingerprint));
        return normalized;
    }

    public static bool Matches(string fingerprint, X509Certificate2? certificate)
    {
        if (certificate is null) return false;
        var expected = Convert.FromHexString(Normalize(fingerprint));
        var actual = certificate.GetCertHash(HashAlgorithmName.SHA256);
        return CryptographicOperations.FixedTimeEquals(expected, actual);
    }

    public static HttpClient CreatePinnedClient(string fingerprint)
    {
        var normalized = Normalize(fingerprint);
        var handler = new HttpClientHandler
        {
            ServerCertificateCustomValidationCallback = (_, certificate, _, _) =>
                certificate is not null && Matches(normalized, certificate),
        };
        return new HttpClient(handler, disposeHandler: true);
    }
}
