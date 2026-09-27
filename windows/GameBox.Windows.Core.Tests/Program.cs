using System.Net;
using System.Text.Json;
using GameBox.Windows.Core;
using GameBox.Windows;

if (args.Length > 0 && string.Equals(args[0], "--live-companion", StringComparison.OrdinalIgnoreCase))
{
    await RunLiveCompanionProbeAsync(args.Skip(1).ToArray());
    return;
}
if (args.Length > 0 && string.Equals(args[0], "--live-companion-discovery", StringComparison.OrdinalIgnoreCase))
{
    await RunLiveCompanionDiscoveryProbeAsync(args.Skip(1).ToArray());
    return;
}
if (args.Length > 0 && string.Equals(args[0], "--live-companion-configuration", StringComparison.OrdinalIgnoreCase))
{
    await RunLiveCompanionConfigurationProbeAsync(args.Skip(1).ToArray());
    return;
}
if (args.Length > 0 && string.Equals(args[0], "--live-companion-save", StringComparison.OrdinalIgnoreCase))
{
    await RunLiveCompanionSaveProbeAsync(args.Skip(1).ToArray());
    return;
}
if (args.Length > 0 && string.Equals(args[0], "--live-companion-content", StringComparison.OrdinalIgnoreCase))
{
    await RunLiveCompanionContentProbeAsync(args.Skip(1).ToArray());
    return;
}
if (args.Length > 0 && string.Equals(args[0], "--live-companion-content-repeat", StringComparison.OrdinalIgnoreCase))
{
    await RunLiveCompanionRepeatedContentProbeAsync(args.Skip(1).ToArray());
    return;
}
if (args.Length > 0 && string.Equals(args[0], "--live-companion-content-cancel", StringComparison.OrdinalIgnoreCase))
{
    await RunLiveCompanionCancelledContentProbeAsync(args.Skip(1).ToArray());
    return;
}

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

    var epicManifestRoot = Path.Combine(root, "EpicManifests");
    var epicInstallRoot = Path.Combine(root, "EpicInstalled", "Epic Alpha");
    Directory.CreateDirectory(epicManifestRoot);
    Directory.CreateDirectory(Path.Combine(epicInstallRoot, "Binaries", "Win64"));
    var epicExecutable = Path.Combine(epicInstallRoot, "Binaries", "Win64", "EpicAlpha.exe");
    await File.WriteAllTextAsync(epicExecutable, "");
    await File.WriteAllTextAsync(
        Path.Combine(epicManifestRoot, "valid.item"),
        JsonSerializer.Serialize(new {
            DisplayName = "Epic Alpha",
            InstallLocation = epicInstallRoot,
            LaunchExecutable = "Binaries/Win64/EpicAlpha.exe"
        }));
    var outsideExecutable = Path.Combine(root, "outside.exe");
    await File.WriteAllTextAsync(outsideExecutable, "");
    await File.WriteAllTextAsync(
        Path.Combine(epicManifestRoot, "traversal.item"),
        JsonSerializer.Serialize(new {
            DisplayName = "Traversal",
            InstallLocation = epicInstallRoot,
            LaunchExecutable = "../../outside.exe"
        }));
    await File.WriteAllTextAsync(
        Path.Combine(epicManifestRoot, "missing.item"),
        JsonSerializer.Serialize(new {
            DisplayName = "Missing",
            InstallLocation = epicInstallRoot,
            LaunchExecutable = "missing.exe"
        }));
    await File.WriteAllTextAsync(
        Path.Combine(epicManifestRoot, "oversized.item"),
        new string('x', checked((int)EpicDiscovery.MaxManifestBytes + 1)));

    var epicDiscovery = EpicDiscovery.Discover(
        epicManifestRoot,
        Array.Empty<GameEntry>());
    Require(epicDiscovery.Entries.Count == 1 && epicDiscovery.Entries[0].Title == "Epic Alpha", "Epic discovery must import valid installed games.");
    Require(epicDiscovery.Entries[0].ExecutablePath == Path.GetFullPath(epicExecutable), "Epic discovery must resolve the manifest launch executable.");
    Require(epicDiscovery.Entries[0].Platform == "Epic Games", "Epic discovery must retain storefront metadata.");
    Require(epicDiscovery.ManifestCount == 4 && epicDiscovery.InvalidManifestCount == 3, "Epic discovery must reject traversal, missing, and oversized manifests.");
    var repeatedEpicDiscovery = EpicDiscovery.Discover(
        epicManifestRoot,
        epicDiscovery.Entries);
    Require(repeatedEpicDiscovery.Entries.Count == 0 && repeatedEpicDiscovery.DuplicateCount == 1, "Epic discovery must deduplicate existing targets.");
    var missingEpicDiscovery = EpicDiscovery.Discover(
        Path.Combine(root, "missing-epic"),
        Array.Empty<GameEntry>());
    Require(missingEpicDiscovery.ManifestCount == 0 && missingEpicDiscovery.Entries.Count == 0, "Missing Epic manifests must be a safe no-op.");

    var gogRoot = Path.Combine(root, "gog");
    var gogInstallRoot = Path.Combine(gogRoot, "GogAlpha");
    Directory.CreateDirectory(gogInstallRoot);
    var gogExecutable = Path.Combine(gogInstallRoot, "GogAlpha.exe");
    await File.WriteAllTextAsync(gogExecutable, "");
    await File.WriteAllTextAsync(
        Path.Combine(gogInstallRoot, "goggame-123.info"),
        JsonSerializer.Serialize(new {
            name = "GOG Alpha",
            playTasks = new[] { new { path = "GogAlpha.exe", isPrimary = true } }
        }));
    await File.WriteAllTextAsync(
        Path.Combine(gogInstallRoot, "goggame-124.info"),
        JsonSerializer.Serialize(new {
            name = "Traversal",
            playTasks = new[] { new { path = "../outside.exe", isPrimary = true } }
        }));

    var gogDiscovery = GogDiscovery.Discover(new[] { gogRoot }, Array.Empty<GameEntry>());
    Require(gogDiscovery.Entries.Count == 1 && gogDiscovery.Entries[0].Title == "GOG Alpha", "GOG discovery must import valid installed games.");
    Require(gogDiscovery.Entries[0].ExecutablePath == Path.GetFullPath(gogExecutable), "GOG discovery must resolve the primary play task.");
    Require(gogDiscovery.Entries[0].Platform == "GOG", "GOG discovery must retain storefront metadata.");
    Require(gogDiscovery.ManifestCount == 2 && gogDiscovery.InvalidManifestCount == 1, "GOG discovery must reject traversal manifests.");
    var repeatedGogDiscovery = GogDiscovery.Discover(new[] { gogRoot }, gogDiscovery.Entries);
    Require(repeatedGogDiscovery.Entries.Count == 0 && repeatedGogDiscovery.DuplicateCount == 1, "GOG discovery must deduplicate existing targets.");
    var missingGogDiscovery = GogDiscovery.Discover(new[] { Path.Combine(root, "missing-gog") }, Array.Empty<GameEntry>());
    Require(missingGogDiscovery.ManifestCount == 0 && missingGogDiscovery.Entries.Count == 0, "Missing GOG roots must be a safe no-op.");

    var moonlightPath = Path.Combine(root, "moonlight.exe");
    await File.WriteAllTextAsync(moonlightPath, "");
    var moonlightSession = MoonlightSession.Create(moonlightPath, "Gaming-PC.local", "Steam Big Picture");
    Require(moonlightSession.Platform == "Moonlight", "Moonlight sessions must retain platform metadata.");
    Require(moonlightSession.Title == "Steam Big Picture on gaming-pc.local", "Moonlight sessions must normalize display metadata.");
    Require(moonlightSession.Arguments == "stream \"gaming-pc.local\" \"Steam Big Picture\"", "Moonlight sessions must produce bounded CLI arguments.");
    Require(MoonlightSession.NormalizeHost("[2001:db8::1]") == "2001:db8::1", "Moonlight sessions must normalize IPv6 hosts.");
    RequireThrows<ArgumentException>(() => MoonlightSession.NormalizeHost("https://host/path"), "Moonlight hosts must reject URLs and paths.");
    RequireThrows<ArgumentException>(() => MoonlightSession.NormalizeApplicationName(""), "Moonlight sessions must require an application name.");
    var nonExeMoonlight = Path.Combine(root, "moonlight.cmd");
    await File.WriteAllTextAsync(nonExeMoonlight, "");
    RequireThrows<InvalidDataException>(() => MoonlightSession.Create(nonExeMoonlight, "host.local", "Desktop"), "Moonlight sessions must require an EXE target.");

    var companionSecret = CompanionProtocol.CreatePairingSecret();
    Require(companionSecret.Length == 64, "Companion pairing secrets must use 256 bits of entropy.");
    var companionAuth = CompanionProtocol.CreateAuthorization(companionSecret, "GET", "/v1/status", 1_700_000_000);
    Require(CompanionProtocol.VerifyAuthorization(companionSecret, "GET", "/v1/status", companionAuth, 1_700_000_030), "Companion authorization must verify a matching request.");
    Require(!CompanionProtocol.VerifyAuthorization(companionSecret, "POST", "/v1/status", companionAuth, 1_700_000_030), "Companion authorization must bind the request method.");
    Require(!CompanionProtocol.VerifyAuthorization(companionSecret, "GET", "/v1/status", companionAuth, 1_700_000_121), "Companion authorization must expire stale requests.");
    RequireThrows<ArgumentException>(() => CompanionProtocol.CreateAuthorization(companionSecret, "GET", "/v1/../status", 1_700_000_000), "Companion authorization must reject path traversal.");

    HttpRequestMessage? companionRequest = null;
    using var companionClient = new HttpClient(new StubHttpMessageHandler(request =>
    {
        companionRequest = request;
        return new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent("{\"protocolVersion\":1,\"deviceName\":\"Living Room\",\"status\":\"ready\"}")
        };
    }));
    var companionStatus = await new CompanionStatusClient(companionClient).GetStatusAsync(
        "192.168.1.22", 49_500, companionSecret);
    Require(companionStatus.DeviceName == "Living Room" && companionStatus.Status == "ready", "Companion status response must be parsed.");
    Require(companionRequest?.RequestUri?.ToString() == "http://192.168.1.22:49500/v1/status", "Companion status must use the configured LAN endpoint.");
    Require(companionRequest?.Headers.Contains(CompanionProtocol.AuthorizationHeader) == true, "Companion status must authenticate its request.");

    using var incompleteStatusClient = new HttpClient(new StubHttpMessageHandler(_ =>
        new HttpResponseMessage(HttpStatusCode.OK) { Content = new StringContent("{}") }));
    var incompleteStatusRejected = false;
    try
    {
        await new CompanionStatusClient(incompleteStatusClient).GetStatusAsync(
            "192.168.1.22", 49_500, companionSecret);
    }
    catch (InvalidDataException) { incompleteStatusRejected = true; }
    Require(incompleteStatusRejected,
        "Companion status must classify incomplete JSON as invalid data.");

    using var rejectedCompanionClient = new HttpClient(new StubHttpMessageHandler(_ => new HttpResponseMessage(HttpStatusCode.Unauthorized)));
    var rejectedCompanionSecret = false;
    try { await new CompanionStatusClient(rejectedCompanionClient).GetStatusAsync("192.168.1.22", 49_500, companionSecret); }
    catch (UnauthorizedAccessException) { rejectedCompanionSecret = true; }
    Require(rejectedCompanionSecret, "Companion status must surface pairing rejection.");

    using var libraryClient = new HttpClient(new StubHttpMessageHandler(_ => new HttpResponseMessage(HttpStatusCode.OK)
    {
        Content = new StringContent("{\"protocolVersion\":1,\"games\":[{\"id\":\"nes-1\",\"title\":\"Galaxy Patrol\",\"platform\":\"NES\",\"installState\":\"INSTALLED\",\"favorite\":true,\"minutesPlayed\":12,\"savePresent\":true}]}")
    }));
    var pairedLibrary = await new CompanionStatusClient(libraryClient).GetLibraryAsync("192.168.1.22", 49_500, companionSecret);
    Require(pairedLibrary.Count == 1 && pairedLibrary[0].Title == "Galaxy Patrol" && pairedLibrary[0].SavePresent, "Companion library must parse paired GameBox metadata.");
    RequireThrows<ArgumentException>(() => new CompanionStatusClient(new HttpClient()).GetStatusAsync("http://host", 49_500, companionSecret).GetAwaiter().GetResult(), "Companion status must reject host URLs.");

    var saveBytes = System.Text.Encoding.UTF8.GetBytes("SAVE-DATA");
    var saveHash = Convert.ToHexString(System.Security.Cryptography.SHA256.HashData(saveBytes)).ToLowerInvariant();
    HttpRequestMessage? saveDownloadRequest = null;
    using var saveDownloadHttp = new HttpClient(new StubHttpMessageHandler(request =>
    {
        saveDownloadRequest = request;
        return new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(JsonSerializer.Serialize(new
            {
                protocolVersion = 1,
                gameId = "nes-1",
                updatedAtMillis = 1234,
                sha256 = saveHash,
                payloadBase64 = Convert.ToBase64String(saveBytes)
            }))
        };
    }));
    var saveDownload = await new CompanionSaveClient(saveDownloadHttp).DownloadAsync(
        "192.168.1.22", 49_500, companionSecret, "nes-1");
    Require(saveDownload.Bytes.SequenceEqual(saveBytes), "Companion save download must verify and return exact bytes.");
    Require(saveDownloadRequest?.RequestUri?.AbsolutePath == "/v1/saves/nes-1", "Save download must bind the selected game ID.");
    Require(saveDownloadRequest?.Headers.Contains(CompanionProtocol.AuthorizationHeader) == true, "Save download must authenticate.");

    var retrySaveAttempts = 0;
    using var retrySaveHttp = new HttpClient(new StubHttpMessageHandler(_ =>
    {
        retrySaveAttempts++;
        if (retrySaveAttempts == 1) throw new HttpRequestException("transient save read failure");
        return new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(JsonSerializer.Serialize(new
            {
                protocolVersion = 1,
                gameId = "nes-1",
                updatedAtMillis = 1234,
                sha256 = saveHash,
                payloadBase64 = Convert.ToBase64String(saveBytes)
            }))
        };
    }));
    var retriedSave = await new CompanionSaveClient(retrySaveHttp).DownloadWithRetryAsync(
        "192.168.1.22", 49_500, companionSecret, "nes-1");
    Require(retrySaveAttempts == 2 && retriedSave.Bytes.SequenceEqual(saveBytes),
        "Managed-save downloads must retry transient LAN failures without changing payload bytes.");

    HttpRequestMessage? saveUploadRequest = null;
    using var saveUploadHttp = new HttpClient(new StubHttpMessageHandler(request =>
    {
        saveUploadRequest = request;
        return new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(JsonSerializer.Serialize(new
            {
                protocolVersion = 1,
                gameId = "nes-1",
                updatedAtMillis = 2345,
                sha256 = saveHash,
                conflictPreserved = true
            }))
        };
    }));
    var saveUpload = await new CompanionSaveClient(saveUploadHttp).UploadAsync(
        "192.168.1.22", 49_500, companionSecret, "nes-1", saveBytes);
    Require(saveUpload.ConflictPreserved, "Save upload must surface Android conflict preservation.");
    Require(saveUploadRequest?.Method == HttpMethod.Put &&
        saveUploadRequest.Content?.Headers.ContentType?.MediaType == "application/octet-stream",
        "Save upload must use a bounded binary PUT.");
    var saveUploadAuthorization = saveUploadRequest!.Headers
        .GetValues(CompanionProtocol.AuthorizationHeader).Single();
    var authorizationNow = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
    Require(
        CompanionProtocol.VerifyAuthorization(
            companionSecret, "PUT", "/v1/saves/nes-1", saveUploadAuthorization,
            authorizationNow, bodySha256: saveHash),
        "Save upload authorization must bind the exact payload checksum.");
    Require(
        !CompanionProtocol.VerifyAuthorization(
            companionSecret, "PUT", "/v1/saves/nes-1", saveUploadAuthorization,
            authorizationNow, bodySha256: new string('0', 64)),
        "Save upload authorization must reject altered payload bytes.");
    var retrySaveUploadAttempts = 0;
    using var retrySaveUploadHttp = new HttpClient(new StubHttpMessageHandler(_ =>
    {
        retrySaveUploadAttempts++;
        if (retrySaveUploadAttempts == 1) throw new HttpRequestException("transient save write failure");
        return new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(JsonSerializer.Serialize(new
            {
                protocolVersion = 1,
                gameId = "nes-1",
                updatedAtMillis = 2345,
                sha256 = saveHash,
                conflictPreserved = false
            }))
        };
    }));
    var retriedSaveUpload = await new CompanionSaveClient(retrySaveUploadHttp).UploadWithRetryAsync(
        "192.168.1.22", 49_500, companionSecret, "nes-1", saveBytes);
    Require(retrySaveUploadAttempts == 2 && !retriedSaveUpload.ConflictPreserved,
        "Managed-save uploads must retry transient LAN failures after idempotent replay support.");
    using var incompleteSaveClient = new HttpClient(new StubHttpMessageHandler(_ =>
        new HttpResponseMessage(HttpStatusCode.OK) { Content = new StringContent("{}") }));
    var incompleteSaveRejected = false;
    try
    {
        await new CompanionSaveClient(incompleteSaveClient).UploadAsync(
            "192.168.1.22", 49_500, companionSecret, "nes-1", saveBytes);
    }
    catch (InvalidDataException) { incompleteSaveRejected = true; }
    Require(incompleteSaveRejected,
        "Companion save upload must classify incomplete confirmations as invalid data.");
    RequireThrows<ArgumentException>(() =>
        new CompanionSaveClient(new HttpClient()).DownloadAsync(
            "192.168.1.22", 49_500, companionSecret, "../escape").GetAwaiter().GetResult(),
        "Companion save transfer must reject traversal IDs.");

    var contentPath = Path.Combine(root, "Galaxy Patrol.nes");
    var contentBytes = System.Text.Encoding.UTF8.GetBytes("OWNED-NES-CONTENT");
    await File.WriteAllBytesAsync(contentPath, contentBytes);
    var contentHash = Convert.ToHexString(
        System.Security.Cryptography.SHA256.HashData(contentBytes)).ToLowerInvariant();
    HttpRequestMessage? contentUploadRequest = null;
    using var contentUploadHttp = new HttpClient(new StubHttpMessageHandler(request =>
    {
        contentUploadRequest = request;
        return new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(JsonSerializer.Serialize(new
            {
                protocolVersion = 1,
                gameId = "galaxy-patrol",
                fileName = "Galaxy Patrol.nes",
                sizeBytes = contentBytes.LongLength,
                sha256 = contentHash
            }))
        };
    }));
    var contentUpload = await new CompanionContentClient(contentUploadHttp).UploadAsync(
        "192.168.1.22", 49_500, companionSecret, "galaxy-patrol", contentPath);
    Require(contentUpload.Sha256 == contentHash && contentUpload.SizeBytes == contentBytes.LongLength,
        "Content upload must verify Android confirmation.");
    Require(contentUploadRequest?.Method == HttpMethod.Put &&
        contentUploadRequest.RequestUri?.AbsolutePath == "/v1/content/galaxy-patrol" &&
        contentUploadRequest.Content?.Headers.ContentLength == contentBytes.LongLength,
        "Content upload must stream a length-delimited binary PUT.");
    Require(contentUploadRequest!.Headers.GetValues("X-GameBox-Content-SHA256").Single() == contentHash,
        "Content upload must declare its signed SHA-256 before streaming.");
    Require(
        System.Text.Encoding.UTF8.GetString(Convert.FromBase64String(
            contentUploadRequest.Headers.GetValues("X-GameBox-File-Name").Single())) == "Galaxy Patrol.nes",
        "Content upload must carry the selected filename without path data.");
    var contentAuthorization = contentUploadRequest.Headers
        .GetValues(CompanionProtocol.AuthorizationHeader).Single();
    Require(CompanionProtocol.VerifyAuthorization(
            companionSecret, "PUT", "/v1/content/galaxy-patrol", contentAuthorization,
            DateTimeOffset.UtcNow.ToUnixTimeSeconds(), bodySha256: contentHash),
        "Content upload authorization must bind the exact file checksum.");

    var largeContentPath = Path.Combine(root, "Large Galaxy Patrol.nes");
    const int largeContentBytes = 64 * 1024 * 1024;
    await using (var largeContent = new FileStream(
        largeContentPath, FileMode.CreateNew, FileAccess.Write, FileShare.Read,
        1024 * 1024, FileOptions.Asynchronous | FileOptions.SequentialScan))
    {
        var block = new byte[1024 * 1024];
        for (var offset = 0; offset < largeContentBytes; offset += block.Length)
            await largeContent.WriteAsync(block);
    }
    var largeContentHandler = new StreamingContentConfirmationHandler();
    using var largeContentHttp = new HttpClient(largeContentHandler);
    var largeContentUpload = await new CompanionContentClient(largeContentHttp).UploadAsync(
        "192.168.1.22", 49_500, companionSecret, "galaxy-patrol", largeContentPath);
    Require(largeContentHandler.BytesRead == largeContentBytes &&
        largeContentUpload.SizeBytes == largeContentBytes &&
        string.Equals(largeContentHandler.Sha256, largeContentUpload.Sha256, StringComparison.OrdinalIgnoreCase),
        "Content upload must stream and checksum-verify a 64 MiB local boundary fixture.");

    var retryContentUploadAttempts = 0;
    using var retryContentUploadHttp = new HttpClient(new StubHttpMessageHandler(_ =>
    {
        retryContentUploadAttempts++;
        if (retryContentUploadAttempts == 1) throw new HttpRequestException("transient content write failure");
        return new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(JsonSerializer.Serialize(new
            {
                protocolVersion = 1,
                gameId = "galaxy-patrol",
                fileName = "Galaxy Patrol.nes",
                sizeBytes = contentBytes.LongLength,
                sha256 = contentHash
            }))
        };
    }));
    var retriedContentUpload = await new CompanionContentClient(retryContentUploadHttp).UploadWithRetryAsync(
        "192.168.1.22", 49_500, companionSecret, "galaxy-patrol", contentPath);
    Require(retryContentUploadAttempts == 2 && retriedContentUpload.Sha256 == contentHash,
        "Owned-content uploads must retry transient LAN failures after idempotent replay support.");
    using var cancelledContent = new CancellationTokenSource();
    cancelledContent.Cancel();
    var cancelledContentAttempts = 0;
    using var cancelledContentHttp = new HttpClient(new StubHttpMessageHandler(_ =>
    {
        cancelledContentAttempts++;
        return new HttpResponseMessage(HttpStatusCode.OK);
    }));
    var cancelledContentObserved = false;
    try
    {
        await new CompanionContentClient(cancelledContentHttp).UploadAsync(
            "192.168.1.22", 49_500, companionSecret, "galaxy-patrol", contentPath,
            cancelledContent.Token);
    }
    catch (OperationCanceledException) { cancelledContentObserved = true; }
    Require(cancelledContentObserved && cancelledContentAttempts == 0,
        "Owned-content uploads must honor caller cancellation before network access.");
    using var inFlightCancellation = new CancellationTokenSource();
    var inFlightReadStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
    var inFlightBytesRead = 0L;
    using var inFlightContentHttp = new HttpClient(new InFlightUploadCancellationHandler(
        bytesRead =>
        {
            inFlightBytesRead = bytesRead;
            inFlightReadStarted.TrySetResult();
        }));
    var inFlightCancellationObserved = false;
    var inFlightUpload = new CompanionContentClient(inFlightContentHttp).UploadAsync(
        "192.168.1.22", 49_500, companionSecret, "galaxy-patrol", contentPath,
        inFlightCancellation.Token);
    await inFlightReadStarted.Task.WaitAsync(TimeSpan.FromSeconds(2));
    inFlightCancellation.Cancel();
    try
    {
        await inFlightUpload;
    }
    catch (OperationCanceledException) { inFlightCancellationObserved = true; }
    Require(inFlightCancellationObserved && inFlightBytesRead > 0,
        "Owned-content uploads must propagate cancellation after request streaming begins.");
    using var oversizedContentResponse = new HttpClient(new StubHttpMessageHandler(_ =>
        new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new UnknownLengthHttpContent(new string('x', 65 * 1024))
        }));
    var oversizedContentRejected = false;
    try
    {
        await new CompanionContentClient(oversizedContentResponse).UploadAsync(
            "192.168.1.22", 49_500, companionSecret, "galaxy-patrol", contentPath);
    }
    catch (InvalidDataException) { oversizedContentRejected = true; }
    Require(oversizedContentRejected,
        "Content upload must reject oversized confirmations before buffering them.");
    using var mismatchedContentResponse = new HttpClient(new StubHttpMessageHandler(_ =>
        new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(JsonSerializer.Serialize(new
            {
                protocolVersion = 1,
                gameId = "different-game",
                fileName = "Galaxy Patrol.nes",
                sizeBytes = contentBytes.LongLength,
                sha256 = contentHash
            }))
        }));
    var mismatchedContentRejected = false;
    try
    {
        await new CompanionContentClient(mismatchedContentResponse).UploadAsync(
            "192.168.1.22", 49_500, companionSecret, "galaxy-patrol", contentPath);
    }
    catch (InvalidDataException) { mismatchedContentRejected = true; }
    Require(mismatchedContentRejected,
        "Content upload must reject a confirmation for a different game.");
    using var malformedContentResponse = new HttpClient(new StubHttpMessageHandler(_ =>
        new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent("{}")
        }));
    var malformedContentRejected = false;
    try
    {
        await new CompanionContentClient(malformedContentResponse).UploadAsync(
            "192.168.1.22", 49_500, companionSecret, "galaxy-patrol", contentPath);
    }
    catch (InvalidDataException) { malformedContentRejected = true; }
    Require(malformedContentRejected,
        "Content upload must classify incomplete confirmations as invalid data.");
    RequireThrows<ArgumentException>(() =>
        new CompanionContentClient(new HttpClient()).UploadAsync(
            "192.168.1.22", 49_500, companionSecret, "../escape", contentPath)
            .GetAwaiter().GetResult(),
        "Content upload must reject traversal IDs.");
    RequireThrows<ArgumentException>(() =>
        new CompanionContentClient(new HttpClient()).UploadAsync(
            "192.168.1.22", 49_500, companionSecret, "Galaxy-Patrol", contentPath)
            .GetAwaiter().GetResult(),
        "Content upload must reject IDs that the Android route does not accept.");

    HttpRequestMessage? favoriteRequest = null;
    using var favoriteHttp = new HttpClient(new StubHttpMessageHandler(request =>
    {
        favoriteRequest = request;
        return new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(
                "{\"protocolVersion\":1,\"gameId\":\"nes-1\",\"favorite\":false}")
        };
    }));
    var unfavorited = await new CompanionStatusClient(favoriteHttp).SetFavoriteAsync(
        "192.168.1.22", 49_500, companionSecret, pairedLibrary[0], false);
    Require(!unfavorited.Favorite && unfavorited.Id == pairedLibrary[0].Id,
        "Paired favorite management must preserve game identity and update local state.");
    Require(favoriteRequest?.Method == HttpMethod.Put &&
        favoriteRequest.RequestUri?.AbsolutePath == "/v1/library/nes-1/favorite/off" &&
        favoriteRequest.Content is null,
        "Paired favorite management must use a body-free idempotent PUT.");
    var favoriteAuthorization = favoriteRequest!.Headers
        .GetValues(CompanionProtocol.AuthorizationHeader).Single();
    Require(CompanionProtocol.VerifyAuthorization(
            companionSecret, "PUT", "/v1/library/nes-1/favorite/off",
            favoriteAuthorization, DateTimeOffset.UtcNow.ToUnixTimeSeconds()),
        "Paired favorite management authorization must bind the desired state.");

    var favoriteRetryAttempts = 0;
    using var favoriteRetryHttp = new HttpClient(new StubHttpMessageHandler(_ =>
    {
        favoriteRetryAttempts++;
        if (favoriteRetryAttempts == 1)
            return new HttpResponseMessage(HttpStatusCode.TooManyRequests);
        return new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(
                "{\"protocolVersion\":1,\"gameId\":\"nes-1\",\"favorite\":true}")
        };
    }));
    var retriedFavorite = await new CompanionStatusClient(favoriteRetryHttp).SetFavoriteWithRetryAsync(
        "192.168.1.22", 49_500, companionSecret, pairedLibrary[0], true);
    Require(retriedFavorite.Favorite && favoriteRetryAttempts == 2,
        "Favorite updates must retry transient HTTP responses because the PUT is idempotent.");

    var pairingPath = Path.Combine(root, "pairing", "paired-device.json");
    var pairingStore = new PairingProfileStore(pairingPath);
    var pairingProfile = new PairingProfile("192.168.1.22", 49_500, companionSecret);
    await pairingStore.SaveAsync(pairingProfile);
    var protectedProfileBytes = await File.ReadAllTextAsync(pairingPath);
    Require(!protectedProfileBytes.Contains(companionSecret, StringComparison.Ordinal),
        "Persisted pairing profile must never contain the plaintext secret.");
    var restoredPairing = await pairingStore.LoadAsync();
    Require(restoredPairing == pairingProfile,
        "DPAPI pairing profile must round-trip for the current Windows user.");
    pairingStore.Delete();
    Require(await pairingStore.LoadAsync() is null && !File.Exists(pairingPath),
        "Forgetting a pairing must remove the durable profile.");
    Directory.CreateDirectory(Path.GetDirectoryName(pairingPath)!);
    await File.WriteAllTextAsync(pairingPath, "{invalid");
    var invalidPairingRejected = false;
    try { await pairingStore.LoadAsync(); }
    catch (InvalidDataException) { invalidPairingRejected = true; }
    Require(invalidPairingRejected, "Malformed pairing profiles must fail closed.");
    pairingStore.Delete();
    await File.WriteAllTextAsync(
        pairingPath,
        "{\"host\":\"192.168.1.22\",\"port\":49500}");
    var incompletePairingRejected = false;
    try { await pairingStore.LoadAsync(); }
    catch (InvalidDataException) { incompletePairingRejected = true; }
    Require(incompletePairingRejected,
        "Pairing profiles missing protected secrets must fail closed as invalid data.");
    pairingStore.Delete();

    var reconnectAttempts = 0;
    using var reconnectHttp = new HttpClient(new StubHttpMessageHandler(_ =>
    {
        reconnectAttempts++;
        if (reconnectAttempts < 3) throw new HttpRequestException("transient LAN outage");
        return new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(
                "{\"protocolVersion\":1,\"deviceName\":\"Recovered GameBox\",\"status\":\"ready\"}")
        };
    }));
    var reconnectDelays = 0;
    var recoveredStatus = await CompanionRetryPolicy.ExecuteAsync(
        token => new CompanionStatusClient(reconnectHttp).GetStatusAsync(
            "192.168.1.22", 49_500, companionSecret, token),
        maxAttempts: 3,
        initialDelay: TimeSpan.Zero,
        delay: (_, _) => { reconnectDelays++; return Task.CompletedTask; });
    Require(recoveredStatus.DeviceName == "Recovered GameBox" &&
        reconnectAttempts == 3 && reconnectDelays == 2,
        "Reconnect policy must recover after bounded transient failures.");

    var transientStatusAttempts = 0;
    using var transientStatusHttp = new HttpClient(new StubHttpMessageHandler(_ =>
    {
        transientStatusAttempts++;
        if (transientStatusAttempts == 1)
            return new HttpResponseMessage(HttpStatusCode.ServiceUnavailable);
        return new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(
                "{\"protocolVersion\":1,\"deviceName\":\"Recovered HTTP GameBox\",\"status\":\"ready\"}")
        };
    }));
    var retriedHttpStatus = await new CompanionStatusClient(transientStatusHttp).ReconnectAsync(
        "192.168.1.22", 49_500, companionSecret);
    Require(retriedHttpStatus.DeviceName == "Recovered HTTP GameBox" && transientStatusAttempts == 2,
        "Reconnect policy must retry transient HTTP 5xx responses.");

    var authenticationAttempts = 0;
    var authenticationFailure = false;
    try
    {
        await CompanionRetryPolicy.ExecuteAsync<int>(
            _ => { authenticationAttempts++; throw new UnauthorizedAccessException("rejected"); },
            maxAttempts: 3,
            initialDelay: TimeSpan.Zero,
            delay: (_, _) => Task.CompletedTask);
    }
    catch (UnauthorizedAccessException) { authenticationFailure = true; }
    Require(authenticationFailure && authenticationAttempts == 1,
        "Reconnect policy must never retry rejected authentication.");

    var malformedRetryAttempts = 0;
    var malformedRetryFailure = false;
    try
    {
        await CompanionRetryPolicy.ExecuteAsync<int>(
            _ => { malformedRetryAttempts++; throw new InvalidDataException("malformed payload"); },
            maxAttempts: 3,
            initialDelay: TimeSpan.Zero,
            delay: (_, _) => Task.CompletedTask);
    }
    catch (InvalidDataException) { malformedRetryFailure = true; }
    Require(malformedRetryFailure && malformedRetryAttempts == 1,
        "Reconnect policy must never retry malformed companion payloads.");

    using var cancelledReconnect = new CancellationTokenSource();
    cancelledReconnect.Cancel();
    var cancelledAttempts = 0;
    var callerCancellationObserved = false;
    try
    {
        await CompanionRetryPolicy.ExecuteAsync(
            _ => { cancelledAttempts++; return Task.FromResult(1); },
            cancelledReconnect.Token);
    }
    catch (OperationCanceledException) { callerCancellationObserved = true; }
    Require(callerCancellationObserved && cancelledAttempts == 0,
        "Reconnect policy must honor caller cancellation before network access.");

    var discoveryNonce = "0123456789abcdef0123456789abcdef";
    var discoveryRequest = CompanionDiscoveryProtocol.CreateRequest(discoveryNonce);
    Require(System.Text.Encoding.ASCII.GetString(discoveryRequest) ==
        "GAMEBOX_DISCOVER_V1:" + discoveryNonce,
        "Discovery request must carry the bounded random nonce.");
    var discoveredDevice = CompanionDiscoveryProtocol.ParseResponse(
        System.Text.Encoding.ASCII.GetBytes(
            "GAMEBOX_HERE_V1:" + discoveryNonce + ":49500:" +
            Convert.ToBase64String(System.Text.Encoding.UTF8.GetBytes("Living Room GameBox"))),
        discoveryNonce,
        IPAddress.Parse("192.168.1.22"));
    Require(discoveredDevice is not null &&
        discoveredDevice.Host == "192.168.1.22" &&
        discoveredDevice.Port == 49_500 &&
        discoveredDevice.DeviceName == "Living Room GameBox",
        "Discovery response must bind nonce, sender address, port, and device name.");
    Require(CompanionDiscoveryProtocol.ParseResponse(
        System.Text.Encoding.ASCII.GetBytes(
            "GAMEBOX_HERE_V1:" + new string('0', 32) + ":49500:VGVzdA=="),
        discoveryNonce,
        IPAddress.Loopback) is null,
        "Discovery must ignore replayed or unrelated nonces.");
    RequireThrows<ArgumentException>(
        () => CompanionDiscoveryProtocol.CreateRequest("../escape"),
        "Discovery must reject malformed nonces.");
    RequireThrows<ArgumentOutOfRangeException>(
        () => _ = new CompanionDiscoveryClient(TimeSpan.FromSeconds(11)),
        "Discovery must enforce a bounded response window.");

    HttpRequestMessage? configurationGetRequest = null;
    using var configurationGetHttp = new HttpClient(new StubHttpMessageHandler(request =>
    {
        configurationGetRequest = request;
        return new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(
                "{\"protocolVersion\":1,\"reducedMotion\":true,\"showUnavailableGames\":false,\"showUnavailableShortcuts\":true,\"downloadsUnmeteredOnly\":false}")
        };
    }));
    var deviceConfiguration = await new CompanionConfigurationClient(configurationGetHttp).GetAsync(
        "192.168.1.22", 49_500, companionSecret);
    Require(deviceConfiguration == new CompanionDeviceConfiguration(true, false, true, false),
        "Device configuration GET must parse all managed preferences.");
    Require(configurationGetRequest?.Method == HttpMethod.Get &&
        configurationGetRequest.RequestUri?.AbsolutePath == "/v1/config" &&
        configurationGetRequest.Headers.Contains(CompanionProtocol.AuthorizationHeader),
        "Device configuration GET must authenticate the exact endpoint.");

    HttpRequestMessage? configurationPutRequest = null;
    using var configurationPutHttp = new HttpClient(new StubHttpMessageHandler(request =>
    {
        configurationPutRequest = request;
        return new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(
                "{\"protocolVersion\":1,\"reducedMotion\":true,\"showUnavailableGames\":false,\"showUnavailableShortcuts\":true,\"downloadsUnmeteredOnly\":false}")
        };
    }));
    var desiredConfiguration = new CompanionDeviceConfiguration(true, false, true, false);
    var appliedConfiguration = await new CompanionConfigurationClient(configurationPutHttp).PutAsync(
        "192.168.1.22", 49_500, companionSecret, desiredConfiguration);
    Require(appliedConfiguration == desiredConfiguration,
        "Device configuration PUT must verify the echoed preference snapshot.");
    Require(configurationPutRequest?.Method == HttpMethod.Put &&
        configurationPutRequest.Content is null &&
        configurationPutRequest.Headers.GetValues(
            CompanionConfigurationClient.ConfigurationHeader).Single() == "1010",
        "Device configuration PUT must send exactly four body-free preference flags.");
    var configurationHash = Convert.ToHexString(System.Security.Cryptography.SHA256.HashData(
        System.Text.Encoding.ASCII.GetBytes("1010"))).ToLowerInvariant();
    Require(configurationPutRequest!.Headers.GetValues(
            CompanionConfigurationClient.ContentSha256Header).Single() == configurationHash,
        "Device configuration PUT must declare the exact flag checksum.");
    var configurationAuthorization = configurationPutRequest.Headers
        .GetValues(CompanionProtocol.AuthorizationHeader).Single();
    var configurationNow = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
    Require(CompanionProtocol.VerifyAuthorization(
            companionSecret, "PUT", "/v1/config", configurationAuthorization,
            configurationNow, bodySha256: configurationHash),
        "Device configuration authorization must bind the exact preference flags.");
    Require(!CompanionProtocol.VerifyAuthorization(
            companionSecret, "PUT", "/v1/config", configurationAuthorization,
            configurationNow, bodySha256: new string('0', 64)),
        "Device configuration authorization must reject altered preference flags.");

    var configurationRetryAttempts = 0;
    using var configurationRetryHttp = new HttpClient(new StubHttpMessageHandler(_ =>
    {
        configurationRetryAttempts++;
        if (configurationRetryAttempts == 1)
            return new HttpResponseMessage(HttpStatusCode.RequestTimeout);
        return new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(
                "{\"protocolVersion\":1,\"reducedMotion\":true,\"showUnavailableGames\":false,\"showUnavailableShortcuts\":true,\"downloadsUnmeteredOnly\":false}")
        };
    }));
    var retriedConfiguration = await new CompanionConfigurationClient(configurationRetryHttp)
        .PutWithRetryAsync("192.168.1.22", 49_500, companionSecret, desiredConfiguration);
    Require(retriedConfiguration == desiredConfiguration && configurationRetryAttempts == 2,
        "Configuration updates must retry transient HTTP responses because the PUT is idempotent.");

    using var badConfigurationHttp = new HttpClient(new StubHttpMessageHandler(_ =>
        new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(
                "{\"protocolVersion\":2,\"reducedMotion\":false,\"showUnavailableGames\":false,\"showUnavailableShortcuts\":false,\"downloadsUnmeteredOnly\":false}")
        }));
    var incompatibleConfigurationRejected = false;
    try
    {
        await new CompanionConfigurationClient(badConfigurationHttp).GetAsync(
            "192.168.1.22", 49_500, companionSecret);
    }
    catch (InvalidDataException) { incompatibleConfigurationRejected = true; }
    Require(incompatibleConfigurationRejected,
        "Device configuration must reject incompatible protocol responses.");

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


