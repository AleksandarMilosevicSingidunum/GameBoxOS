using System.Net;
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
    Require(GameLibrary.Filter(ordered, "", platform: "PC").Single().Id == second.Id, "Platform filtering failed.");
    Require(GameLibrary.Filter(ordered, "", platform: " pc ").Single().Id == second.Id, "Platform filtering must ignore case and surrounding whitespace.");
    var olderPlayed = first with { LastPlayedUtc = DateTimeOffset.UtcNow.AddHours(-2) };
    var newerPlayed = second with { LastPlayedUtc = DateTimeOffset.UtcNow.AddMinutes(-5) };
    Require(GameLibrary.Filter(new[] { olderPlayed, newerPlayed }, "", platform: null, sort: LibrarySort.RecentlyPlayed)[0].Id == second.Id, "Recent sorting must place the newest launch first.");
    Require(GameLibrary.Filter(new[] { first, newerPlayed }, "", sort: LibrarySort.RecentlyPlayed)[0].Id == second.Id, "Played entries must sort ahead of never-played entries.");
    Require(GameLibrary.IsLaunchTargetAvailable(first), "Existing launch targets must be available.");
    Require(GameLibrary.GetLaunchDirectory(first) == Path.GetFullPath(root), "Launch directory resolution failed.");
    Require(GameLibrary.ContainsLaunchTarget(ordered, firstPath), "Existing launch targets must be detected.");
    Require(!GameLibrary.ContainsLaunchTarget(ordered, firstPath, first.Id), "The excluded entry must not conflict with itself.");
    File.Delete(secondPath);
    Require(!GameLibrary.IsLaunchTargetAvailable(second), "Missing launch targets must be unavailable.");
    Require(GameLibrary.Filter(ordered, "", availableOnly: true).Single().Id == first.Id, "Available-only filtering must hide missing targets.");
    var relocated = GameLibrary.Relocate(first, secondPath);
    Require(relocated.ExecutablePath == Path.GetFullPath(secondPath), "Relocation must update the launch target.");
    Require(relocated.Id == first.Id && relocated.Title == first.Title && relocated.Favorite == first.Favorite, "Relocation must preserve entry metadata.");

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

    var syncExisting = first with { Id = "sync-game", Favorite = true };
    using var syncClient = new HttpClient(new StubHttpMessageHandler(_ => new HttpResponseMessage(HttpStatusCode.OK) {
        RequestMessage = new HttpRequestMessage(HttpMethod.Get, "https://catalog.example/games"),
        Content = new StringContent("{\"games\":[{\"id\":\"sync-game\",\"title\":\"Updated Alpha\",\"platform\":\"DOS\"},{\"id\":\"remote-only\",\"title\":\"Ignored\",\"platform\":\"PC\"}]}")
    }));
    var synced = await new WindowsCatalogSyncClient(syncClient).EnrichExistingAsync(
        new[] { syncExisting }, "https://catalog.example/games");
    Require(synced.UpdatedCount == 1 && synced.IgnoredRemoteCount == 1, "Catalog sync counts failed.");
    Require(synced.Entries[0].Title == "Updated Alpha" && synced.Entries[0].ExecutablePath == syncExisting.ExecutablePath && synced.Entries[0].Favorite, "Catalog sync must preserve local launch state.");

    var redirectRejected = false;
    using var redirectClient = new HttpClient(new StubHttpMessageHandler(_ => new HttpResponseMessage(HttpStatusCode.OK) {
        RequestMessage = new HttpRequestMessage(HttpMethod.Get, "https://other.example/games"),
        Content = new StringContent("{\"games\":[]}")
    }));
    try { await new WindowsCatalogSyncClient(redirectClient).EnrichExistingAsync(new[] { syncExisting }, "https://catalog.example/games"); }
    catch (InvalidDataException) { redirectRejected = true; }
    Require(redirectRejected, "Catalog redirects must be rejected.");

    var unsafeEndpointRejected = false;
    try { await new WindowsCatalogSyncClient(new HttpClient()).EnrichExistingAsync(new[] { syncExisting }, "http://catalog.example/games"); }
    catch (ArgumentException) { unsafeEndpointRejected = true; }
    Require(unsafeEndpointRejected, "Insecure catalog endpoints must be rejected.");

    var oversizedRejected = false;
    using var oversizedClient = new HttpClient(new StubHttpMessageHandler(_ => new HttpResponseMessage(HttpStatusCode.OK) {
        RequestMessage = new HttpRequestMessage(HttpMethod.Get, "https://catalog.example/games"),
        Content = new StringContent(new string('x', 1_048_577))
    }));
    try { await new WindowsCatalogSyncClient(oversizedClient).EnrichExistingAsync(new[] { syncExisting }, "https://catalog.example/games"); }
    catch (InvalidDataException) { oversizedRejected = true; }
    Require(oversizedRejected, "Catalog responses above the limit must be rejected before parsing.");

    var invalidTimeoutRejected = false;
    try { _ = new WindowsCatalogSyncClient(new HttpClient(), TimeSpan.Zero); }
    catch (ArgumentOutOfRangeException) { invalidTimeoutRejected = true; }
    Require(invalidTimeoutRejected, "Non-positive catalog timeouts must be rejected.");

    var cancellationObserved = false;
    using var blockingClient = new HttpClient(new BlockingHandler());
    try { await new WindowsCatalogSyncClient(blockingClient, TimeSpan.FromMilliseconds(10)).EnrichExistingAsync(new[] { syncExisting }, "https://catalog.example/games"); }
    catch (OperationCanceledException) { cancellationObserved = true; }
    Require(cancellationObserved, "Catalog synchronization timeout must cancel a blocked request.");

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


sealed class BlockingHandler : HttpMessageHandler
{
    protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        await Task.Delay(Timeout.InfiniteTimeSpan, cancellationToken);
        throw new InvalidOperationException("unreachable");
    }
}
