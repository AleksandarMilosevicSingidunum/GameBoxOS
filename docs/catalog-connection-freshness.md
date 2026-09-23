# Catalog connection freshness

DATA-15/DATA-29 follow-up: HTTPS Save & test requires a newly fetched and parsed
response. A previously cached catalog cannot make invalid credentials, server
failure or malformed data appear healthy.

Normal catalog load still permits same-endpoint cached fallback. Explicit refresh
is forwarded through selecting, configured and metadata decorators to the remote
refresh operation; configured fallback is reported as REMOTE_FAILURE. Metadata
enrichment remains optional. Coroutine cancellation is propagated rather than
converted into a successful fallback or connection-error result.

The Android regression seeds the real disk cache, simulates 401 and malformed
responses through the fetch boundary, verifies browsing recovery and failed
connection tests, checks endpoint isolation, then verifies successful recovery.
JVM coverage verifies decorator forwarding and cancellation. This is automated
cache/production-path evidence, not a claim of live WebDAV/S3 endpoint validation.