static void RequireThrows<TException>(Action action, string message) where TException : Exception
{
    try { action(); }
    catch (TException) { return; }
    throw new InvalidOperationException(message);
}

static async Task RunLiveCompanionProbeAsync(string[] args)
{
    if (args.Length != 3 || !int.TryParse(args[1], out var port))
        throw new ArgumentException("Usage: --live-companion <host> <port> <pairing-secret>");

    using var http = new HttpClient();
    var client = new CompanionStatusClient(http);
    var status = await client.GetStatusAsync(args[0], port, args[2]);
    var library = await client.GetLibraryAsync(args[0], port, args[2]);
    Console.WriteLine($"Live Companion status: {status.DeviceName} / {status.Status} / protocol {status.ProtocolVersion}");
    Console.WriteLine($"Live Companion library entries: {library.Count}");
    foreach (var game in library)
        Console.WriteLine($"- {game.Id}: {game.Title} [{game.Platform}] {game.InstallState} favorite={game.Favorite} savePresent={game.SavePresent}");
}

static async Task RunLiveCompanionDiscoveryProbeAsync(string[] args)
{
    if (args.Length != 2 || !int.TryParse(args[1], out var expectedPort))
        throw new ArgumentException("Usage: --live-companion-discovery <expected-host> <expected-port>");

    var devices = await new CompanionDiscoveryClient().DiscoverAsync();
    var device = devices.FirstOrDefault(item =>
        string.Equals(item.Host, args[0], StringComparison.OrdinalIgnoreCase) &&
        item.Port == expectedPort);
    if (device is null)
        throw new InvalidOperationException($"Companion discovery did not find {args[0]}:{expectedPort}.");
    Console.WriteLine($"Live Companion discovery: {device.DeviceName} at {device.Host}:{device.Port}");
}

