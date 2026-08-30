using System.Text;
using System.Text.RegularExpressions;

namespace GameBox.Windows.Core;

public sealed record SteamDiscoveryResult(
    IReadOnlyList<GameEntry> Entries,
    int ManifestCount,
    int DuplicateCount,
    int InvalidManifestCount);

public static partial class SteamDiscovery
{
    public static SteamDiscoveryResult Discover(
        string steamRoot,
        string shortcutRoot,
        IEnumerable<GameEntry> existingEntries,
        int maxManifests = 2000)
    {
        if (string.IsNullOrWhiteSpace(steamRoot))
            throw new ArgumentException("Steam root is required.", nameof(steamRoot));
        if (string.IsNullOrWhiteSpace(shortcutRoot))
            throw new ArgumentException("Shortcut root is required.", nameof(shortcutRoot));
        ArgumentNullException.ThrowIfNull(existingEntries);
        if (maxManifests is < 1 or > 5000)
            throw new ArgumentOutOfRangeException(nameof(maxManifests));

        var root = Path.GetFullPath(steamRoot);
        var outputRoot = Path.GetFullPath(shortcutRoot);
        if (!Directory.Exists(root))
            return new SteamDiscoveryResult(Array.Empty<GameEntry>(), 0, 0, 0);

        var libraries = DiscoverLibraryRoots(root);
        var knownTargets = new HashSet<string>(
            existingEntries.Select(x => GameLibrary.ValidateExecutablePath(x.ExecutablePath)),
            StringComparer.OrdinalIgnoreCase);
        var entries = new List<GameEntry>();
        var manifests = 0;
        var duplicates = 0;
        var invalid = 0;

        foreach (var library in libraries)
        {
            var steamApps = Path.Combine(library, "steamapps");
            if (!Directory.Exists(steamApps)) continue;
            foreach (var manifestPath in Directory.EnumerateFiles(
                         steamApps,
                         "appmanifest_*.acf",
                         SearchOption.TopDirectoryOnly))
            {
                if (manifests >= maxManifests)
                    return new SteamDiscoveryResult(entries, manifests, duplicates, invalid);
                manifests++;

                if (!TryReadManifest(manifestPath, out var appId, out var title))
                {
                    invalid++;
                    continue;
                }

                var shortcutPath = Path.Combine(outputRoot, "steam-" + appId + ".url");
                var normalizedTarget = GameLibrary.ValidateExecutablePath(shortcutPath);
                if (!knownTargets.Add(normalizedTarget))
                {
                    duplicates++;
                    continue;
                }

                WriteSteamShortcut(shortcutPath, appId);
                entries.Add(GameLibrary.Create(title, shortcutPath) with {
                    Platform = "Steam"
                });
            }
        }

        return new SteamDiscoveryResult(entries, manifests, duplicates, invalid);
    }

    public static IReadOnlyList<string> DiscoverLibraryRoots(string steamRoot)
    {
        var root = Path.GetFullPath(steamRoot);
        var libraries = new HashSet<string>(StringComparer.OrdinalIgnoreCase) { root };
        var libraryFile = Path.Combine(root, "steamapps", "libraryfolders.vdf");
        if (!File.Exists(libraryFile)) return libraries.ToList();

        var payload = File.ReadAllText(libraryFile);
        foreach (Match match in LibraryPathRegex().Matches(payload))
        {
            var value = match.Groups["path"].Value
                .Replace(@"\\", @"\", StringComparison.Ordinal)
                .Trim();
            if (value.Length == 0) continue;
            try { libraries.Add(Path.GetFullPath(value)); }
            catch (Exception error) when (error is ArgumentException or NotSupportedException or PathTooLongException) { }
        }
        return libraries.ToList();
    }

    private static bool TryReadManifest(string path, out string appId, out string title)
    {
        appId = "";
        title = "";
        string payload;
        try { payload = File.ReadAllText(path); }
        catch (IOException) { return false; }
        catch (UnauthorizedAccessException) { return false; }

        var idMatch = AppIdRegex().Match(payload);
        var nameMatch = NameRegex().Match(payload);
        if (!idMatch.Success || !nameMatch.Success) return false;
        appId = idMatch.Groups["value"].Value;
        title = nameMatch.Groups["value"].Value
            .Replace("\\\"", "\"", StringComparison.Ordinal)
            .Replace(@"\\", @"\", StringComparison.Ordinal)
            .Trim();
        return appId.Length is > 0 and <= 20 &&
               appId.All(char.IsAsciiDigit) &&
               title.Length is > 0 and <= 200;
    }

    private static void WriteSteamShortcut(string path, string appId)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        var temp = path + "." + Guid.NewGuid().ToString("N") + ".tmp";
        File.WriteAllText(
            temp,
            "[InternetShortcut]" + Environment.NewLine +
            "URL=steam://rungameid/" + appId + Environment.NewLine,
            new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
        File.Move(temp, path, overwrite: true);
    }

    [GeneratedRegex("\\\"path\\\"\\s+\\\"(?<path>[^\\\"]+)\\\"", RegexOptions.IgnoreCase)]
    private static partial Regex LibraryPathRegex();

    [GeneratedRegex("\\\"appid\\\"\\s+\\\"(?<value>[0-9]+)\\\"", RegexOptions.IgnoreCase)]
    private static partial Regex AppIdRegex();

    [GeneratedRegex("\\\"name\\\"\\s+\\\"(?<value>(?:\\\\.|[^\\\"])*)\\\"", RegexOptions.IgnoreCase)]
    private static partial Regex NameRegex();
}
