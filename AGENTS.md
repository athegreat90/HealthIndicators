# Engineering instructions

This is athegreat90's independent LGPL-3.0 compatibility fork of AdyTech99's
Health Indicators. Preserve history, attribution, license, mod ID
`healthindicators`, package/asset namespace and existing configuration keys.
Read `PORTING_NOTES.md` and `COMPATIBILITY.md` before continuing the port.

## Branches and scope

- `main` retains the upstream baseline for straightforward synchronization;
  do not merge version-specific ports into it without justification.
- `26.2` targets Minecraft 26.2 / NeoForge **26.2.0.93**, Architectury **21.1.12**
  and YACL NeoForge **3.9.7+26.2-neoforge**.
- `26.3` targets Minecraft 26.3 / NeoForge **26.3.0.97**, Architectury **22.0.5**
  and YACL NeoForge **3.9.7+26.3-neoforge**.
- Keep shared fixes consistent across the two ports. Their current status is
  partial, with 26.3 derived from an unvalidated 26.2 port because of network
  blockers. Do not describe either as completed until the evidence supports it.
- Preserve Fabric sources. `-PincludeFabric=true` configures the unported
  module; do not claim Fabric compatibility without testing it.

Inspect `git status`, branches and remotes before editing. Preserve user changes.
Use logical commits; never force-push, reset user work or delete branches.
Keep `origin` as this fork and `upstream` as AdyTech99/HealthIndicators. Integrate
upstream changes deliberately on the appropriate branch, preserving attribution.
Cloud tasks are already isolated: use the existing checkout. Create worktrees
only when explicitly requested or needed for authorized parallel branch work.

## Toolchain and validation

Use **Java 25**, the checked-in **Gradle 9.5.1** wrapper, Architectury plugin
**3.5.170**, Loom no-remap **1.17.477** and Shadow **9.2.2**. Do not introduce
Kotlin runtime requirements. Keep exact compatible dependency pins; never
silently change the requested Minecraft/NeoForge versions or use dynamic versions.
Check actual target source signatures instead of guessing APIs.

From the repository root, with a Java 25 JDK selected:

```bash
bash ./gradlew :common:test :neoforge:build --no-daemon --stacktrace
python scripts/run_logic_tests.py
python -m unittest discover -s scripts/tests -v
```

Use `gradlew.bat` on Windows. Python tools require Python 3.11+;
`HEALTHINDICATORS_TEST_CACHE` optionally selects the JUnit runner cache.
Run incremental builds during iteration; use `clean :neoforge:build` for final
artifact validation. Do not regard NO-SOURCE/zero-test runs as passing suites.

After a real build, invoke `scripts/verify_artifact.py --minecraft <26.2|26.3>
<explicit-production-jar-path>`. The verifier writes a SHA-256 sidecar after
validating metadata/common classes/resources. Keep raw/dev/sources artifacts
distinct from the production JAR. Independent Java tests and fixture JAR tests
do not establish that the full mod compiles or runs.

Preserve TLS, artifact checksums and package verification. Network HTTP 403 is
an access blocker, not evidence that a pinned artifact does not exist. Never
extract credentials or put secrets in repository files or logs.

## Rendering and behavior

- Keep the mod client-only. Do not require installation on dedicated servers,
  add custom world data or add unnecessary networking.
- One JAR per Minecraft version must use Minecraft's normal rendering lifecycle
  for both OpenGL and native Vulkan. Do not implement a separate Vulkan renderer,
  add native graphics bindings or manipulate direct OpenGL/global graphics state.
- Submit geometry/text through supported Minecraft abstractions. Let Minecraft
  select graphics devices, fallback behavior and OIT phases. Do not manually
  render whole feature phases to draw indicators.
- Verify 26.3-specific SDL input and RenderPearl/OIT changes against the exact
  sources. Avoid mechanical package renames. `PoseStack.rotate` is a 26.3 change.
- Do not disable required Mixins, reduce required injections, suppress rendering
  exceptions or remove features to obtain a build.
- Finish immutable extraction-time snapshots; avoid mutable live-world/config
  references in deferred rendering, stale state, unbounded tracking and
  unnecessary per-frame allocations.
- Preserve all working indicators, textures, status variants, commands, settings
  and existing config filenames. Handle invalid/non-finite/extreme values safely.
  Preserve unknown config fields and existing files during migration.
- Keep the current singleplayer damage-direction restriction until equivalent
  client-visible information is proven; do not invent multiplayer data.

## Honest delivery

Track passed, failed, blocked, skipped and unrun checks separately. No fabricated
APIs, artifacts, checksums, CI results, performance claims or graphics PASS results.
Compilation does not validate Mixins: launch the exact client and inspect logs.
Confirm active OpenGL/Vulkan in F3/system specifications; fallback is not a Vulkan
PASS. On 26.3 test Improved Transparency OFF and ON on both backends.

Follow the manual checklist in `COMPATIBILITY.md`; record exact commit/tag, JAR
checksum and runtime evidence. No GPU/display means graphics tests are NOT TESTED.
Do not publish an unvalidated release. Release automation requires explicit
maintainer validation and an HTTPS evidence URL, rebuilds the tagged source and
creates only a draft. Do not automate Modrinth/CurseForge publication.
