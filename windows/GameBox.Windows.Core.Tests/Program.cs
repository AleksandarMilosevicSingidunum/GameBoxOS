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
    var shortcutRoot = Path.Combine(root, "shortcuts");
    Directory.CreateDirectory(Path.Combine(shortcutRoot, "nested"));
    var discoveredPath = Path.Combine(shortcutRoot, "nested", "Gamma.lnk");
    await File.WriteAllTextAsync(discoveredPath, "");
    var duplicatePath = Path.Combine(shortcutRoot, "Beta.lnk");
    File.Copy(secondPath, duplicatePath);
    var existingDuplicate = second with { ExecutablePath = duplicatePath };
    var discovery = ShortcutDiscovery.Discover(new[] { shortcutRoot, shortcutRoot }, new[] { existingDuplicate });
    Require(discovery.Entries.Count == 1 && discovery.Entries[0].Title == "Gamma", "Shortcut discovery must find nested shortcuts.");
    Require(discovery.ScannedCount == 2 && discovery.DuplicateCount == 1, "Shortcut discovery must report scanned and duplicate targets.");
    Require(discovery.Entries[0].Platform == "Windows shortcut", "Discovered shortcuts must use the shortcut platform.");
    var invalidDiscoveryLimitRejected = false;
    try { ShortcutDiscovery.Discover(new[] { shortcutRoot }, Array.Empty<GameEntry>(), 0); }
    catch (ArgumentOutOfRangeException) { invalidDiscoveryLimitRejected = true; }
    Require(invalidDiscoveryLimitRejected, "Invalid shortcut discovery limits must be rejected.");
    File.Delete(secondPath);
    Require(!GameLibrary.IsLaunchTargetAvailable(second), "Missing launch targets must be unavailable.");
    var health = LibraryHealthSummary.Create(ordered);
    Require(health.TotalCount == 2 && health.AvailableCount == 1 && health.MissingCount == 1, "Library health availability counts failed.");
    Require(health.FavoriteCount == 1 && health.PlatformCount == 2, "Library health favorite and platform counts failed.");
    var cleanup = LibraryMaintenance.PlanMissingEntryCleanup(ordered);
    Require(cleanup.RemovedCount == 1 && cleanup.RemovedEntries.Single().Id == second.Id, "Missing-entry cleanup must identify unavailable targets.");
    Require(cleanup.RetainedEntries.Single().Id == first.Id, "Missing-entry cleanup must preserve available entries.");
    Require(cleanup.RemovedEntries[0].Title == second.Title && cleanup.RemovedEntries[0].Platform == second.Platform, "Cleanup planning must preserve removed metadata for confirmation.");
    Require(GameLibrary.Filter(ordered, "", availableOnly: true).Single().Id == first.Id, "Available-only filtering must hide missing targets.");
    var launchTime = new DateTimeOffset(2026, 8, 30, 10, 0, 0, TimeSpan.FromHours(2));
    var played = GameLibrary.RecordLaunch(first, launchTime);
    Require(played.LastPlayedUtc == launchTime.ToUniversalTime(), "Launch history must be stored in UTC.");
    var cleared = GameLibrary.ClearPlayHistory(played);
    Require(cleared.LastPlayedUtc is null && cleared.Id == first.Id && cleared.Favorite == first.Favorite, "Clearing play history must preserve game metadata.");

    var edited = GameLibrary.UpdateLaunchMetadata(first, " Renamed ", " Handheld ", " --fullscreen ");
    Require(edited.Title == "Renamed" && edited.Platform == "Handheld" && edited.Arguments == "--fullscreen", "Launch metadata must be normalized.");
    Require(edited.Id == first.Id && edited.ExecutablePath == first.ExecutablePath && edited.Favorite == first.Favorite, "Editing metadata must preserve identity, target, and state.");
    var invalidMetadataRejected = false;
    try { GameLibrary.UpdateLaunchMetadata(first, " ", "Windows", ""); }
    catch (InvalidDataException) { invalidMetadataRejected = true; }
    Require(invalidMetadataRejected, "Blank edited titles must be rejected.");

    var relocated = GameLibrary.Relocate(first, secondPath);
    Require(relocated.ExecutablePath == Path.GetFullPath(secondPath), "Relocation must update the launch target.");
    Require(relocated.Id == first.Id && relocated.Title == first.Title && relocated.Favorite == first.Favorite, "Relocation must preserve entry metadata.");

    var store = new LibraryStore(Path.Combine(root, "data", "library.json"));
    await store.SaveAsync(ordered);
    var loaded = await store.LoadAsync();
    Require(loaded.Count == 2 && loaded.Any(x => x.Title == "Alpha"), "Atomic JSON round trip failed.");
    var backupPath = Path.Combine(root, "backup", "library-backup.json");
    await store.ExportAsync(backupPath);
    Require(File.Exists(backupPath), "Library export must create a backup.");
    await store.SaveAsync(new[] { second });
    var restored = await store.ImportAsync(backupPath);
    Require(restored.Count == 2 && (await store.LoadAsync()).Any(x => x.Id == first.Id), "Library import must validate and atomically restore the backup.");
    var missingBackupRejected = false;
    try { await store.ImportAsync(Path.Combine(root, "missing.json")); }
    catch (FileNotFoundException) { missingBackupRejected = true; }
    Require(missingBackupRejected, "Missing library backups must be rejected.");

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

    var steamRoot = Path.Combine(root, "Steam");
    var secondarySteamLibrary = Path.Combine(root, "SteamLibrary");
    var steamApps = Path.Combine(steamRoot, "steamapps");
    var secondarySteamApps = Path.Combine(secondarySteamLibrary, "steamapps");
    Directory.CreateDirectory(steamApps);
    Directory.CreateDirectory(secondarySteamApps);
    var escapedSecondary = secondarySteamLibrary.Replace(@"\", @"\\", StringComparison.Ordinal);
    await File.WriteAllTextAsync(
        Path.Combine(steamApps, "libraryfolders.vdf"),
        "\"libraryfolders\"\n{\n  \"1\"\n  {\n    \"path\" \"" + escapedSecondary + "\"\n  }\n}");
    await File.WriteAllTextAsync(
        Path.Combine(steamApps, "appmanifest_100.acf"),
        "\"AppState\"\n{\n  \"appid\" \"100\"\n  \"name\" \"Steam Alpha\"\n}");
    await File.WriteAllTextAsync(
        Path.Combine(secondarySteamApps, "appmanifest_200.acf"),
        "\"AppState\"\n{\n  \"appid\" \"200\"\n  \"name\" \"Steam Beta\"\n}");
    await File.WriteAllTextAsync(
        Path.Combine(secondarySteamApps, "appmanifest_invalid.acf"),
        "\"AppState\"\n{\n  \"name\" \"Missing ID\"\n}");

    var steamShortcuts = Path.Combine(root, "generated-steam-shortcuts");
    var steamDiscovery = SteamDiscovery.Discover(
        steamRoot,
        steamShortcuts,
        Array.Empty<GameEntry>());
    Require(steamDiscovery.Entries.Count == 2, "Steam discovery must import games from primary and secondary libraries.");
    Require(steamDiscovery.ManifestCount == 3 && steamDiscovery.InvalidManifestCount == 1, "Steam discovery must report invalid manifests.");
    Require(steamDiscovery.Entries.All(x => x.Platform == "Steam"), "Steam entries must retain storefront metadata.");
    Require(steamDiscovery.Entries.All(x => File.Exists(x.ExecutablePath)), "Steam discovery must create local protocol shortcuts.");
    var steamShortcutPayload = await File.ReadAllTextAsync(
        steamDiscovery.Entries.Single(x => x.Title == "Steam Alpha").ExecutablePath);
    Require(steamShortcutPayload.Contains("steam://rungameid/100", StringComparison.Ordinal), "Steam shortcut must use the validated app ID.");
    var repeatedSteamDiscovery = SteamDiscovery.Discover(
        steamRoot,
        steamShortcuts,
        steamDiscovery.Entries);
    Require(repeatedSteamDiscovery.Entries.Count == 0 && repeatedSteamDiscovery.DuplicateCount == 2, "Steam discovery must deduplicate existing launch targets.");
    var missingSteamDiscovery = SteamDiscovery.Discover(
        Path.Combine(root, "missing-steam"),
        steamShortcuts,
        Array.Empty<GameEntry>());
    Require(missingSteamDiscovery.ManifestCount == 0 && missingSteamDiscovery.Entries.Count == 0, "Missing Steam installations must be a safe no-op.");

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
