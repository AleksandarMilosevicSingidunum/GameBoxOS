#!/usr/bin/env python3
"""Create a schema-compatible GameBox Android release provenance manifest."""

from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path

TAG = re.compile(r"^v[0-9]+\.[0-9]+\.[0-9]+(?:-[A-Za-z0-9.-]+)?$")
STABLE_TAG = re.compile(r"^v[0-9]+\.[0-9]+\.[0-9]+$")
CHANNELS = {"ALPHA", "BETA", "PRODUCTION"}


def create_manifest(
    artifact_path: str | Path,
    release_tag: str,
    channel: str,
    rollback_tag: str | None = None,
) -> dict[str, object]:
    artifact = Path(artifact_path)
    if not artifact.is_file():
        raise ValueError(f"artifact does not exist: {artifact}")
    if not TAG.fullmatch(release_tag):
        raise ValueError("release tag must be semantic vMAJOR.MINOR.PATCH with an optional prerelease")
    channel = channel.upper()
    if channel not in CHANNELS:
        raise ValueError(f"unsupported release channel: {channel}")
    if channel == "PRODUCTION" and not STABLE_TAG.fullmatch(release_tag):
        raise ValueError("production releases require a stable semantic tag")
    if channel == "PRODUCTION" and not rollback_tag:
        raise ValueError("production releases require a rollback tag")
    if rollback_tag is not None:
        if not TAG.fullmatch(rollback_tag) or rollback_tag == release_tag:
            raise ValueError("rollback tag must be a distinct valid release tag")
    digest = hashlib.sha256()
    size = 0
    with artifact.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
            size += len(chunk)
    return {
        "schemaVersion": 1,
        "artifactName": artifact.name,
        "releaseTag": release_tag,
        "channel": channel,
        "sha256": digest.hexdigest(),
        "sizeBytes": size,
        **({"rollbackReleaseTag": rollback_tag} if rollback_tag else {}),
    }


def write_manifest(
    artifact_path: str | Path,
    release_tag: str,
    channel: str,
    output_path: str | Path,
    rollback_tag: str | None = None,
) -> None:
    manifest = create_manifest(artifact_path, release_tag, channel, rollback_tag)
    Path(output_path).write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")


def main(argv: list[str]) -> int:
    if len(argv) not in (4, 5):
        print(
            "usage: create-apk-manifest.py ARTIFACT RELEASE_TAG CHANNEL OUTPUT [ROLLBACK_TAG]",
            file=sys.stderr,
        )
        return 2
    try:
        write_manifest(
            artifact_path=argv[0],
            release_tag=argv[1],
            channel=argv[2],
            output_path=argv[3],
            rollback_tag=argv[4] if len(argv) == 5 else None,
        )
    except ValueError as error:
        print(str(error), file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
