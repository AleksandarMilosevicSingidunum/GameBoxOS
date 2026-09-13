# Cloud-safe uninstall

When a game has a managed save copy, the uninstall dialog now preflights cloud-save readiness before enabling content removal. A configured path is not treated as proof: the controller creates a fresh verified local backup and performs the real encrypted cloud upload during uninstall.

If cloud configuration, connectivity, credentials, or upload verification fails, removal stops before any content file is deleted. The dialog then presents the sanitized failure and requires the user to explicitly acknowledge continuing with only the verified local backup. This acknowledgement is scoped to the open confirmation and is not persisted.

Games without a managed save do not require cloud protection. Saves, backups, metadata, favorites, and history remain outside the content-removal manifest in all paths. This implements FILE-23's production flow; live cloud-provider validation remains separately tracked.