static async Task RunLiveCompanionConfigurationProbeAsync(string[] args)
{
    if (args.Length != 3 || !int.TryParse(args[1], out var port))
        throw new ArgumentException("Usage: --live-companion-configuration <host> <port> <pairing-secret>");

    using var http = new HttpClient();
    var client = new CompanionConfigurationClient(http);
    var before = await client.GetAsync(args[0], port, args[2]);
    var confirmed = await client.PutAsync(args[0], port, args[2], before);
    if (confirmed != before)
        throw new InvalidDataException("Live Companion configuration round-trip changed device settings.");
    Console.WriteLine(
        $"Live Companion configuration round-trip: reducedMotion={confirmed.ReducedMotion}, " +
        $"showUnavailableGames={confirmed.ShowUnavailableGames}, " +
        $"showUnavailableShortcuts={confirmed.ShowUnavailableShortcuts}, " +
        $"downloadsUnmeteredOnly={confirmed.DownloadsUnmeteredOnly}");
}

static async Task RunLiveCompanionSaveProbeAsync(string[] args)
{
    if (args.Length != 5 || !int.TryParse(args[1], out var port))
        throw new ArgumentException("Usage: --live-companion-save <host> <port> <pairing-secret> <game-id> <payload>");
    if (string.IsNullOrWhiteSpace(args[4]))
        throw new ArgumentException("Live save probe payload must not be empty.", nameof(args));

    var bytes = System.Text.Encoding.UTF8.GetBytes(args[4]);
    using var http = new HttpClient();
    var client = new CompanionSaveClient(http);
    var uploaded = await client.UploadWithRetryAsync(args[0], port, args[2], args[3], bytes);
    var downloaded = await client.DownloadWithRetryAsync(args[0], port, args[2], args[3]);
    if (!downloaded.Bytes.SequenceEqual(bytes) ||
        !string.Equals(downloaded.Sha256, uploaded.Sha256, StringComparison.OrdinalIgnoreCase))
        throw new InvalidDataException("Live Companion save round-trip did not preserve the exact payload.");

    Console.WriteLine(
        $"Live Companion save round-trip: game={uploaded.GameId}, bytes={downloaded.Bytes.Length}, " +
        $"sha256={downloaded.Sha256}, conflictPreserved={uploaded.ConflictPreserved}");
}

