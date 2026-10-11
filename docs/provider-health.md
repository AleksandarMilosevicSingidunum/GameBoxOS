# Retired provider health integration

The historical TheGamesDB-specific API-key sync and health snapshot were removed when
GameBox moved metadata enrichment and manual match candidates to keyless Wikidata and
Libretro sources. The old health model is no longer part of the application runtime.

Generic HTTPS catalog and cloud-save transports continue to use the shared provider recovery
policy. Historical cached discovery rows and Room IDs are retained for upgrade compatibility;
they are not refreshed by TheGamesDB.
