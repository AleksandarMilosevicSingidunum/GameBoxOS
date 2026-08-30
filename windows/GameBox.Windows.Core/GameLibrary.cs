namespace GameBox.Windows.Core;

public static class GameLibrary
{
    private static readonly HashSet<string> LaunchableExtensions =
        new(StringComparer.OrdinalIgnoreCase) { ".exe", ".lnk", ".bat", ".cmd" };

    public static IReadOnlyList<GameEntry> Normalize(IEnumerable<GameEntry> entries)
    {
        ArgumentNullException.ThrowIfNull(entries);
        var normalized = entries.Select(NormalizeEntry).ToList();
        var duplicate = normalized.GroupBy(x => x.Id, StringComparer.OrdinalIgnoreCase)
            .FirstOrDefault(x => x.Count() > 1);
        if (duplicate is not null)
            throw new InvalidDataException("Duplicate game ID: " + duplicate.Key);
        return normalized.OrderByDescending(x => x.Favorite)
            .ThenBy(x => x.Title, StringComparer.CurrentCultureIgnoreCase)
            .ToList();
    }

    public static IReadOnlyList<GameEntry> Filter(
        IEnumerable<GameEntry> entries,
        string? query,
        bool favoritesOnly = false)
    {
        var value = query?.Trim() ?? "";
        return Normalize(entries).Where(x =>
            (!favoritesOnly || x.Favorite) &&
            (value.Length == 0 ||
             x.Title.Contains(value, StringComparison.CurrentCultureIgnoreCase) ||
             x.Platform.Contains(value, StringComparison.CurrentCultureIgnoreCase)))
            .ToList();
    }

    public static GameEntry Create(string title, string executablePath)
    {
        var fullPath = ValidateExecutablePath(executablePath);
        var cleanTitle = title.Trim();
        if (cleanTitle.Length == 0) cleanTitle = Path.GetFileNameWithoutExtension(fullPath);
        return NormalizeEntry(new GameEntry {
            Id = Guid.NewGuid().ToString("N"),
            Title = cleanTitle,
            ExecutablePath = fullPath
        });
    }

    public static bool IsLaunchTargetAvailable(GameEntry entry)
    {
        ArgumentNullException.ThrowIfNull(entry);
        return File.Exists(ValidateExecutablePath(entry.ExecutablePath));
    }

    public static string ValidateExecutablePath(string executablePath)
    {
        if (string.IsNullOrWhiteSpace(executablePath))
            throw new ArgumentException("Executable path is required.", nameof(executablePath));
        var fullPath = Path.GetFullPath(executablePath.Trim());
        if (!LaunchableExtensions.Contains(Path.GetExtension(fullPath)))
            throw new InvalidDataException("Choose an EXE, shortcut, BAT, or CMD file.");
        return fullPath;
    }

    private static GameEntry NormalizeEntry(GameEntry entry)
    {
        if (string.IsNullOrWhiteSpace(entry.Id))
            throw new InvalidDataException("Every game requires an ID.");
        if (string.IsNullOrWhiteSpace(entry.Title))
            throw new InvalidDataException("Every game requires a title.");
        return entry with {
            Id = entry.Id.Trim(),
            Title = entry.Title.Trim(),
            Platform = string.IsNullOrWhiteSpace(entry.Platform) ? "Windows" : entry.Platform.Trim(),
            ExecutablePath = ValidateExecutablePath(entry.ExecutablePath),
            Arguments = entry.Arguments.Trim()
        };
    }
}
