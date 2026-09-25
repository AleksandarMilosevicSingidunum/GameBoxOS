# Configured game discovery sources

GameBox can persist optional HTTPS discovery sources independently of the trusted
authorized catalog provider.

Two discovery source types are currently usable:

- **External web** — opens a configured HTTPS site or search URL in the browser.
- **GameBox JSON** — downloads a bounded metadata-only JSON manifest and merges its
  titles into the cached Store discovery database.

Neither source type grants a game binary permission to install.

## Settings

Open **Settings → Game discovery sources**, choose a source type and provide a display
name plus HTTPS endpoint. Sources may optionally be limited to comma-separated console
names.

External-web sources may also define a search URL template. Supported placeholders are:

- `{query}` — the current Store text plus the selected console;
- `{title}` — the current Store text or selected discovery title;
- `{platform}` — the currently selected console/platform.

Configured sources can be enabled, disabled, or removed without changing installed
files, save records or the trusted authorized catalog configuration.

## External-web Store behavior

Enabled external-web sources appear directly in Store when they apply to the selected
console. Opening one launches the resolved HTTPS URL in the user's browser. On a game
details screen the same source is resolved against that game's title and platform.

If no Store query or console is selected, GameBox opens the configured base URL instead
of manufacturing an empty search request.

## GameBox JSON schema

A GameBox JSON source is a metadata feed. The endpoint must return schema version 1:

```json
{
  "schemaVersion": 1,
  "games": [
    {
      "id": "provider-game-1",
      "title": "Example Game",
      "platform": "PS2",
      "year": 2005,
      "region": "USA",
      "description": "Optional description",
      "developer": "Optional developer",
      "publisher": "Optional publisher",
      "players": "1-2",
      "rating": 8.5,
      "coverUrl": "https://cdn.example.test/cover.jpg",
      "backgroundUrl": "https://cdn.example.test/background.jpg",
      "logoUrl": "https://cdn.example.test/logo.png",
      "screenshots": [
        "https://cdn.example.test/screen-1.jpg"
      ],
      "detailsUrl": "https://example.test/games/provider-game-1"
    }
  ]
}
```

Only `id`, `title` and `platform` are required. IDs must be unique within one
manifest. Artwork, screenshots and details links must be credential-free HTTPS URLs.

GameBox bounds a JSON response to 2 MiB by default, refuses redirects, caps a manifest
at 5,000 games and retains at most 12 screenshot URLs per title. A configured console
scope also filters the imported manifest: a source configured only for PS2 does not
cache GameCube entries from that feed.

Activating a GameBox JSON source in Store fetches the feed, validates it and writes its
metadata into the same Room discovery cache used by TheGamesDB. Existing favorite flags
are preserved. If the platform already exists, its existing TheGamesDB platform identity
is retained so provider metadata can coexist in the Store.

Provider-supplied game IDs are not used directly as local IDs. GameBox creates a
source-namespaced SHA-256-derived identifier and records the original provider ID as an
external identifier.

## Security and install boundary

Configured discovery sources are metadata/navigation only. They cannot populate a
remote binary URL, checksum, install state or download job.

Remote installation still requires the existing trusted catalog path to supply an HTTPS
source plus an expected SHA-256 checksum, followed by the existing WorkManager staging
and verification pipeline. Local imports still pass through the existing format and
hash-verification flow.

GameBox rejects non-HTTPS discovery URLs, embedded URL credentials, malformed hosts and
URL fragments. Credentials for trusted catalog transports continue to use the Android
Keystore-backed configuration rather than discovery records.

## Current scope

External-web and GameBox JSON discovery sources are implemented. WebDAV and S3 remain
supported as trusted authorized-catalog transports, but they are not separate discovery
feed adapters in this source layer.

Automated parser/sync tests cover schema validation, unsafe metadata URLs, favorite
preservation, provider-ID namespacing and console scoping. Real third-party endpoint
behavior remains a separate live-service validation gate.
