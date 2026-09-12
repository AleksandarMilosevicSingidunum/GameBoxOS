#!/usr/bin/env python3
"""Convert a GameBox semantic release tag to a monotonic Android versionCode."""

from __future__ import annotations

import re
import sys

TAG = re.compile(
    r"^v(?P<major>\d+)\.(?P<minor>\d+)\.(?P<patch>\d+)"
    r"(?:(?:-(?P<channel>alpha|beta))(?:\.(?P<ordinal>\d+))?)?$"
)
MAX_ANDROID_VERSION_CODE = 2_100_000_000


def android_version_code(tag: str) -> int:
    match = TAG.fullmatch(tag)
    if not match:
        raise ValueError("tag must be vMAJOR.MINOR.PATCH, optionally -alpha.N or -beta.N")
    major, minor, patch = (int(match[name]) for name in ("major", "minor", "patch"))
    if major > 20 or minor > 99 or patch > 999:
        raise ValueError("version exceeds supported Android ordering ranges")
    channel = match["channel"]
    ordinal = int(match["ordinal"] or "1")
    if channel is None:
        if match["ordinal"] is not None:
            raise ValueError("stable versions cannot have a prerelease ordinal")
        stage = 999
    elif channel == "alpha":
        if ordinal not in range(1, 500):
            raise ValueError("alpha ordinal must be between 1 and 499")
        stage = ordinal
    else:
        if ordinal not in range(1, 500):
            raise ValueError("beta ordinal must be between 1 and 499")
        stage = 499 + ordinal
    code = major * 100_000_000 + minor * 1_000_000 + patch * 1_000 + stage
    if code <= 0 or code > MAX_ANDROID_VERSION_CODE:
        raise ValueError("calculated versionCode is outside Android's supported range")
    return code


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print("usage: android-version-code.py vMAJOR.MINOR.PATCH[-alpha.N|-beta.N]", file=sys.stderr)
        return 2
    try:
        print(android_version_code(argv[1]))
    except ValueError as error:
        print(str(error), file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
