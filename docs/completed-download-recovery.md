# Local recovery of completed remote downloads

Audit follow-up: FILE-12 (retry installation without redownloading verified content), FILE-14 (restart reconciliation).

The production remote worker now performs local completion recovery before its download-space preflight or catalog credential lookup:

1. Check an existing final file against the expected SHA-256 and maximum permitted size.
2. Otherwise verify an existing staging file; if it is complete, atomically promote it.
3. Return normal worker success so existing repository reconciliation can finish the install.
4. Incomplete or mismatched files are not accepted. Incomplete staging data remains available for ordinary range resume.
5. Promotion/read failure reports a storage recovery error and keeps the staged payload for a local retry.

Verification checks cancellation throughout hashing and before promotion. A file's existence or length alone never counts as success. This does not bypass the worker's configured WorkManager network scheduling constraints: local recovery occurs once the job starts, even though it makes no content request.

Unit regressions cover complete staging recovery, already-committed recovery, corrupted/incomplete content, replacing an old final file, a real failed filesystem promotion followed by successful retry, size bounds and cancellation. Physical power-loss and real process-kill/WorkManager lifecycle acceptance remain pending. The alpha.4 release is unchanged by this follow-up.
