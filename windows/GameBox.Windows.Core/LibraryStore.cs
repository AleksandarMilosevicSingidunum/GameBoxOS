using System.Text.Json;

namespace GameBox.Windows.Core;

public sealed class LibraryStore
{
    private static readonly JsonSerializerOptions JsonOptions = new() {
        WriteIndented = true,
        PropertyNameCaseInsensitive = true
    };

    public LibraryStore(string path)
    {
        Path = System.IO.Path.GetFullPath(path);
    }

    public string Path { get; }

    public async Task<IReadOnlyList<GameEntry>> LoadAsync(CancellationToken cancellationToken = default)
    {
        if (!File.Exists(Path)) return Array.Empty<GameEntry>();
        await using var stream = File.OpenRead(Path);
        var entries = await JsonSerializer.DeserializeAsync<List<GameEntry>>(
            stream, JsonOptions, cancellationToken) ?? new();
        return GameLibrary.Normalize(entries);
    }

    public async Task ExportAsync(string destinationPath, CancellationToken cancellationToken = default)
    {
        var destination = new LibraryStore(destinationPath);
        await destination.SaveAsync(await LoadAsync(cancellationToken), cancellationToken);
    }

    public async Task<IReadOnlyList<GameEntry>> ImportAsync(string sourcePath, CancellationToken cancellationToken = default)
    {
        var source = new LibraryStore(sourcePath);
        if (!File.Exists(source.Path)) throw new FileNotFoundException("Library backup was not found.", source.Path);
        var entries = await source.LoadAsync(cancellationToken);
        await SaveAsync(entries, cancellationToken);
        return entries;
    }

    public async Task SaveAsync(
        IEnumerable<GameEntry> entries,
        CancellationToken cancellationToken = default)
    {
        var normalized = GameLibrary.Normalize(entries);
        var directory = System.IO.Path.GetDirectoryName(Path)
            ?? throw new InvalidOperationException("Library path requires a directory.");
        Directory.CreateDirectory(directory);
        var temporary = Path + ".tmp";
        await using (var stream = new FileStream(
            temporary, FileMode.Create, FileAccess.Write, FileShare.None))
        {
            await JsonSerializer.SerializeAsync(stream, normalized, JsonOptions, cancellationToken);
            await stream.FlushAsync(cancellationToken);
        }
        File.Move(temporary, Path, true);
    }
}