static async Task RunLiveCompanionContentProbeAsync(string[] args)
{
    if (args.Length != 5 || !int.TryParse(args[1], out var port))
        throw new ArgumentException("Usage: --live-companion-content <host> <port> <pairing-secret> <game-id> <content-path>");

    using var http = new HttpClient();
    var client = new CompanionContentClient(http);
    var uploaded = await client.UploadWithRetryAsync(args[0], port, args[2], args[3], args[4]);
    Console.WriteLine(
        $"Live Companion content upload: game={uploaded.GameId}, file={uploaded.FileName}, " +
        $"bytes={uploaded.SizeBytes}, sha256={uploaded.Sha256}");
}

static async Task RunLiveCompanionRepeatedContentProbeAsync(string[] args)
{
    if (args.Length != 6 || !int.TryParse(args[1], out var port) ||
        !int.TryParse(args[5], out var repeatCount) || repeatCount is < 1 or > 2048)
        throw new ArgumentException(
            "Usage: --live-companion-content-repeat <host> <port> <pairing-secret> <game-id> <source-path> <repeat-count>");

    var source = new FileInfo(args[4]);
    if (!source.Exists || source.Length <= 0)
        throw new FileNotFoundException("Repeated content probe source does not exist or is empty.", source.FullName);
    if (source.Length > CompanionContentClient.MaxContentBytes / repeatCount)
        throw new InvalidDataException("Repeated content probe would exceed the companion content limit.");

    var temporaryPath = Path.Combine(
        Path.GetTempPath(),
        "gamebox-live-content-" + Guid.NewGuid().ToString("N") + ".nes");
    try
    {
        await using (var input = new FileStream(
            source.FullName, FileMode.Open, FileAccess.Read, FileShare.Read,
            1024 * 1024, FileOptions.Asynchronous | FileOptions.SequentialScan))
        await using (var output = new FileStream(
            temporaryPath, FileMode.CreateNew, FileAccess.Write, FileShare.None,
            1024 * 1024, FileOptions.Asynchronous | FileOptions.SequentialScan))
        {
            var buffer = new byte[1024 * 1024];
            for (var copy = 0; copy < repeatCount; copy++)
            {
                input.Position = 0;
                int read;
                while ((read = await input.ReadAsync(buffer)) > 0)
                    await output.WriteAsync(buffer.AsMemory(0, read));
            }
        }

        using var http = new HttpClient();
        var uploaded = await new CompanionContentClient(http).UploadWithRetryAsync(
            args[0], port, args[2], args[3], temporaryPath);
        Console.WriteLine(
            $"Live Companion repeated content upload: game={uploaded.GameId}, " +
            $"bytes={uploaded.SizeBytes}, sha256={uploaded.Sha256}, repeats={repeatCount}");
    }
    finally
    {
        File.Delete(temporaryPath);
    }
}

