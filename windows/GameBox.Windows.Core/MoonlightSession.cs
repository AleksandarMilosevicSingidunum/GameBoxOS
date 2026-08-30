using System.Net;
using System.Text;

namespace GameBox.Windows.Core;

public static class MoonlightSession
{
    public static GameEntry Create(
        string moonlightExecutable,
        string host,
        string applicationName)
    {
        var executable = GameLibrary.ValidateExecutablePath(moonlightExecutable);
        if (!Path.GetExtension(executable).Equals(".exe", StringComparison.OrdinalIgnoreCase))
            throw new InvalidDataException("Moonlight must be an EXE launch target.");

        var normalizedHost = NormalizeHost(host);
        var normalizedApplication = NormalizeApplicationName(applicationName);
        return GameLibrary.Create(
            normalizedApplication + " on " + normalizedHost,
            executable) with {
                Platform = "Moonlight",
                Arguments = "stream " + Quote(normalizedHost) + " " + Quote(normalizedApplication)
            };
    }

    public static string NormalizeHost(string host)
    {
        var value = host?.Trim() ?? "";
        if (value.Length is <= 0 or > 253 ||
            value.Any(char.IsControl) ||
            value.Contains('/') ||
            value.Contains('\\') ||
            value.Contains('@') ||
            value.Contains('?') ||
            value.Contains('#'))
            throw new ArgumentException("Enter a DNS name or IP address without a URL, path, or credentials.", nameof(host));

        if (IPAddress.TryParse(value.Trim('[', ']'), out var address))
            return address.ToString();

        if (Uri.CheckHostName(value) != UriHostNameType.Dns)
            throw new ArgumentException("Enter a valid DNS name or IP address.", nameof(host));
        return value.ToLowerInvariant();
    }

    public static string NormalizeApplicationName(string applicationName)
    {
        var value = applicationName?.Trim() ?? "";
        if (value.Length is <= 0 or > 200 || value.Any(char.IsControl))
            throw new ArgumentException("Streaming application name must contain 1 to 200 printable characters.", nameof(applicationName));
        return value;
    }

    private static string Quote(string value)
    {
        var builder = new StringBuilder(value.Length + 2);
        builder.Append('"');
        var slashes = 0;
        foreach (var character in value)
        {
            if (character == '\\')
            {
                slashes++;
                continue;
            }
            if (character == '"')
            {
                builder.Append('\\', checked(slashes * 2 + 1));
                builder.Append('"');
                slashes = 0;
                continue;
            }
            builder.Append('\\', slashes);
            builder.Append(character);
            slashes = 0;
        }
        builder.Append('\\', checked(slashes * 2));
        builder.Append('"');
        return builder.ToString();
    }
}
