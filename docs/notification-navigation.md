# Download notification navigation

Remote download notifications now open the relevant production screen:

- Active progress opens **Downloads**.
- Successful completion opens **Library** with that game selected.
- Cold-start and already-running activity delivery use the same validated navigation request.
- Destination values and game IDs are allow-listed before they reach Compose.
- Notification intents are immutable and reuse only the matching game's notification identity.

This is automated implementation evidence for DATA-04. Android notification permission,
background-delivery and tap behavior still require representative device acceptance; CI does
not replace that runtime validation.
