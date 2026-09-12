# Remote download network policy

GameBox remote game transfers default to **unmetered connections only**. The setting is
available under Settings → Downloads and can be disabled explicitly when the user wants
to permit metered or mobile-data transfers.

Each new install or resume reads the current setting and writes the corresponding
WorkManager constraint:

- Enabled: `NetworkType.UNMETERED`
- Disabled: `NetworkType.CONNECTED`

Changing the setting does not silently replace an already-running worker; it applies to
the next install or resume. WorkManager keeps queued work durable and starts it when the
selected network constraint is satisfied.

This advances FILE-05. Live metered-network transitions and carrier-specific behavior
still require device acceptance.
