# Production release checklist

- [ ] Configure production signing keys in protected CI secrets
- [ ] Verify release APK checksum and provenance
- [ ] Publish to the selected update channel
- [ ] Validate rollback to the previous known-good version
- [ ] Test migration from the latest alpha/beta build
- [ ] Confirm diagnostics export excludes credentials and user content
- [ ] Record release notes and recovery procedure