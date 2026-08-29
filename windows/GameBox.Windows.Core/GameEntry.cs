namespace GameBox.Windows.Core;

public sealed record GameEntry
{
    public required string Id { get; init; }
    public required string Title { get; init; }
    public string Platform { get; init; } = "Windows";
    public required string ExecutablePath { get; init; }
    public string Arguments { get; init; } = "";
    public bool Favorite { get; init; }
    public DateTimeOffset? LastPlayedUtc { get; init; }
}
