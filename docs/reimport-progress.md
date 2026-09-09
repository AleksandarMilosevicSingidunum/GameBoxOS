# Retained import recovery

Removed imported games remain reachable in Library. Their details offer a system
file picker to select an owned game file or a descriptor plus all disc tracks.
The existing verified importer registers the replacement under the same game ID,
preserving history, preferences and separate save files. Launch/session recovery
states disable the picker. File copying and registration run off the UI thread.

Evidence: two Library membership unit tests pass locally. The preceding PR 279
revision passed all five checks, including its Room/importer content-removal and
reimport integration tests and confirmation-dialog tests. This UI follow-up needs
its own Android build and regression run. No real-emulator save recovery is claimed.

Still pending: process-death recovery during import, shared per-game mutation
locking, end-to-end system-picker automation, and real emulator save validation.
The picker cannot prove that a selected file is the same title: users must select
the matching owned copy. No game files are supplied or downloaded by this flow.

