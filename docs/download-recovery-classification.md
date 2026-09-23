# Download retry and partial-content recovery

Blueprint follow-up: FILE-13 (resume), SYS-14 (recovery), DATA-17/DATA-18 (authenticated provider downloads).

The remote worker now consumes typed retry eligibility from the transfer engine. Invalid/expired credentials, missing content, rejected redirects and invalid configuration fail immediately with an actionable provider message. Network I/O, temporary server errors and rate limits retain the existing bounded WorkManager exponential backoff (at most three retries). Manual retry remains available after configuration repair.

A stream ending before its advertised full size is an interrupted download, not a checksum mismatch: partial bytes are retained and the next attempt requests the remaining range. Content still must pass SHA-256 before installation. Range responses are checked for start/end/total and body-length consistency; rejected resume ranges (including HTTP 416) restart once from byte zero.

Tests cover retry classification/bounds, short-body preservation followed by successful resume/checksum/commit, HTTP 416 restart, malformed ranges and cancellation propagation. These are deterministic production-engine tests using an HTTP connection boundary, not live-provider/process-kill or hardware acceptance.
