# Console mode and game-source foundation

This branch introduces the first production-facing foundation for using GameBox as the
device's living-room shell.

## Android HOME role

`MainActivity` now advertises a separate Android `HOME` / `DEFAULT` intent filter in
addition to the existing launcher filters. This allows a user to explicitly select
GameBox as the default Home app on a dedicated console phone without removing the normal
launcher entry.

The user must still choose GameBox as the Home app in Android settings. GameBox does not
silently replace the system launcher.

## Discovery source boundary

`DiscoverySource` is intentionally separate from `CatalogProvider`.

- `DiscoverySource`: browse/search/details for a provider-neutral game catalog.
- `CatalogProvider`: trusted install manifests and verified binary-source resolution.
- existing WorkManager download flow: transfer, resume, checksum validation, staging, and
  atomic promotion.

This keeps metadata/discovery independent from acquisition and prevents an arbitrary
website result from becoming an installable binary merely because it exposes a URL.

## Source configuration

`GameSourceConfig` provides a provider-neutral configuration model for future Settings UI
and persistence. It validates:

- stable local source id;
- non-empty display name;
- HTTPS endpoint;
- no embedded credentials;
- no URL fragments;
- optional out-of-band credential key.

Supported foundation types are `GAMEBOX_JSON`, `EXTERNAL_WEB`, `WEBDAV`, and `S3`.

Future work should connect this registry to Settings persistence and the Store source
selector, then adapt existing HTTPS/WebDAV/S3 catalog providers where appropriate.
