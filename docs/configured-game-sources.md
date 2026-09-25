# Configured game discovery sources

GameBox can persist optional HTTPS discovery sources independently of the trusted
authorized catalog provider.

## Settings

Open **Settings → Game discovery sources** and provide:

- a display name;
- an HTTPS base URL;
- an optional search URL template;
- optional comma-separated console names.

Supported template placeholders are:

- `{query}` — the current Store text plus the selected console;
- `{title}` — the current Store text or the selected discovery title;
- `{platform}` — the currently selected console/platform.

Configured sources can be enabled, disabled, or removed without changing the
authorized catalog, installed files, metadata cache, or save records.

## Store behavior

Enabled sources appear directly in Store when they apply to the selected console.
Opening one launches the resolved HTTPS URL in the user's browser. On a game details
screen the same source is resolved against that game's title and platform.

If no Store query or console is selected, GameBox opens the configured base URL
instead of manufacturing an empty search request.

## Security and install boundary

Configured external-web sources are discovery links only. A web result is never
converted into an install job automatically. Remote installation still requires the
existing trusted catalog path to supply an HTTPS source plus an expected SHA-256
checksum, followed by the existing WorkManager staging and verification pipeline.

GameBox rejects non-HTTPS source URLs, embedded URL credentials, malformed hosts and
URL fragments. Credentials for trusted catalog transports continue to use the
existing Android Keystore-backed configuration rather than these discovery records.

## Current scope

This increment wires external-web sources into compact and TV/DeX Store layouts and
Discovery Details. `GAMEBOX_JSON`, WebDAV and S3 source types remain architectural
placeholders until their discovery adapters are implemented and validated; the
Settings UI currently creates external-web sources only.
