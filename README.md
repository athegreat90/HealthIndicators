# Health Indicators — NeoForge compatibility fork

An independently maintained fork by [athegreat90](https://github.com/athegreat90/HealthIndicators)
of [AdyTech99's Health Indicators](https://github.com/AdyTech99/HealthIndicators),
which displays health and armor above living entities, numerical health, a
monitoring HUD and damage-direction indicators. This fork is not endorsed by
the original author. Original project pages:
[Modrinth](https://modrinth.com/mod/health-indicators) ·
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/health-indicator).

**Work in progress.** The port is partial: Maven requests are blocked by HTTP
403 in the current development environment. Neither target has a successful
full build, production JAR or client launch. OpenGL, native Vulkan, Mixins,
gameplay and performance are **NOT TESTED**. See [COMPATIBILITY.md](COMPATIBILITY.md)
for results and remaining work; do not treat the target table as certification.

## Targets

| Branch | Minecraft | NeoForge | Architectury API | YACL NeoForge | Mod version |
|---|---|---|---|---|---|
| `26.2` | 26.2 | 26.2.0.93 | 21.1.12 | 3.9.7+26.2-neoforge | 26.2.0.0-port.1 |
| `26.3` | 26.3 | 26.3.0.97 | 22.0.5 | 3.9.7+26.3-neoforge | 26.3.0.0-port.1 |

Both targets use Java 25 and the Gradle 9.5.1 wrapper. `main` retains the
upstream 26.1 baseline. The intent is one NeoForge artifact per Minecraft
version using Minecraft's rendering abstractions for both OpenGL and native
Vulkan, with Improved Transparency ON/OFF on 26.3. This is not yet verified.
Fabric sources remain available but are not part of the port or its support
claims; `-PincludeFabric=true` opts into configuring that unported module.

## Installation when validated builds become available

Install only the production JAR for your exact Minecraft target, the pinned
NeoForge version, and matching NeoForge Architectury API and YACL dependencies
from the table. Use Java 25. Place the mod in the **client's** `mods` directory;
a dedicated server does not need it. No production JAR is supplied by this
partial port yet.

**Do not install the original mod alongside this fork:** both use mod ID
`healthindicators`. The original Java package, asset namespace, license,
attribution and configuration names are preserved. Existing configuration
files are `healthindicators.json` and `health_indicators_config.json`; back them
up before trying a development build. Actual YACL GUI compatibility is pending.

## Controls and commands

Default keys, configurable in Minecraft's controls menu:

| Key | Action |
|---|---|
| Left arrow | Toggle health indicators |
| Right Shift | Toggle armor indicators |
| Hold right arrow | Temporarily override filters |
| Up / down arrows | Increase / decrease indicator offset |
| I | Open configuration |

Client commands, when enabled in configuration:

```text
/healthindicators
/healthindicators offset <value>
/healthindicators indicator-type <heart|number>
/healthindicators monitor <entity_name>
/healthindicators stop-monitoring
```

Quote names containing spaces. Dynamic health display remains available in
configuration. Damage-direction rendering retains the original singleplayer
restriction; multiplayer information is not invented. Controls and commands
have not been exercised in the target clients.

## Build and checks

Check out the desired version branch. Install a Java 25 JDK and Python 3.11+
and set `JAVA_HOME`/`PATH` accordingly. The wrapper downloads Gradle; Maven and
Minecraft repositories must be reachable. Pins and source evidence are recorded
in [PORTING_NOTES.md](PORTING_NOTES.md).

Linux/macOS, from the repository root:

```bash
bash ./gradlew clean :neoforge:build --stacktrace
bash ./gradlew :common:test :neoforge:test --no-daemon
```

Windows PowerShell:

```powershell
.\gradlew.bat clean :neoforge:build --stacktrace
.\gradlew.bat :common:test :neoforge:test --no-daemon
```

Independent checks that can run while Minecraft dependencies are inaccessible:

```bash
python scripts/run_logic_tests.py
python -m unittest discover -s scripts/tests -v
```

The first command downloads a checksum-verified JUnit 6.0.3 console runner and
compiles selected dependency-independent Java sources with Java 25. Set
`HEALTHINDICATORS_TEST_CACHE` to override its runner cache. These tests do not
compile the full mod or validate GPU rendering. Current results: 22 Java tests
and 13 Python packaging-verifier fixture tests passed.

After a successful full build, verify the explicit production path (example
for branch `26.2`):

```bash
python scripts/verify_artifact.py --minecraft 26.2 neoforge/build/libs/HealthIndicators-NeoForge-26.2-26.2.0.0-port.1.jar
```

For `26.3`, use `--minecraft 26.3` and the corresponding versioned filename.
The verifier checks common classes, metadata, resources and dependencies and
writes `<jar>.sha256` only after passing. It does not launch Minecraft. Do not
use `-raw`, sources or development JARs as production artifacts.

Build and manual draft-release workflows are included but have not run on
GitHub. The release workflow requires an existing matching tag, explicit
runtime validation and an HTTPS evidence URL, then rebuilds and prepares a
**draft** release. No releases have been created, and no Modrinth/CurseForge
publication is automated.

## License

[LGPL-3.0](LICENSE), preserving AdyTech99's authorship and original history.
Modified source remains available in this fork; retain the license and source
availability when redistributing. See [CHANGELOG.md](CHANGELOG.md) for changes
and [AGENTS.md](AGENTS.md) for engineering rules.
