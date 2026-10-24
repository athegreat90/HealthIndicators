# Changelog

## Unreleased — partial 26.2 / 26.3 NeoForge ports

Target versions: `26.2.0.0-port.1` and `26.3.0.0-port.1`. Neither is a
validated release; no production JAR or remote CI result is available.

- Pin Minecraft/NeoForge, Architectury and YACL per target. Use Java 25,
  Gradle 9.5.1, Architectury plugin 3.5.170 and Loom no-remap 1.17.477.
- Remove unnecessary Kotlin tooling; isolate the unported Fabric module behind
  `-PincludeFabric=true` without deleting its source.
- Configure the NeoForge production Shadow artifact to include common classes,
  resources and the original license, with target-specific metadata and filenames.
- Restrict the mod entrypoint to the client distribution; register keybindings
  on the mod bus and the configuration screen through `ModContainer`.
- Adapt configuration-screen/overlay calls to the 26.2 GUI organization and
  update the style consumer API.
- Add guarded numerical calculations and independent tests; repair filter and
  tracking behavior, world-transition cleanup and unsafe monitoring casts.
- Preserve unknown toggle-config properties and use atomic writes. Full YACL
  GUI/config runtime compatibility remains unverified.
- Remove live-entity capture from deferred geometry callbacks. Full immutable
  render-state snapshots and extreme finite heart/armor handling remain pending.
- Prepare 26.3-specific SDL key constants and `PoseStack.rotate` migration;
  native Vulkan and Improved Transparency remain untested.
- Add 22 passing independent Java tests and 13 passing Python packaging-verifier
  fixture tests, production-JAR checks, checksum generation and GitHub workflows.
- Add fork attribution, engineering rules, source evidence and a manual graphics
  validation checklist. Preserve the original mod ID, assets and LGPL license.

### Blocking validation

Maven/metadata requests are denied with HTTP 403 before compilation. Exact
dependency availability, full builds, production artifacts, client startup,
Mixins, OpenGL/Vulkan, transparency, gameplay and performance remain unverified.
See [COMPATIBILITY.md](COMPATIBILITY.md) and [PORTING_NOTES.md](PORTING_NOTES.md).

## Upstream baseline

`0656b5d3e598d0693ae355615c8f38bf8b45eb02` — `Feat: update to 26.1`.
Original author: AdyTech99. `main` retains that baseline for upstream synchronization.
