# Production release checklist

- [ ] Configure production signing keys in protected CI secrets
- [ ] Verify release APK checksum, size, and provenance manifest
- [ ] Confirm the manifest tag and channel match the published release
- [ ] Publish to selected update channel
- [ ] Validate rollback to previous known-good version recorded in the manifest
- [ ] Test migration from latest alpha/beta
- [ ] Confirm diagnostics export excludes credentials/user content
- [ ] Record release notes and recovery procedure