static async Task RunLiveCompanionCancelledContentProbeAsync(string[] args)
{
    if (args.Length != 7 || !int.TryParse(args[1], out var port) ||
        !int.TryParse(args[5], out var repeatCount) || repeatCount is < 1 or > 2048 ||
        !int.TryParse(args[6], out var cancelAfterMilliseconds) || cancelAfterMilliseconds is < 1 or > 60_000)
        throw new ArgumentException(
            "Usage: --live-companion-content-cancel <host> <port> <pairing-secret> <game-id> <source-path> <repeat-count> <cancel-after-ms>");

    var source = new FileInfo(args[4]);
    if (!source.Exists || source.Length <= 0)
        throw new FileNotFoundException("Cancelled content probe source does not exist or is empty.", source.FullName);
    if (source.Length > CompanionContentClient.MaxContentBytes / repeatCount)
        throw new InvalidDataException("Cancelled content probe would exceed the companion content limit.");

    var temporaryPath = Path.Combine(
        Path.GetTempPath(),
        "gamebox-live-cancel-content-" + Guid.NewGuid().ToString("N") + ".nes");
    try
    {
        await using (var input = new FileStream(
            source.FullName, FileMode.Open, FileAccess.Read, FileShare.Read,
            1024 * 1024, FileOptions.Asynchronous | FileOptions.SequentialScan))
        await using (var output = new FileStream(
            temporaryPath, FileMode.CreateNew, FileAccess.Write, FileShare.None,
            1024 * 1024, FileOptions.Asynchronous | FileOptions.SequentialScan))
        {
            var buffer = new byte[1024 * 1024];
            for (var copy = 0; copy < repeatCount; copy++)
            {
                input.Position = 0;
                int read;
                while ((read = await input.ReadAsync(buffer)) > 0)
                    await output.WriteAsync(buffer.AsMemory(0, read));
            }
        }

        using var cancellation = new CancellationTokenSource(cancelAfterMilliseconds);
        using var http = new HttpClient();
        try
        {
            await new CompanionContentClient(http).UploadWithRetryAsync(
                args[0], port, args[2], args[3], temporaryPath, cancellation.Token);
            throw new InvalidOperationException("Live content cancellation completed before the request was cancelled.");
        }
        catch (OperationCanceledException) when (cancellation.IsCancellationRequested)
        {
            Console.WriteLine(
                $"Live Companion content cancellation: game={args[3]}, " +
                $"bytes={new FileInfo(temporaryPath).Length}, cancelAfterMs={cancelAfterMilliseconds}");
        }
    }
    finally
    {
        File.Delete(temporaryPath);
    }
}

