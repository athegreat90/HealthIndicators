"""Synthetic ZIP packaging fixtures only: these are NOT compiled/game artifacts.

These tests validate the verifier's rejection/acceptance behavior. They provide
no evidence of Minecraft loading, valid JVM bytecode or graphics compatibility.
Run: python3 -m unittest discover -s scripts/tests -v
"""

import contextlib
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
import zipfile


SPEC = importlib.util.spec_from_file_location("verify_artifact", Path(__file__).resolve().parents[1] / "verify_artifact.py")
verifier = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(verifier)


def metadata(minecraft="26.2", version="test.1"):
    # Explicit fixture values, not lookups from the verifier's target constants.
    neoforge, next_minor, architectury, yacl = {
        "26.2": ("26.2.0.93", "26.3", "21.1.12", "3.9.7+26.2-neoforge"),
        "26.3": ("26.3.0.97", "26.4", "22.0.5", "3.9.7+26.3-neoforge"),
    }[minecraft]
    text = f'''modLoader = "javafml"
loaderVersion = "[2,)"
license = "LGPL-3.0-only"
[[mods]]
modId = "healthindicators"
version = "{version}"
[[mixins]]
config = "healthindicators.mixins.json"
'''
    for mod_id, version_range in (
        ("minecraft", f"[{minecraft},{next_minor})"),
        ("neoforge", f"[{neoforge},{next_minor})"),
        ("architectury", f"[{architectury},)"),
        ("yet_another_config_lib_v3", f"[{yacl},)"),
    ):
        text += f'''[[dependencies.healthindicators]]
modId = "{mod_id}"
type = "required"
versionRange = "{version_range}"
side = "CLIENT"
'''
    return text.encode()


def fixture_entries(minecraft="26.2", version="test.1"):
    entries = {name: b"synthetic packaging fixture; not JVM bytecode or a game asset"
               for name in verifier.REQUIRED_ENTRIES}
    entries["META-INF/neoforge.mods.toml"] = metadata(minecraft, version)
    entries["healthindicators.mixins.json"] = json.dumps({
        "required": True,
        "package": "io.github.adytech99.healthindicators.mixin",
        "client": ["EntityRendererMixin", "EntityDamageMixin"],
        "injectors": {"defaultRequire": 1},
    }).encode()
    entries["healthindicators.accesswidener"] = b"accessWidener v2 official\n"
    for language in ("en_us", "ru_ru", "zh_cn"):
        entries[f"assets/healthindicators/lang/{language}.json"] = b'{"fixture": "packaging only"}'
    return entries


class VerifyArtifactTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.directory = Path(self.temporary.name)
        self.jar = self.directory / "HealthIndicators-NeoForge-26.2-test.1.jar"

    def tearDown(self):
        self.temporary.cleanup()

    def write_fixture(self, entries=None, jar=None):
        jar = jar or self.jar
        with zipfile.ZipFile(jar, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            for name, content in (fixture_entries() if entries is None else entries).items():
                archive.writestr(name, content)
        return jar

    def assert_rejected(self, jar=None, minecraft="26.2"):
        jar = jar or self.jar
        with self.assertRaises(verifier.ArtifactError):
            verifier.verify_artifact(jar, minecraft)
        self.assertFalse(jar.with_name(jar.name + ".sha256").exists())

    def test_good_fixtures_for_both_targets_generate_correct_checksum(self):
        for minecraft in ("26.2", "26.3"):
            with self.subTest(minecraft=minecraft):
                jar = self.directory / f"HealthIndicators-NeoForge-{minecraft}-test.1.jar"
                self.write_fixture(fixture_entries(minecraft), jar)
                checksum, digest = verifier.verify_artifact(jar, minecraft)
                self.assertEqual(digest, hashlib.sha256(jar.read_bytes()).hexdigest())
                self.assertEqual(checksum.read_text(), f"{digest}  {jar.name}\n")

    def test_missing_renderer_metadata_texture_license_and_widener_are_rejected(self):
        for missing in (
            verifier.PACKAGE + "mixin/EntityRendererMixin.class",
            verifier.PACKAGE + "neoforge/HealthIndicatorsNeoForge.class",
            "META-INF/neoforge.mods.toml", "LICENSE", "healthindicators.accesswidener",
            "assets/healthindicators/textures/gui/heart/half.png",
        ):
            with self.subTest(missing=missing):
                entries = fixture_entries()
                del entries[missing]
                self.write_fixture(entries)
                self.assert_rejected()

    def test_invalid_zip_is_rejected(self):
        self.jar.write_bytes(b"this is not a ZIP")
        self.assert_rejected()

    def test_malformed_and_unexpanded_metadata_are_rejected(self):
        for text in (b"invalid = [", metadata().replace(b'test.1', b'${version}')):
            with self.subTest(text=text):
                entries = fixture_entries()
                entries["META-INF/neoforge.mods.toml"] = text
                self.write_fixture(entries)
                self.assert_rejected()

    def test_wrong_target_in_filename_is_rejected(self):
        self.write_fixture()
        self.assert_rejected(minecraft="26.3")

    def test_wrong_target_in_metadata_is_rejected(self):
        self.write_fixture(fixture_entries("26.3"))
        self.assert_rejected()

    def test_dev_sources_and_raw_names_are_rejected(self):
        for classifier in ("dev", "dev-shadow", "sources", "raw"):
            with self.subTest(classifier=classifier):
                version = "test.1-" + classifier
                jar = self.directory / f"HealthIndicators-NeoForge-26.2-{version}.jar"
                self.write_fixture(fixture_entries(version=version), jar)
                self.assert_rejected(jar)

    def test_fabric_and_kotlinforforge_runtime_content_is_rejected(self):
        for forbidden in ("fabric.mod.json", "net/fabricmc/loader/Fixture.class",
                          "net/fabricmc/fabric/api/Fixture.class", "net/fabricmc/api/Fixture.class",
                          verifier.PACKAGE + "fabric/HealthIndicatorsFabric.class",
                          "thedarkcolour/kotlinforforge/Fixture.class"):
            with self.subTest(forbidden=forbidden):
                entries = fixture_entries()
                entries[forbidden] = b"synthetic prohibited runtime fixture"
                self.write_fixture(entries)
                self.assert_rejected()

    def test_dependency_pins_sides_and_required_status_are_enforced(self):
        original = metadata()
        for wrong in (
            original.replace(b"26.2.0.93", b"26.2.0.92"),
            original.replace(b"[26.2,26.3)", b"[26.2,)"),
            original.replace(b"21.1.12", b"22.0.5"),
            original.replace(b"3.9.7+26.2-neoforge", b"3.9.7+26.3-neoforge"),
            original.replace(b'"CLIENT"', b'"BOTH"'),
            original.replace(b'"required"', b'"optional"'),
        ):
            with self.subTest(metadata=wrong):
                entries = fixture_entries()
                entries["META-INF/neoforge.mods.toml"] = wrong
                self.write_fixture(entries)
                self.assert_rejected()

    def test_fabric_runtime_dependency_is_rejected(self):
        entries = fixture_entries()
        entries["META-INF/neoforge.mods.toml"] += b'''[[dependencies.healthindicators]]
modId = "fabricloader"
type = "required"
side = "CLIENT"
versionRange = "[0.19,)"
'''
        self.write_fixture(entries)
        self.assert_rejected()

    def test_duplicate_zip_entries_are_rejected(self):
        self.write_fixture()
        with zipfile.ZipFile(self.jar, "a") as archive:
            import warnings
            with warnings.catch_warnings():
                warnings.simplefilter("ignore", UserWarning)
                archive.writestr("LICENSE", b"duplicate fixture")
        self.assert_rejected()

    def test_disabled_mixins_are_rejected(self):
        entries = fixture_entries()
        mixins = json.loads(entries["healthindicators.mixins.json"])
        mixins["required"] = False
        entries["healthindicators.mixins.json"] = json.dumps(mixins).encode()
        self.write_fixture(entries)
        self.assert_rejected()

    def test_cli_returns_failure_without_checksum(self):
        self.jar.write_bytes(b"broken fixture")
        with contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(verifier.main(["--minecraft", "26.2", str(self.jar)]), 1)
        self.assertFalse(self.jar.with_name(self.jar.name + ".sha256").exists())


if __name__ == "__main__":
    unittest.main()
