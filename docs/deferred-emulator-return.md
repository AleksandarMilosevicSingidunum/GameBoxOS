# Deferred emulator return

A pause/resume arriving while the handoff or durable session confirmation is still preparing is now retained and replayed once preparation finishes. A resume without a post-dispatch pause is not treated as a deferred return; leaving again clears the earlier return. MainActivity forwards pause events. Cold-start journal recovery remains unchanged.

Validation: five isolated lifecycle-latch tests passed locally. A controller regression blocks confirmation, sends pause/resume, and requires recovery without another resume; full project execution pending CI. This is lifecycle accounting, not proof of gameplay or emulator save recovery.