sealed class BlockingHandler : HttpMessageHandler
{
    protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        await Task.Delay(Timeout.InfiniteTimeSpan, cancellationToken);
        throw new InvalidOperationException("unreachable");
    }
}


sealed class StubHttpMessageHandler(Func<HttpRequestMessage, HttpResponseMessage> responder) : HttpMessageHandler
{
    protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
        => Task.FromResult(responder(request));
}

sealed class InFlightUploadCancellationHandler(Action<long> bytesRead) : HttpMessageHandler
{
    protected override async Task<HttpResponseMessage> SendAsync(
        HttpRequestMessage request,
        CancellationToken cancellationToken)
    {
        if (request.Content is null) throw new InvalidOperationException("Upload content is missing.");
        await using var stream = await request.Content.ReadAsStreamAsync(cancellationToken);
        var buffer = new byte[4096];
        var read = await stream.ReadAsync(buffer, cancellationToken);
        if (read <= 0) throw new InvalidDataException("Upload content was empty.");
        bytesRead(read);
        await Task.Delay(Timeout.InfiniteTimeSpan, cancellationToken);
        throw new InvalidOperationException("unreachable");
    }
}

sealed class StreamingContentConfirmationHandler : HttpMessageHandler
{
    public long BytesRead { get; private set; }
    public string Sha256 { get; private set; } = "";

