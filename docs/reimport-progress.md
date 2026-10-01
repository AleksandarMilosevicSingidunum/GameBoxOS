# Retained import recovery

Removed imported games remain reachable in Library. Their details offer a system
file picker to select an owned game file or a descriptor plus all disc tracks.
The existing verified importer registers the replacement under the same game ID,
preserving history, preferences and separate save files. Launch/session recovery
states disable the picker. File copying and registration run off the UI thread.

Evidence: two Library membership unit tests pass locally. The preceding PR 279
revision passed all five checks, including its Room/importer content-removal and
reimport integration tests and confirmation-dialog tests. The latest connected Honor
instrumentation run includes this production UI path. No real-emulator save recovery
is claimed.

Shared per-game mutation locking is now implemented for production import/reimport,
missing-file Forget, and content-only uninstall flows. The lock spans file mutation plus
the matching library-state publication so those operations cannot interleave for the same
game; unrelated games remain independent.

Interrupted importer filesystem transactions now recover on the next importer startup or
mutation: abandoned staging directories are removed, surviving backup directories roll
the target back to its pre-import contents, and single-file partials are cleaned. New
imports fail closed if that recovery cannot complete.

The file-to-Library registration boundary is now journaled durably. A marker is written
before the import directory swap, the previous directory is retained until Room
registration is confirmed, and startup reconciliation compares the journaled SHA-256 file
set with the durable Room row. A committed registration finalizes the new files; an
uncommitted registration rolls back to the pre-import directory (or removes a brand-new
unregistered import). A registration exception also attempts that rollback immediately.

Android instrumentation now drives the production reimport card through the real
DocumentsUI picker on the API 35 Google APIs emulator. The test publishes a synthetic
owned ISO into Downloads, selects it through the system picker, verifies the retained
SHA-256 identity, completes the production importer + Room registration path, and checks
that the transaction journal/staging/backup state is fully cleaned.

Still pending: physical Honor Magic5 Pro picker behavior and real emulator save validation.
Restore now requires the retained filenames and SHA-256 checksums for the full
file set. Identity is checked in staging before replacing content; a different
edition must be imported separately. Three local identity regression tests pass.
Revision 583c472 passed all five GitHub checks, including Android instrumentation,
unit/build checks, Windows build, and phone/DeX screenshots. The added
ReimportIdentityIntegrationTest exercises the production importer against actual
temporary files: mismatches leave existing content and synthetic save bytes intact;
matching content restores a removed file. Its execution is now covered by the
connected-device instrumentation run. This does not validate a real emulator or its
save format.
No game files are supplied or downloaded by this flow.

Current API-35 recheck (2026-10-01): `SystemDocumentPickerReimportTest` no
longer completes on the current Google APIs emulator. DocumentsUI opens,
shows the synthetic Downloads document, and returns to the test host after the
selection, but the repository never reaches `INSTALLED` within 20 seconds and
the test fails at its post-picker verification. Device logs show no GameBox
import exception or crash; the result callback/selection contract requires a
focused follow-up before this flow can be considered green again. The earlier
pass recorded above is historical and is not reused as current evidence.

