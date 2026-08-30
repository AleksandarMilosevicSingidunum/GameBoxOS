namespace GameBox.Windows.Core;

public sealed record ShortcutDiscoveryResult(
    IReadOnlyList<GameEntry> Entries,
    int ScannedCount,
    int DuplicateCount);

public static class ShortcutDiscovery
{
    public static ShortcutDiscoveryResult Discover(
        IEnumerable<string> roots,
        IEnumerable<GameEntry> existingEntries,
        int maxCandidates = 500)
    {
        ArgumentNullException.ThrowIfNull(roots);
        ArgumentNullException.ThrowIfNull(existingEntries);
        if (maxCandidates is < 1 or > 5000)
            throw new ArgumentOutOfRangeException(nameof(maxCandidates));

        var knownTargets = new HashSet<string>(
            existingEntries.Select(x => GameLibrary.ValidateExecutablePath(x.ExecutablePath)),
            StringComparer.OrdinalIgnoreCase);
        var discovered = new List<GameEntry>();
        var scanned = 0;
        var duplicates = 0;
        var options = new EnumerationOptions {
            RecurseSubdirectories = true,
            IgnoreInaccessible = true,
            ReturnSpecialDirectories = false,
            MaxRecursionDepth = 12
        };

        foreach (var rootValue in roots.Where(x => !string.IsNullOrWhiteSpace(x))
                     .Select(Path.GetFullPath).Distinct(StringComparer.OrdinalIgnoreCase))
        {
            if (!Directory.Exists(rootValue)) continue;
            foreach (var path in Directory.EnumerateFiles(rootValue, "*.lnk", options))
            {
                if (scanned >= maxCandidates)
                    return new ShortcutDiscoveryResult(discovered, scanned, duplicates);
                scanned++;
                var fullPath = GameLibrary.ValidateExecutablePath(path);
                if (!knownTargets.Add(fullPath))
                {
                    duplicates++;
                    continue;
                }
                discovered.Add(GameLibrary.Create(Path.GetFileNameWithoutExtension(fullPath), fullPath) with {
                    Platform = "Windows shortcut"
                });
            }
        }
        return new ShortcutDiscoveryResult(discovered, scanned, duplicates);
    }
}
