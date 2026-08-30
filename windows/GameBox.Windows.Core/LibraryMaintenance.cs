namespace GameBox.Windows.Core;

public sealed record MissingEntryCleanupPlan(
    IReadOnlyList<GameEntry> RetainedEntries,
    IReadOnlyList<GameEntry> RemovedEntries)
{
    public int RemovedCount => RemovedEntries.Count;
}

public static class LibraryMaintenance
{
    public static MissingEntryCleanupPlan PlanMissingEntryCleanup(IEnumerable<GameEntry> entries)
    {
        var normalized = GameLibrary.Normalize(entries);
        var retained = new List<GameEntry>();
        var removed = new List<GameEntry>();
        foreach (var entry in normalized)
        {
            if (GameLibrary.IsLaunchTargetAvailable(entry)) retained.Add(entry);
            else removed.Add(entry);
        }
        return new MissingEntryCleanupPlan(retained, removed);
    }
}
