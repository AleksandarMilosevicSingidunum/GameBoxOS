# Configured game discovery sources

GameBox can persist optional HTTPS discovery sources independently of the trusted
authorized catalog provider.

Three discovery source types are currently usable:

- **External web** — opens a configured HTTPS site or search URL in the browser.
- **GameBox JSON** — downloads a bounded metadata-only JSON manifest and merges its
  titles into the cached Store discovery database.
- **Vimm's Lair** — performs a bounded title lookup against Vimm vault listing pages
  for a selected console and shows matching titles directly in Store. Selecting a match
  opens its Vimm vault page in the external browser.

None of these source types grants a game binary permission to install.

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

## Vimm's Lair discovery

Choose **Vimm's Lair** in Settings and use the preset to populate:

- name: `Vimm's Lair`;
- base URL: `https://vimm.net/vault`;
- console scope: `PS2, GameCube, Wii, PSP, Dreamcast`.

All fields remain editable before saving, so Vimm integration is an optional configured
source rather than a mandatory built-in Store backend.

Saved Vimm sources also expose a **Test** action in Settings. The probe performs one
bounded request to a supported alphabetical vault listing page and verifies that the
response still contains numeric `/vault/<id>` game links. This distinguishes basic
reachability/HTML-structure failures from a Store query that simply has no matching title.

In Store, enter a game title and choose **Search Vimm's Lair**. If a console is selected,
GameBox requests only that console's corresponding alphabetical vault listing page. If no
console is selected, GameBox searches the configured supported consoles sequentially (up
to eight), merges the results, and reports partial failures without discarding successful
matches. An unrestricted Vimm source defaults to the GameBox target set: PS2, GameCube,
Wii, PSP, and Dreamcast.

Each listing request parses `/vault/<numeric-id>` title links, filters them against the
entered title, and displays the matches as temporary discovery cards. Results are not
written into the authorized catalog and do not create download jobs.

Selecting a Vimm search result now stays inside GameBox first. Before opening the normal
discovery Details flow, GameBox performs one bounded request to the exact numeric vault
page and hydrates non-binary metadata when present: page title, system/platform, region,
release year, and Vimm-hosted Open Graph artwork. If the detail request fails or the page
is sparse, the original listing result remains usable.

GameBox derives a stable, source-namespaced local identity from the configured source id
plus Vimm's numeric vault id. From Details the user can:

- open the exact `https://vimm.net/vault/<id>` page externally;
- import a locally selected copy through the same format, SHA-256, transaction-journal
  and Room registration path used by other discovery titles;
- import a multi-file disc set when that console profile supports one.

The external numeric id is hashed before it becomes a local GameBox game id. The Vimm
result itself does not become a remote install source and does not enqueue a download.

Before presenting a Vimm result, GameBox also checks its existing discovery cache for one
and only one exact normalized title match on the same canonical platform. When that match
is unambiguous, the temporary Vimm result reuses the cached description, rating, release
metadata, cover/background/logo artwork and screenshots while retaining its Vimm-derived
local identity and exact external vault link. Ambiguous same-title matches are deliberately
left unenriched rather than guessed.

For already cached Store titles, the configured Vimm source also appears on the details
screen and opens the appropriate platform/title browse page.

The adapter follows the category/game-link structure demonstrated by the
`heywander/vimms-lair-scrape` project, but intentionally does not reproduce its
`mediaId` extraction or binary download functions. Detail hydration ignores download
form fields and accepts artwork only from HTTPS `vimm.net` URLs. GameBox sends a normal
`GameBoxOS/0.1` user agent, does not rotate proxies, does not bypass access controls,
does not follow redirects, and bounds each HTML response to 2 MiB.

Current GameBox mappings include PS2, GameCube, Wii, PSP and Dreamcast, plus several
retro platform aliases in the adapter. 3DS and Switch are intentionally unmapped because
the adapter has no verified Vimm vault mapping for those GameBox console labels.

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

External-web, GameBox JSON and Vimm's Lair discovery sources are implemented. WebDAV and
S3 remain supported as trusted authorized-catalog transports, but they are not separate
discovery feed adapters in this source layer.

Automated tests cover JSON schema validation, unsafe metadata URLs, favorite
preservation, provider-ID namespacing, console scoping, Vimm URL ownership, platform
mapping, alphabetical browse resolution and bounded HTML title parsing. Real third-party
endpoint behavior remains a separate live-service validation gate.
