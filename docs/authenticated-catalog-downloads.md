# Authenticated catalog game downloads

Audit follow-up: DATA-17, DATA-18 (provider/download integration); SYS-15 (credential isolation).

The production remote-download worker now reads the current configured catalog transport and Keystore credentials when it starts. HTTPS/WebDAV game requests use Basic authentication; S3 requests use the existing AWS Signature V4 signer and configured region. Every range request is signed afresh. No password, secret key or Authorization header is persisted in WorkManager input data.

## Configuration and scope

- HTTPS: put authorized game binaries beside or below the directory containing the configured catalog URL.
- WebDAV: put them inside the configured base directory.
- S3-compatible storage: use absolute HTTPS object URLs in the configured endpoint/bucket/prefix.
- Manifest source URLs and SHA-256 checksums are still required; metadata discovery never supplies game binaries.
- Authorization is restricted to matching HTTPS host, effective port and directory boundary. Traversal and ambiguously encoded paths receive no credentials.
- Other hosts/directories continue as anonymous public downloads. For another private location, configure its own catalog rather than sharing credentials across hosts.
- S3 pre-signed URLs keep their own authorization and do not receive an additional signature.
- Redirects remain disabled. Use the final URL.
- A queued/retried download reads current settings; changing provider settings may require restoring the original provider to resume a private download. Removed credentials are not retained in queued work.

## Evidence and limits

Unit tests exercise Basic headers on initial/resumed transfer requests, S3 signing per request, host/port/directory/bucket/prefix isolation, traversal rejection, pre-signed URLs, missing credentials and redirect rejection through an injected HTTP connection boundary.

This does not prove a live WebDAV/S3 account, process-killed download, gameplay, or real hardware acceptance. Those gates remain pending. No external binaries are downloaded on the work PC, and no release is published.
