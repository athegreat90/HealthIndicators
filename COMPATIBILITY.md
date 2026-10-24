# Compatibility and validation

**Partial port, not a supported release.** Exact dependency pins are targets;
their Maven artifacts have not resolved in this environment. HTTP 403 from the
network proxy blocks the required repositories before compilation. This does
not establish that the requested versions are absent.

| Target | NeoForge pin | Full build / JAR | Client / Mixins |
|---|---|---|---|
| Minecraft 26.2 | 26.2.0.93 | BLOCKED | NOT TESTED |
| Minecraft 26.3 | 26.3.0.97 | BLOCKED | NOT TESTED |

No Minecraft/NeoForge combination is runtime-certified. Fabric is unported.
This fork is client-only; do not install it alongside the original mod because
both declare `healthindicators`.

## Executed checks

| Check | Result | Scope |
|---|---|---|
| Java 25 and Gradle 9.5.1 startup | PASS | Tools execute locally |
| Independent JUnit 6.0.3 tests | PASS — 22 tests | Numeric logic, filters and toggle-config persistence |
| Python verifier fixture tests | PASS — 13 tests | Packaging checker positive/negative cases |
| Full mod compilation, Gradle tests and production packaging | BLOCKED | Maven access denied |
| Actual production-JAR inspection and checksum | NOT TESTED | No production JAR exists |
| GitHub build/release workflows | NOT TESTED | Definitions added; no remote execution recorded |
| Workflow static validation | PASS | actionlint 1.7.7; no remote execution implied |

Fixture JARs test the verifier, not the actual mod. Independent logic tests
do not establish full YACL, Mixin, Minecraft API or graphics compatibility.

## Graphics matrix

| Minecraft | OpenGL | Native Vulkan | Improved Transparency |
|---|---|---|---|
| 26.2 | NOT TESTED | NOT TESTED | Availability and behavior NOT TESTED |
| 26.3 | NOT TESTED | NOT TESTED | OFF: NOT TESTED |
| 26.3 | NOT TESTED | NOT TESTED | ON: NOT TESTED |

The cloud environment provides no GPU/display for actual gameplay validation.
No FPS, frame-time or memory comparison has been measured. Backend-independent
submission code is an implementation goal, not evidence of a passing Vulkan test.

## Known limitations

- Maven and Minecraft metadata access returns HTTP 403. Activate the required
  environment network settings, then resolve exact dependencies and build both
  branches. Do not silently change the requested pins.
- The render-state `WeakHashMap` still associates states with live entities.
  Deferred geometry callbacks no longer capture the entity, but a complete
  immutable extraction-time snapshot migration remains outstanding.
- Extremely large finite health/absorption/armor counts still need bounded,
  overflow-safe rendering without changing normal display behavior.
- Custom-texture namespace/label inconsistencies need review in the real client.
- YACL menu opening, defaults, legacy-config loading, saving, reset controls and
  persistence across client restarts have not been runtime-tested. The separate
  toggle-file persistence logic has independent tests.
- Damage-direction indicators retain the original singleplayer-only behavior.
- Mixins remain required; no startup has established that their injection
  descriptors work against the exact target binaries.
- 26.3 is derived from the partial 26.2 implementation because 26.2 validation
  is blocked. This is not derivation from a completed, validated port.

## Manual release checklist

Use each exact target with its matching Architectury/YACL dependencies.
Retain logs and screenshots linked to the exact commit/tag and JAR checksum.

1. Launch with **Prefer OpenGL**, restart as required, and confirm the active
   backend in F3/system specifications. Check startup logs for Mixin and linkage
   errors; a build alone does not validate injections.
2. Repeat with **Prefer Vulkan (Experimental)**. Verify Vulkan is actually
   active; fallback to OpenGL cannot count as a Vulkan PASS.
3. On 26.3, repeat both backends with Improved Transparency OFF and ON. Record
   options that are unavailable rather than inventing results.
4. Check hearts, half/empty/absorption hearts, poison/wither/frozen/hardcore and
   regeneration variants, numerical/dynamic mode, armor, rows, size, opacity,
   colors, decimals, text shadow/background and extreme health values.
5. Check camera-facing geometry, depth, occlusion and see-through behavior at
   different angles/distances, including behind opaque/translucent objects.
6. Check filters, damage tracking, monitoring HUD, keybindings, commands, config
   GUI and persistence. Change world, disconnect, respawn and unload entities;
   ensure stale HUD/tracking state disappears.
7. Exercise zombie, skeleton, creeper, cow, villager, iron golem, another player
   and high-health entities. Check GUI scales, resolutions and windowed/fullscreen.
8. If hardware permits, compare identical 1/20/100-entity scenes with and without
   the mod; record frame times, spikes, CPU/GPU load and memory for each backend.

Only record PASS after executing the relevant case. Release automation requires
maintainer runtime evidence and creates a draft; it does not perform these tests.
