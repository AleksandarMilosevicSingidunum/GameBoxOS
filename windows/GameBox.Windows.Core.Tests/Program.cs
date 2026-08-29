using GameBox.Windows.Core;

var root = Path.Combine(Path.GetTempPath(), "gamebox-windows-tests-" + Guid.NewGuid().ToString("N"));
Directory.CreateDirectory(root);
try
{
    var firstPath = Path.Combine(root, "Alpha.exe");
    var secondPath = Path.Combine(root, "Beta.lnk");
    await File.WriteAllTextAsync(firstPath, "");
    await File.WriteAllTextAsync(secondPath, "");

    var first = GameLibrary.Create(" Alpha ", firstPath) with { Favorite = true };
    var second = GameLibrary.Create("Beta", secondPath) with { Platform = "PC" };

    var ordered = GameLibrary.Normalize(new[] { second, first });
    Require(ordered[0].Id == first.Id, "Favorites must sort first.");
    Require(GameLibrary.Filter(ordered, "pc").Single().Id == second.Id, "Search must match platform.");
    Require(GameLibrary.Filter(ordered, "", favoritesOnly: true).Single().Id == first.Id, "Favorite filtering failed.");

    var store = new LibraryStore(Path.Combine(root, "data", "library.json"));
    await store.SaveAsync(ordered);
    var loaded = await store.LoadAsync();
    Require(loaded.Count == 2 && loaded.Any(x => x.Title == "Alpha"), "Atomic JSON round trip failed.");

    var duplicateRejected = false;
    try { GameLibrary.Normalize(new[] { first, first }); }
    catch (InvalidDataException) { duplicateRejected = true; }
    Require(duplicateRejected, "Duplicate IDs must be rejected.");

    var unsafeRejected = false;
    try { GameLibrary.Create("Unsafe", Path.Combine(root, "notes.txt")); }
    catch (InvalidDataException) { unsafeRejected = true; }
    Require(unsafeRejected, "Unsupported launch targets must be rejected.");

    Console.WriteLine("GameBox Windows core tests passed.");
}
finally
{
    Directory.Delete(root, recursive: true);
}
static void Require(bool condition, string message)
{
    if (!condition) throw new InvalidOperationException(message);
}
