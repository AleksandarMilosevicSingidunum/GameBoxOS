# Production release and rollback runbook

## Publish

1. Create and review a stable tag (`vMAJOR.MINOR.PATCH`) on the intended commit.
2. Ensure the protected GitHub Actions secrets are present: `GAMEBOX_KEYSTORE_BASE64`, `GAMEBOX_KEYSTORE_PASSWORD`, `GAMEBOX_KEY_ALIAS`, and `GAMEBOX_KEY_PASSWORD`.
3. Run **Publish signed production APK** with the new tag and a distinct previous known-good rollback tag.
4. Verify the uploaded APK checksum and signature before distribution.

The workflow refuses prerelease tags, missing signing inputs, missing tags, or a rollback tag equal to the release tag.

## Rollback

1. Stop distribution of the current release.
2. Re-publish the previously verified rollback tag through the same workflow, using the release being withdrawn as the rollback reference.
3. Verify the rollback APK signature and SHA-256 checksum.
4. Record the incident and affected versions in the release notes.

The workflow never overwrites source tags and does not silently fall back to an unsigned build.
