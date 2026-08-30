namespace GameBox.Windows.Core;

public sealed record LibraryHealthSummary(
    int TotalCount,
    int AvailableCount,
    int MissingCount,
    int FavoriteCount,
    int PlatformCount)
{
    public static LibraryHealthSummary Create(IEnumerable<GameEntry> entries)
    {
        var normalized = GameLibrary.Normalize(entries);
        var available = normalized.Count(GameLibrary.IsLaunchTargetAvailable);
        return new LibraryHealthSummary(
            normalized.Count,
            available,
            normalized.Count - available,
            normalized.Count(x => x.Favorite),
            normalized.Select(x => x.Platform).Distinct(StringComparer.CurrentCultureIgnoreCase).Count());
    }
}