    protected override async Task<HttpResponseMessage> SendAsync(
        HttpRequestMessage request,
        CancellationToken cancellationToken)
    {
        if (request.Content is null) throw new InvalidOperationException("Upload content is missing.");
        await using var stream = await request.Content.ReadAsStreamAsync(cancellationToken);
        using var sha = System.Security.Cryptography.SHA256.Create();
        var buffer = new byte[1024 * 1024];
        int read;
        while ((read = await stream.ReadAsync(buffer, cancellationToken)) > 0)
        {
            sha.TransformBlock(buffer, 0, read, buffer, 0);
            BytesRead += read;
        }
        sha.TransformFinalBlock(Array.Empty<byte>(), 0, 0);
        Sha256 = Convert.ToHexString(sha.Hash!).ToLowerInvariant();
        var fileName = System.Text.Encoding.UTF8.GetString(Convert.FromBase64String(
            request.Headers.GetValues("X-GameBox-File-Name").Single()));
        var gameId = request.RequestUri?.Segments.Last().TrimEnd('/') ?? "";
        return new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(JsonSerializer.Serialize(new
            {
                protocolVersion = CompanionProtocol.Version,
                gameId,
                fileName,
                sizeBytes = BytesRead,
                sha256 = Sha256,
            }))
        };
    }
}

sealed class UnknownLengthHttpContent(string value) : HttpContent
{
    private readonly byte[] bytes = System.Text.Encoding.UTF8.GetBytes(value);

    protected override Task SerializeToStreamAsync(Stream stream, TransportContext? context) =>
        stream.WriteAsync(bytes).AsTask();

    protected override bool TryComputeLength(out long length)
    {
        length = 0;
        return false;
    }
}
