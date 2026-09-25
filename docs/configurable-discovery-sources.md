# Configurable game discovery sources

GameBox supports two user-configured discovery source types in addition to the built-in
TheGamesDB metadata flow.

## External website

An external website source opens a browser page. The optional search template can use:

- `{query}` — title/search text plus the selected platform;
- `{title}` — title/search text only;
- `{platform}` — selected platform only.

All resolved URLs must remain absolute HTTPS URLs. Credentials are not accepted inside the
URL.

## GameBox JSON

A GameBox JSON source is searched directly from the Store. It is metadata/discovery only
and does not authorize or enqueue downloads.

Example response:

```json
{
  "games": [
    {
      "id": "example-ps2",
      "title": "Example Game",
      "platform": "PS2",
      "region": "USA",
      "year": 2005,
      "detailsUrl": "https://example.test/games/example-ps2",
      "coverUrl": "https://example.test/covers/example-ps2.jpg"
    }
  ],
  "nextPage": null
}
```

The client:

- accepts HTTPS only;
- rejects redirects;
- limits a response to 1 MiB by default;
- limits one response to 500 games;
- requires unique non-empty game IDs;
- validates optional details and cover URLs as HTTPS;
- sends no catalog/download credentials.

The Store can search all enabled JSON sources that support the selected console and merges
their results by source ID plus external game ID. Selecting a result opens its configured
details page when one is supplied.

## Acquisition boundary

Discovery results never become installable merely because they contain metadata or an
external page. Game installation continues to use the existing trusted catalog/download
path with an explicit source URL and expected SHA-256, or the local authorized-copy import
flow.

This keeps user-configured discovery separate from binary acquisition and preserves the
existing verified WorkManager installation pipeline.

## Platform filtering

A source may optionally list supported platforms in Settings, for example:

`PS2, PSP, GameCube`

An empty list means the source is available for every Store platform.
