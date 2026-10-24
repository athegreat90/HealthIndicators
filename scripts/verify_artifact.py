#!/usr/bin/env python3
"""Verify production JAR packaging, not Minecraft startup or GPU behavior.

Requires Python 3.11+ (stdlib only). Writes <artifact>.sha256 only after all
packaging checks succeed. A successful check does not validate Mixins at runtime.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import sys
import tomllib
import zipfile


TARGETS = {
    "26.2": {"next": "26.3", "neoforge": "26.2.0.93", "architectury": "21.1.12",
             "yacl": "3.9.7+26.2-neoforge"},
    "26.3": {"next": "26.4", "neoforge": "26.3.0.97", "architectury": "22.0.5",
             "yacl": "3.9.7+26.3-neoforge"},
}
PACKAGE = "io/github/adytech99/healthindicators/"
ASSETS = "assets/healthindicators/"
REQUIRED_CLASSES = (
    "neoforge/HealthIndicatorsNeoForge", "neoforge/commands/ModCommands",
    "HealthIndicatorsCommon", "RenderTracker", "HudRenderer",
    "DamageDirectionIndicatorRenderer", "mixin/EntityRendererMixin",
    "mixin/EntityDamageMixin", "config/Config", "config/ModConfig",
    "config/EntitiesListGroup", "util/RenderUtils", "util/HeartJumpData",
    "enums/HeartTypeEnum", "enums/ArmorTypeEnum", "enums/HealthDisplayTypeEnum",
)
HEART_TEXTURES = (
    "container", "container_blinking", "container_hardcore", "container_hardcore_blinking",
    "absorbing_full", "absorbing_half", "absorbing_full_blinking", "absorbing_half_blinking",
    "absorbing_hardcore_full", "absorbing_hardcore_half",
    "absorbing_hardcore_full_blinking", "absorbing_hardcore_half_blinking",
) + tuple(
    f"{status}{hardcore}{half}{blink}"
    for status in ("", "poisoned_", "withered_", "frozen_")
    for hardcore in ("", "hardcore_")
    for half in ("full", "half")
    for blink in ("", "_blinking")
)
REQUIRED_ENTRIES = frozenset(
    [PACKAGE + name + ".class" for name in REQUIRED_CLASSES]
    + ["META-INF/neoforge.mods.toml", "healthindicators.mixins.json",
       "healthindicators.accesswidener", "LICENSE", ASSETS + "icon.png",
       ASSETS + "textures/gui/icons.png"]
    + [ASSETS + f"lang/{language}.json" for language in ("en_us", "ru_ru", "zh_cn")]
    + [ASSETS + f"textures/gui/heart/{name}.png" for name in HEART_TEXTURES]
    + [ASSETS + f"textures/gui/armor/armor_{name}.png" for name in ("empty", "full", "half")]
)


class ArtifactError(ValueError):
    """The supplied file does not meet the production packaging contract."""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ArtifactError(message)


def verify_artifact(jar: Path, minecraft: str) -> tuple[Path, str]:
    """Validate one explicitly selected target JAR and return checksum path/digest."""
    require(minecraft in TARGETS, f"Unsupported Minecraft target: {minecraft}")
    match = re.fullmatch(rf"HealthIndicators-NeoForge-{re.escape(minecraft)}-([A-Za-z0-9][A-Za-z0-9.+_-]*)\.jar", jar.name)
    require(match is not None, f"Expected HealthIndicators-NeoForge-{minecraft}-<version>.jar")
    version = match.group(1)
    require(re.search(r"(?:^|[-._+])(sources?|raw|dev)(?:[-._+]|$)", version, re.IGNORECASE) is None,
            "Sources, raw and dev artifacts are not distributable production JARs")
    require(jar.is_file(), f"JAR does not exist: {jar}")
    try:
        with zipfile.ZipFile(jar) as archive:
            names = archive.namelist()
            require(len(names) == len(set(names)), "JAR contains duplicate ZIP entries")
            require(archive.testzip() is None, "JAR contains an entry with a bad CRC")
            missing = sorted(REQUIRED_ENTRIES.difference(names))
            require(not missing, "Missing required entries: " + ", ".join(missing))
            for name in REQUIRED_ENTRIES:
                require(archive.getinfo(name).file_size > 0, f"Required entry is empty: {name}")
            for name in names:
                lowered = name.lower()
                require(not (lowered.startswith(("net/fabricmc/loader/", "net/fabricmc/fabric/", "net/fabricmc/api/",
                                                PACKAGE + "fabric/"))
                             or "kotlinforforge" in lowered
                             or lowered in ("fabric.mod.json", "architectury.common.json")),
                        f"Fabric or KotlinForForge runtime content in NeoForge JAR: {name}")

            metadata_text = archive.read("META-INF/neoforge.mods.toml").decode("utf-8")
            require("${" not in metadata_text, "Mod metadata contains unexpanded placeholders")
            metadata = tomllib.loads(metadata_text)
            require(metadata.get("modLoader") == "javafml", "Expected NeoForge javafml loader")
            mods = metadata.get("mods")
            require(isinstance(mods, list) and len(mods) == 1 and isinstance(mods[0], dict),
                    "Expected one mod metadata entry")
            mod = mods[0]
            require(mod.get("modId") == "healthindicators", "Original mod ID must be healthindicators")
            require(mod.get("version") == version, "Metadata version does not match the JAR filename")
            deps_by_mod = metadata.get("dependencies", {})
            require(isinstance(deps_by_mod, dict), "Invalid dependencies table")
            deps = deps_by_mod.get("healthindicators", [])
            require(isinstance(deps, list) and all(isinstance(dep, dict) for dep in deps), "Invalid mod dependencies")
            identifiers = [dep.get("modId") for dep in deps]
            require(all(isinstance(identifier, str) for identifier in identifiers), "Dependency IDs must be strings")
            require(len(identifiers) == len(set(identifiers)), "Duplicate dependency declarations")
            require(not any(isinstance(identifier, str) and ("fabric" in identifier.lower() or "kotlinforforge" in identifier.lower())
                            for identifier in identifiers), "Fabric/KotlinForForge runtime dependency declared")
            target = TARGETS[minecraft]
            expected_ranges = {
                "minecraft": f"[{minecraft},{target['next']})",
                "neoforge": f"[{target['neoforge']},{target['next']})",
                "architectury": f"[{target['architectury']},)",
                "yet_another_config_lib_v3": f"[{target['yacl']},)",
            }
            for identifier, version_range in expected_ranges.items():
                matching = [dep for dep in deps if dep.get("modId") == identifier]
                require(len(matching) == 1, f"Missing required dependency: {identifier}")
                dep = matching[0]
                require(dep.get("type") == "required", f"Dependency {identifier} must be required")
                require(dep.get("side") == "CLIENT", f"Dependency {identifier} must be CLIENT-only")
                require(dep.get("versionRange") == version_range,
                        f"Dependency {identifier} must use target range {version_range}")

            mixin_entries = metadata.get("mixins", [])
            require(isinstance(mixin_entries, list) and any(isinstance(entry, dict) and entry.get("config") == "healthindicators.mixins.json"
                                                          for entry in mixin_entries), "Mixin config is not registered in NeoForge metadata")
            mixins = json.loads(archive.read("healthindicators.mixins.json"))
            require(isinstance(mixins, dict), "Invalid Mixin config")
            require(mixins.get("required") is True, "Required Mixins must not be disabled")
            require(mixins.get("package") == "io.github.adytech99.healthindicators.mixin", "Incorrect Mixin package")
            require(isinstance(mixins.get("client"), list) and all(isinstance(name, str) for name in mixins["client"])
                    and {"EntityDamageMixin", "EntityRendererMixin"}.issubset(mixins["client"]),
                    "Client Mixin declarations are missing")
            injectors = mixins.get("injectors", {})
            require(isinstance(injectors, dict) and isinstance(injectors.get("defaultRequire"), int)
                    and injectors["defaultRequire"] >= 1, "Required Mixin injection counts must remain enabled")
            widener = archive.read("healthindicators.accesswidener").decode("utf-8")
            require(widener.splitlines()[0].strip() == "accessWidener v2 official", "Incorrect access-widener namespace/header")
            for language in ("en_us", "ru_ru", "zh_cn"):
                translations = json.loads(archive.read(ASSETS + f"lang/{language}.json"))
                require(isinstance(translations, dict) and bool(translations), f"Invalid/empty {language} translations")
    except (OSError, zipfile.BadZipFile, RuntimeError, UnicodeError, tomllib.TOMLDecodeError, json.JSONDecodeError) as error:
        raise ArtifactError(f"Unreadable or malformed JAR: {error}") from error

    with jar.open("rb") as artifact_file:
        digest = hashlib.file_digest(artifact_file, "sha256").hexdigest()
    checksum = jar.with_name(jar.name + ".sha256")
    checksum.write_text(f"{digest}  {jar.name}\n", encoding="utf-8")
    return checksum, digest


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--minecraft", required=True, choices=tuple(TARGETS))
    parser.add_argument("jar", type=Path, help="Explicit production JAR path (no glob selection)")
    args = parser.parse_args(argv)
    try:
        checksum, digest = verify_artifact(args.jar, args.minecraft)
    except (ArtifactError, OSError) as error:
        print(f"Packaging verification failed: {error}", file=sys.stderr)
        return 1
    print(f"Packaging verified: {args.jar} ({args.jar.stat().st_size} bytes)")
    print(f"SHA-256: {digest}  (saved to {checksum})")
    print("Minecraft startup, Mixin application, gameplay, OpenGL and Vulkan: NOT TESTED by this packaging check.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
