namespace GameBox.Windows.Core;

public enum LibrarySort
{
    FavoritesThenTitle,
    RecentlyPlayed
}

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
        bool favoritesOnly = false,
        bool availableOnly = false,
        string? platform = null,
        LibrarySort sort = LibrarySort.FavoritesThenTitle)
    {
        var value = query?.Trim() ?? "";
        var filtered = Normalize(entries).Where(x =>
            (!favoritesOnly || x.Favorite) &&
            (!availableOnly || IsLaunchTargetAvailable(x)) &&
            (string.IsNullOrWhiteSpace(platform) || x.Platform.Equals(platform.Trim(), StringComparison.CurrentCultureIgnoreCase)) &&
            (value.Length == 0 ||
             x.Title.Contains(value, StringComparison.CurrentCultureIgnoreCase) ||
             x.Platform.Contains(value, StringComparison.CurrentCultureIgnoreCase)));
        return (sort == LibrarySort.RecentlyPlayed
                ? filtered.OrderByDescending(x => x.LastPlayedUtc.HasValue)
                    .ThenByDescending(x => x.LastPlayedUtc)
                    .ThenBy(x => x.Title, StringComparer.CurrentCultureIgnoreCase)
                : filtered)
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

    public static string GetLaunchDirectory(GameEntry entry)
    {
        ArgumentNullException.ThrowIfNull(entry);
        var target = ValidateExecutablePath(entry.ExecutablePath);
        return Path.GetDirectoryName(target) ?? throw new InvalidDataException("Launch target has no parent directory.");
    }

    public static bool ContainsLaunchTarget(IEnumerable<GameEntry> entries, string executablePath, string? exceptId = null)
    {
        ArgumentNullException.ThrowIfNull(entries);
        var target = ValidateExecutablePath(executablePath);
        return entries.Any(entry =>
            !entry.Id.Equals(exceptId, StringComparison.OrdinalIgnoreCase) &&
            ValidateExecutablePath(entry.ExecutablePath).Equals(target, StringComparison.OrdinalIgnoreCase));
    }

    public static GameEntry Relocate(GameEntry entry, string executablePath)
    {
        ArgumentNullException.ThrowIfNull(entry);
        return NormalizeEntry(entry with { ExecutablePath = ValidateExecutablePath(executablePath) });
    }

    public static GameEntry UpdateLaunchMetadata(GameEntry entry, string title, string platform, string? arguments)
    {
        ArgumentNullException.ThrowIfNull(entry);
        return NormalizeEntry(entry with {
            Title = title,
            Platform = platform,
            Arguments = arguments ?? ""
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
