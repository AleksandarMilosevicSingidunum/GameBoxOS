import pathlib
import sys
import unittest

import importlib.util

module_path = pathlib.Path(__file__).with_name("android-version-code.py")
spec = importlib.util.spec_from_file_location("android_version_code", module_path)
module = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(module)
android_version_code = module.android_version_code


class AndroidVersionCodeTest(unittest.TestCase):
    def test_release_channels_are_monotonic(self):
        versions = [
            "v0.1.0-alpha.1",
            "v0.1.0-alpha.2",
            "v0.1.0-beta.1",
            "v0.1.0",
            "v0.1.1-alpha.1",
            "v0.1.1",
            "v0.2.0-alpha.1",
            "v1.0.0",
        ]
        codes = [android_version_code(version) for version in versions]
        self.assertEqual(codes, sorted(codes))
        self.assertEqual(len(codes), len(set(codes)))

    def test_stable_and_prerelease_values_are_exact(self):
        self.assertEqual(1, android_version_code("v0.0.0-alpha.1"))
        self.assertEqual(500, android_version_code("v0.0.0-beta.1"))
        self.assertEqual(999, android_version_code("v0.0.0"))
        self.assertEqual(101_000_999, android_version_code("v1.1.0"))

    def test_invalid_or_collision_prone_ranges_are_rejected(self):
        for value in ("1.0.0", "v0.1.0-rc.1", "v0.1.0-alpha.0",
                      "v0.1.0-beta.500", "v21.0.0", "v1.100.0", "v1.1.1000"):
            with self.subTest(value=value):
                with self.assertRaises(ValueError):
                    android_version_code(value)


if __name__ == "__main__":
    unittest.main()
