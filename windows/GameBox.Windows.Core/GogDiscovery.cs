using System.Text.Json;

namespace GameBox.Windows.Core;

public sealed record GogDiscoveryResult(
    IReadOnlyList<GameEntry> Entries,
    int ManifestCount,
    int DuplicateCount,
    int InvalidManifestCount);

public static class GogDiscovery
{
    public const long MaxManifestBytes = 1024 * 1024;

    public static GogDiscoveryResult Discover(
        IEnumerable<string> searchRoots,
        IEnumerable<GameEntry> existingEntries,
        int maxManifests = 2000)
    {
        ArgumentNullException.ThrowIfNull(searchRoots);
        ArgumentNullException.ThrowIfNull(existingEntries);
        if (maxManifests is < 1 or > 5000)
            throw new ArgumentOutOfRangeException(nameof(maxManifests));

        var knownTargets = new HashSet<string>(
            existingEntries.Select(x => GameLibrary.ValidateExecutablePath(x.ExecutablePath)),
            StringComparer.OrdinalIgnoreCase);
        var entries = new List<GameEntry>();
        var manifests = 0;
        var duplicates = 0;
        var invalid = 0;

        foreach (var rawRoot in searchRoots.Where(x => !string.IsNullOrWhiteSpace(x)))
        {
            string root;
            try { root = Path.GetFullPath(rawRoot); }
            catch (Exception error) when (error is ArgumentException or NotSupportedException or PathTooLongException) { continue; }
            if (!Directory.Exists(root)) continue;

            IEnumerable<string> paths;
            try { paths = Directory.EnumerateFiles(root, "goggame-*.info", SearchOption.AllDirectories); }
            catch (Exception error) when (error is IOException or UnauthorizedAccessException) { continue; }

            using var enumerator = paths.GetEnumerator();
            while (manifests < maxManifests)
            {
                string manifestPath;
                try
                {
                    if (!enumerator.MoveNext()) break;
                    manifestPath = enumerator.Current;
                }
                catch (Exception error) when (error is IOException or UnauthorizedAccessException) { break; }

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
                entries.Add(GameLibrary.Create(title, executablePath) with { Platform = "GOG" });
            }
            if (manifests >= maxManifests) break;
        }

        return new GogDiscoveryResult(entries, manifests, duplicates, invalid);
    }

    private static bool TryReadManifest(string manifestPath, out string title, out string executablePath)
    {
        title = "";
        executablePath = "";
        try
        {
            var info = new FileInfo(manifestPath);
            if (!info.Exists || info.Length is <= 0 or > MaxManifestBytes) return false;

            using var stream = new FileStream(manifestPath, FileMode.Open, FileAccess.Read, FileShare.Read, 16 * 1024, FileOptions.SequentialScan);
            using var document = JsonDocument.Parse(stream, new JsonDocumentOptions {
                AllowTrailingCommas = false,
                CommentHandling = JsonCommentHandling.Disallow,
                MaxDepth = 32
            });
            var root = document.RootElement;
            if (root.ValueKind != JsonValueKind.Object ||
                !TryString(root, "name", out title) ||
                !root.TryGetProperty("playTasks", out var tasks) ||
                tasks.ValueKind != JsonValueKind.Array)
                return false;

            title = title.Trim();
            if (title.Length is <= 0 or > 200) return false;
            var installRoot = Path.GetDirectoryName(Path.GetFullPath(manifestPath));
            if (string.IsNullOrWhiteSpace(installRoot)) return false;

            foreach (var task in tasks.EnumerateArray())
            {
                if (task.ValueKind != JsonValueKind.Object ||
                    !TryString(task, "path", out var relativePath) ||
                    Path.IsPathRooted(relativePath))
                    continue;
                if (task.TryGetProperty("isPrimary", out var primary) &&
                    primary.ValueKind == JsonValueKind.False)
                    continue;

                var candidate = Path.GetFullPath(Path.Combine(
                    installRoot,
                    relativePath.Replace(Path.AltDirectorySeparatorChar, Path.DirectorySeparatorChar)));
                var relative = Path.GetRelativePath(installRoot, candidate);
                if (Path.IsPathRooted(relative) ||
                    relative.Equals("..", StringComparison.Ordinal) ||
                    relative.StartsWith(".." + Path.DirectorySeparatorChar, StringComparison.Ordinal))
                    continue;

                var validated = GameLibrary.ValidateExecutablePath(candidate);
                if (!File.Exists(validated)) continue;
                executablePath = validated;
                return true;
            }
            return false;
        }
        catch (Exception error) when (
            error is IOException or UnauthorizedAccessException or JsonException or
            ArgumentException or NotSupportedException or PathTooLongException or
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
