using System.Text.Json;

namespace GameBox.Windows.Core;

public sealed record EpicDiscoveryResult(
    IReadOnlyList<GameEntry> Entries,
    int ManifestCount,
    int DuplicateCount,
    int InvalidManifestCount);

public static class EpicDiscovery
{
    public const long MaxManifestBytes = 1024 * 1024;

    public static EpicDiscoveryResult Discover(
        string manifestRoot,
        IEnumerable<GameEntry> existingEntries,
        int maxManifests = 2000)
    {
        if (string.IsNullOrWhiteSpace(manifestRoot))
            throw new ArgumentException("Epic manifest root is required.", nameof(manifestRoot));
        ArgumentNullException.ThrowIfNull(existingEntries);
        if (maxManifests is < 1 or > 5000)
            throw new ArgumentOutOfRangeException(nameof(maxManifests));

        var root = Path.GetFullPath(manifestRoot);
        if (!Directory.Exists(root))
            return new EpicDiscoveryResult(Array.Empty<GameEntry>(), 0, 0, 0);

        var knownTargets = new HashSet<string>(
            existingEntries.Select(x => GameLibrary.ValidateExecutablePath(x.ExecutablePath)),
            StringComparer.OrdinalIgnoreCase);
        var entries = new List<GameEntry>();
        var manifests = 0;
        var duplicates = 0;
        var invalid = 0;

        foreach (var manifestPath in Directory.EnumerateFiles(
                     root,
                     "*.item",
                     SearchOption.TopDirectoryOnly))
        {
            if (manifests >= maxManifests)
                return new EpicDiscoveryResult(entries, manifests, duplicates, invalid);
            manifests++;

            if (!TryReadManifest(manifestPath, out var title, out var executablePath))
            {
                invalid++;
                continue;
            }

            if (!knownTargets.Add(executablePath))
            {
                duplicates++;
                continue;
            }

            entries.Add(GameLibrary.Create(title, executablePath) with {
                Platform = "Epic Games"
            });
        }

        return new EpicDiscoveryResult(entries, manifests, duplicates, invalid);
    }

    private static bool TryReadManifest(
        string manifestPath,
        out string title,
        out string executablePath)
    {
        title = "";
        executablePath = "";
        try
        {
            var info = new FileInfo(manifestPath);
            if (!info.Exists || info.Length is <= 0 or > MaxManifestBytes)
                return false;

            using var stream = new FileStream(
                manifestPath,
                FileMode.Open,
                FileAccess.Read,
                FileShare.Read,
                bufferSize: 16 * 1024,
                FileOptions.SequentialScan);
            using var document = JsonDocument.Parse(stream, new JsonDocumentOptions {
                AllowTrailingCommas = false,
                CommentHandling = JsonCommentHandling.Disallow,
                MaxDepth = 32
            });
            var root = document.RootElement;
            if (root.ValueKind != JsonValueKind.Object ||
                !TryString(root, "DisplayName", out title) ||
                !TryString(root, "InstallLocation", out var installValue) ||
                !TryString(root, "LaunchExecutable", out var launchValue))
                return false;

            title = title.Trim();
            if (title.Length is <= 0 or > 200 ||
                string.IsNullOrWhiteSpace(installValue) ||
                string.IsNullOrWhiteSpace(launchValue) ||
                Path.IsPathRooted(launchValue))
                return false;

            var installRoot = Path.GetFullPath(installValue.Trim());
            var relativeLaunch = launchValue.Trim()
                .Replace(Path.AltDirectorySeparatorChar, Path.DirectorySeparatorChar);
            var candidate = Path.GetFullPath(Path.Combine(installRoot, relativeLaunch));
            var relative = Path.GetRelativePath(installRoot, candidate);
            if (Path.IsPathRooted(relative) ||
                relative.Equals("..", StringComparison.Ordinal) ||
                relative.StartsWith(".." + Path.DirectorySeparatorChar, StringComparison.Ordinal))
                return false;

            executablePath = GameLibrary.ValidateExecutablePath(candidate);
            return File.Exists(executablePath);
        }
        catch (Exception error) when (
            error is IOException or
            UnauthorizedAccessException or
            JsonException or
            ArgumentException or
            NotSupportedException or
            PathTooLongException or
            InvalidDataException)
        {
            return false;
        }
    }

    private static bool TryString(JsonElement root, string name, out string value)
    {
        value = "";
        return root.TryGetProperty(name, out var element) &&
               element.ValueKind == JsonValueKind.String &&
               (value = element.GetString() ?? "").Length > 0;
    }
}
