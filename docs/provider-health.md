# Provider health and recovery

Blueprint requirements DATA-28 and DATA-29 distinguish provider availability from cached
catalog availability. GameBox now records a durable TheGamesDB health snapshot after every
sync attempt:

- configuration/authentication state;
- last attempt and last successful contact;
- measured request latency;
- rate-limit retry deadline when the provider supplies `Retry-After`;
- bounded, user-safe recovery text.

HTTP 401/403 responses require credential repair. HTTP 429 responses retain their retry
deadline. Transient network and server failures remain retryable and do not erase the last
successful contact. Unsupported or malformed responses are reported as degraded rather
than pretending the provider is offline.

Both compact and living-room Store layouts expose the health summary. Cached metadata
remains browsable when the provider is unavailable. The API key stays in Android Keystore;
health persistence contains no credentials, request URLs, headers, or response bodies.

Automated tests cover successful latency/last-success publication, missing credentials,
authentication rejection, rate limiting, and truthful summary text. These tests establish
classification and production wiring, not live TheGamesDB account acceptance. A real
authenticated sync and provider-controlled rate-limit response remain release evidence.
