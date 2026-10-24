#!/usr/bin/env python3
"""Run the Minecraft-independent logic tests with Java 25 and JUnit 6.

Usage: python scripts/run_logic_tests.py
Set JAVA_HOME or put java and javac on PATH. HEALTHINDICATORS_TEST_CACHE may
override the default ~/.cache/healthindicators/tests dependency cache.
Only build/logic-tests/classes and build/logic-tests/reports are refreshed.
This suite does not validate Minecraft APIs, Mixins or graphics backends.
"""

from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.request


ROOT = Path(__file__).resolve().parents[1]
MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2/"
DEPENDENCIES = (
    (
        "org/junit/platform/junit-platform-console-standalone/6.0.3/"
        "junit-platform-console-standalone-6.0.3.jar",
        "3ba0d6150af79214a1411f9ea2fbef864eef68b68c89a17f672c0b89bff9d3a2",
    ),
    (
        "com/google/code/gson/gson/2.10.1/gson-2.10.1.jar",
        "4241c14a7727c34feea6507ec801318a3d4a90f070e4525681079fb94ee4c593",
    ),
)
CLASSES = (
    "io.github.adytech99.healthindicators.util.IndicatorMath",
    "io.github.adytech99.healthindicators.util.EntityFilters",
    "io.github.adytech99.healthindicators.config.ToggleConfigStore",
)


def executable(name: str) -> str:
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        suffix = ".exe" if os.name == "nt" else ""
        candidate = Path(java_home) / "bin" / (name + suffix)
        if not candidate.is_file():
            raise RuntimeError(f"{name} was not found under JAVA_HOME/bin")
        return str(candidate)
    candidate = shutil.which(name)
    if candidate is None:
        raise RuntimeError(f"{name} is required: set JAVA_HOME to a Java 25 JDK or update PATH")
    return candidate


def require_java_25(command: str, name: str) -> None:
    result = subprocess.run([command, "-version"], capture_output=True, text=True, check=True)
    version = (result.stdout + result.stderr).strip()
    match = re.search(r'(?:version\s+"?|javac\s+)(\d+)', version)
    if match is None or int(match.group(1)) != 25:
        raise RuntimeError(f"{name} must be version 25; reported: {version}")
    print(version.splitlines()[0], flush=True)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def dependency(cache: Path, relative_url: str, expected_hash: str) -> Path:
    path = cache / relative_url.rsplit("/", 1)[1]
    if path.is_file() and sha256(path) == expected_hash:
        print(f"Verified cached {path.name}", flush=True)
        return path
    if path.exists():
        print(f"Replacing cache entry with an unexpected checksum: {path.name}", flush=True)
    print(f"Downloading {path.name} from Maven Central", flush=True)
    temporary = None
    try:
        # urllib honors standard HTTP(S)_PROXY settings and verifies TLS by default.
        with urllib.request.urlopen(MAVEN_CENTRAL + relative_url, timeout=60) as response:
            with tempfile.NamedTemporaryFile(dir=cache, prefix="download-", suffix=".tmp", delete=False) as output:
                temporary = Path(output.name)
                shutil.copyfileobj(response, output)
        if sha256(temporary) != expected_hash:
            raise RuntimeError(f"SHA-256 verification failed for {path.name}; artifact was not used")
        os.replace(temporary, path)
        return path
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)


def refresh_output(path: Path) -> None:
    if path.is_symlink():
        raise RuntimeError(f"Refusing to remove a symlink at the test output path: {path}")
    if path.exists():
        shutil.rmtree(path)
    path.mkdir(parents=True)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.parse_args()
    java, javac = executable("java"), executable("javac")
    require_java_25(java, "java")
    require_java_25(javac, "javac")

    cache = Path(os.environ.get("HEALTHINDICATORS_TEST_CACHE", "~/.cache/healthindicators/tests")).expanduser()
    cache.mkdir(parents=True, exist_ok=True)
    junit, gson = (dependency(cache, url, digest) for url, digest in DEPENDENCIES)
    output = ROOT / "build" / "logic-tests"
    classes, reports = output / "classes", output / "reports"
    refresh_output(classes)
    refresh_output(reports)

    sources = []
    for name in CLASSES:
        relative = Path(*name.split("."))
        sources.extend((
            ROOT / "common" / "src" / "main" / "java" / relative.with_suffix(".java"),
            ROOT / "common" / "src" / "test" / "java" / relative.with_name(relative.name + "Test").with_suffix(".java"),
        ))
    classpath = os.pathsep.join((str(junit), str(gson)))
    print("Compiling the three logic classes and their tests (Java 25)", flush=True)
    subprocess.run([
        javac, "--release", "25", "-encoding", "UTF-8", "-cp", classpath,
        "-d", str(classes), *(str(source) for source in sources),
    ], cwd=ROOT, check=True)
    selectors = [argument for name in CLASSES for argument in ("--select-class", name + "Test")]
    subprocess.run([
        java, "-jar", str(junit), "execute",
        "--class-path", os.pathsep.join((str(classes), str(gson))),
        *selectors, "--reports-dir", str(reports), "--fail-if-no-tests",
        "--details=summary", "--disable-ansi-colors",
    ], cwd=ROOT, check=True)
    print(f"Reports: {reports}", flush=True)


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as error:
        sys.exit(error.returncode if error.returncode > 0 else 1)
    except (OSError, RuntimeError) as error:
        print(f"Logic tests failed: {error}", file=sys.stderr)
        sys.exit(1)
    except KeyboardInterrupt:
        sys.exit(130)
