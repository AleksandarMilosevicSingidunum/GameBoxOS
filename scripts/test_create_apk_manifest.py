import hashlib
import importlib.util
import pathlib
import tempfile
import unittest


module_path = pathlib.Path(__file__).with_name("create-apk-manifest.py")
spec = importlib.util.spec_from_file_location("create_apk_manifest", module_path)
module = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(module)
create_manifest = module.create_manifest


class CreateApkManifestTest(unittest.TestCase):
    def test_manifest_contains_verified_artifact_provenance(self):
        with tempfile.TemporaryDirectory() as directory:
            artifact = pathlib.Path(directory) / "GameBoxOS-v1.0.0.apk"
            payload = b"signed release fixture"
            artifact.write_bytes(payload)

            manifest = create_manifest(
                artifact,
                "v1.0.0",
                "production",
                "v0.9.0",
                "ABCDEF1234567",
                "production-keystore",
            )

        self.assertEqual("GameBoxOS-v1.0.0.apk", manifest["artifactName"])
        self.assertEqual("PRODUCTION", manifest["channel"])
        self.assertEqual(hashlib.sha256(payload).hexdigest(), manifest["sha256"])
        self.assertEqual(len(payload), manifest["sizeBytes"])
        self.assertEqual("v0.9.0", manifest["rollbackReleaseTag"])
        self.assertEqual("abcdef1234567", manifest["sourceCommit"])
        self.assertEqual("production-keystore", manifest["signing"])

    def test_preview_manifest_accepts_source_and_signing_metadata(self):
        with tempfile.TemporaryDirectory() as directory:
            artifact = pathlib.Path(directory) / "app.apk"
            artifact.write_bytes(b"preview")
            manifest = create_manifest(
                artifact,
                "v1.0.0-alpha.1",
                "preview",
                source_commit="0123456789abcdef",
                signing="Android debug key",
            )

        self.assertEqual("PREVIEW", manifest["channel"])
        self.assertEqual("0123456789abcdef", manifest["sourceCommit"])
        self.assertEqual("Android debug key", manifest["signing"])

    def test_rejects_malformed_provenance_metadata(self):
        with tempfile.TemporaryDirectory() as directory:
            artifact = pathlib.Path(directory) / "app.apk"
            artifact.write_bytes(b"fixture")

            with self.assertRaises(ValueError):
                create_manifest(artifact, "v1.0.0-alpha.1", "PREVIEW", source_commit="not-a-sha")
            with self.assertRaises(ValueError):
                create_manifest(artifact, "v1.0.0-alpha.1", "PREVIEW", signing=" ")

    def test_production_requires_stable_tag_and_distinct_rollback(self):
        with tempfile.TemporaryDirectory() as directory:
            artifact = pathlib.Path(directory) / "app.apk"
            artifact.write_bytes(b"fixture")
            for tag, rollback in (("v1.0.0-beta.1", "v0.9.0"), ("v1.0.0", None), ("v1.0.0", "v1.0.0")):
                with self.subTest(tag=tag, rollback=rollback):
                    with self.assertRaises(ValueError):
                        create_manifest(artifact, tag, "PRODUCTION", rollback)

    def test_rejects_unknown_channel_and_missing_artifact(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(ValueError):
                create_manifest(pathlib.Path(directory) / "missing.apk", "v1.0.0", "PRODUCTION")
            artifact = pathlib.Path(directory) / "app.apk"
            artifact.write_bytes(b"fixture")
            with self.assertRaises(ValueError):
                create_manifest(artifact, "v1.0.0", "NIGHTLY")


if __name__ == "__main__":
    unittest.main()
