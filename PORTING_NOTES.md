# Porting notes

## Applied on branch 26.3

Derived from the partial 26.2 port, rather than a validated build, because Maven
access remains blocked. This branch pins Minecraft 26.3, NeoForge 26.3.0.97,
Architectury 22.0.5 and YACL 3.9.7+26.3-neoforge. Its three camera rotations use
`PoseStack.rotate`; the six key defaults use `InputConstants.KEY_*` rather than
GLFW codes. The shared logic suite passed 22/22 tests and packaging-verifier
fixtures passed 13/13 after the branch changes. These tests do not compile the
Minecraft-dependent entrypoint or renderer. The full Gradle invocation failed
during plugin resolution before compilation. No production JAR was generated.

Status: **partial implementation; not a validated release**. The baseline is
`0656b5d3e598d0693ae355615c8f38bf8b45eb02` (`Feat: update to 26.1`), matching
the fork's remote `main` and upstream HEAD when inspected. `main` preserves
that baseline. Branch `26.2` contains the initial port; `26.3` must inherit the
shared work with the explicit limitation that the 26.2 client/build could not
be validated first because dependency repositories are inaccessible.

## Exact targets

| Dependency | 26.2 | 26.3 |
|---|---|---|
| Minecraft | 26.2 | 26.3 |
| NeoForge | 26.2.0.93 | 26.3.0.97 |
| Architectury API | 21.1.12 | 22.0.5 |
| YACL NeoForge | 3.9.7+26.2-neoforge | 3.9.7+26.3-neoforge |
| Java | 25 | 25 |
| Gradle wrapper | 9.5.1 | 9.5.1 |
| Architectury Gradle plugin | 3.5.170 | 3.5.170 |
| Architectury Loom no-remap | 1.17.477 | 1.17.477 |

These are requested pins, not a claim that all Maven artifacts resolved.
No alternate NeoForge version is authorized. Fabric sources are retained;
Fabric compatibility is outside this port's scope.

## Evidence and authoritative sources

Reference repositories were inspected outside the project checkout. SHA-linked
sources describe the inspected revision, not proof of the exact published
NeoForge binary's contents.

- [NeoForge 26.2 source](https://github.com/neoforged/NeoForge/tree/4e2117162e07bdf22de7e705a26fa0a0ae382350),
  SHA `4e2117162e07bdf22de7e705a26fa0a0ae382350`.
- [NeoForge 26.3 source](https://github.com/neoforged/NeoForge/tree/e0595472af773e7fa96482b0abe5371264c9a215),
  SHA `e0595472af773e7fa96482b0abe5371264c9a215`.
- [26.2 migration primer](https://github.com/neoforged/.github/blob/2bb6f9f3bdaec6efe2b69c3241b15fb47b64fc53/primers/26.2/index.md)
  and [26.3 migration primer](https://github.com/neoforged/.github/blob/2bb6f9f3bdaec6efe2b69c3241b15fb47b64fc53/primers/26.3/index.md),
  SHA `2bb6f9f3bdaec6efe2b69c3241b15fb47b64fc53`. NeoForge Documentation's
  `scripts/initialize.js` identifies `neoforged/.github/primers` as its primer
  source. These primers use official Mojang names; the original primers retain
  their authorship and CC BY 4.0 terms.
- Architectury API [26.2 source](https://github.com/architectury/architectury-api/tree/050efc35e9d4a8300e943d36ab761dfd6b961d0a),
  SHA `050efc35e9d4a8300e943d36ab761dfd6b961d0a`; [26.3 source](https://github.com/architectury/architectury-api/tree/8565f9051d38857e1db2d12e9ebf56f5ecc8404f),
  SHA `8565f9051d38857e1db2d12e9ebf56f5ecc8404f`.
- Architectury plugin [source](https://github.com/architectury/architectury-plugin/tree/c3feb632b2148186d63ed1e10e53651fe448e7a0),
  SHA `c3feb632b2148186d63ed1e10e53651fe448e7a0`, and successful official
  [publish workflow run #170](https://github.com/architectury/architectury-plugin/actions/runs/34854530336).
  `gradle.properties` sets `base_version=3.5`; `build.gradle` combines it with
  `GITHUB_RUN_NUMBER`, supporting the `3.5.170` pin. The workflow runs
  `./gradlew publish`. The same properties declare `loom_version_117=1.17.477`.
  Maven resolution of both pins remains required.
- Loom [1.17 source](https://github.com/architectury/architectury-loom/tree/5b00f1e37558b4a063f0b0c4c8c8613e15b6db68),
  SHA `5b00f1e37558b4a063f0b0c4c8c8613e15b6db68`. The Git tag `1.17` is not
  itself a complete Maven version: its build also appends a CI run number.

## Initial source audit

The baseline uses Architectury common/Fabric/NeoForge modules, Java 25,
Gradle 9.5.1, Architectury plugin `3.5-SNAPSHOT`, and Loom no-remap
`1.14-SNAPSHOT`. NeoForge applies an unnecessary Kotlin plugin despite having
no Kotlin source. Common output is bundled through the platform's Shadow
configuration; production packaging must be checked for common classes,
resources and accidental Fabric runtime requirements.

The entity renderer injects at `LivingEntityRenderer.extractRenderState` and
`submit`, associates render states with live `LivingEntity` objects using a
`WeakHashMap`, and submits icons as custom textured geometry and numbers via
`OrderedSubmitNodeCollector.submitText`. No separate Vulkan renderer, custom
shader, direct OpenGL call or direct Vulkan call is required by that design.
Deferred callbacks must not retain mutable entity/config references.

Both Mixins are listed on the client side with required injections. The access
widener files contain only comments after their headers. `JAVA_21` in Mixin
metadata is not automatically wrong merely because Java 25 compiles the mod.
The actual selected Mixin implementation and client startup must validate it.

## Verified API migration points

- NeoForge `src/client/java/.../client/ClientNeoForgeMod.java`, in both inspected
  branches, demonstrates `@Mod(value = ..., dist = Dist.CLIENT)`, constructor
  injection of `IEventBus` and `ModContainer`, registration on the mod event
  bus, and `container.registerExtensionPoint(IConfigScreenFactory.class, ...)`.
  These are preferred over loading client classes through an unrestricted
  entrypoint and a legacy construct-event registration.
- `RegisterClientCommandsEvent.getDispatcher()` still exposes
  `CommandDispatcher<CommandSourceStack>` on `NeoForge.EVENT_BUS`. Keep the
  existing client command tree and avoid adding server permission checks.
- Architectury's `common/src/main/java/dev/architectury/event/events/client/ClientGuiEvent.java`
  retains `RENDER_HUD` and `renderHud(GuiGraphicsExtractor, DeltaTracker)` in
  both branches. NeoForge's GUI source also uses the existing `text` and
  `fill` operations.
- The 26.2 primer moves `Minecraft.screen` to `Minecraft.gui.screen()`,
  `Minecraft.setScreen` to `Minecraft.gui.setScreen`, and
  `gui.setOverlayMessage` to `gui.hud.setOverlayMessage`. These changes affect
  the configuration-screen and temporary-filter overlay code directly.
- The NeoForge patches
  `patches/net/minecraft/client/renderer/entity/LivingEntityRenderer.java.patch`
  show `submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)` in both
  branches. `EntityRenderer.java.patch` shows `extractRenderState(entity,
  state, partialTicks)`, `finalizeRenderState`, and
  `shouldShowName(entity, state.distanceToCameraSq)`.
- The patched `EntityRenderer` creates a render state before extraction; this
  does not establish a universal lifecycle guarantee for every subclass or
  future release. Replacing the live-entity association with immutable,
  extraction-time indicator snapshots remains necessary before declaring
  rendering lifecycle/thread safety complete.
- Both NeoForge branches' `tests/.../client/MapDecorationRenderTests.java`
  demonstrate `submitCustomGeometry(PoseStack, RenderTypes.text(texture),
  callback)` and vertex position/color/UV/light submission. The inspected
  sources do not establish that every current texture/see-through combination
  remains visually correct.
- 26.2 removes `Font.drawInBatch` and `MultiBufferSource`; keep number rendering
  in `submitText` instead of rebuilding a manual font pipeline.
- 26.3 renames `PoseStack.mulPose` to `rotate`. Apply that change only in the
  26.3 branch. `PoseStack` and `VertexConsumer` remain in Blaze3D; do not move
  every import into RenderPearl without evidence.
- The 26.3 primer remaps keyboard inputs from GLFW to SDL and recommends
  `InputConstants.KEY_*` or SDL scancodes. NeoForge's `KeyModifier.java`
  explicitly uses `InputConstants.KEY_RSHIFT`; keybinding registration and
  persistence still need runtime validation against the exact target.
- 26.3 merges `SubmitNodeCollection.seeThroughNameTags` into `seeThrough` and
  adds `oitTranslucent`. `SubmitNodeStorage` owns improved-transparency
  selection. Keep submissions in Minecraft's normal phases; do not call whole
  feature-rendering phases manually to draw indicators.
- 26.3 removes `RenderTypes.textBackground` and `textBackgroundSeeThrough` and
  introduces `OrderedSubmitNodeCollector.submitTextBackground`. Existing
  `submitText` background behavior must be checked in the resolved sources
  before adding any replacement call or custom pipeline.

## Configuration, tracking and numerical logic

Keep `healthindicators.json` and `health_indicators_config.json`, serialized
property names and enum identifiers. Retain unknown toggle properties when
writing supported settings and preserve invalid legacy input rather than
silently replacing the user's file. Test missing/unknown properties and
round trips independently; actual YACL GUI open/save/restart tests still need
the resolved runtime.

Tracking and HUD state must clear on disconnect, world changes, respawn and
removed/dead entities. Damage-source information visible only to an integrated
server must not be invented for multiplayer clients. Keep existing local-server
restrictions until equivalent client-visible information is demonstrated.

Non-finite and invalid health inputs require finite calculations, defined
rounding and safe dynamic-mode selection. Extreme finite health/absorption
values also require overflow-safe heart counts and rendering bounds without
silently removing normal features. Pure-Java logic tests cannot establish
Minecraft rendering correctness.

## Current blockers and required follow-up

Read-only GitHub access succeeds. The current machine's HTTPS proxy rejects
Maven/metadata requests with **HTTP 403 / CONNECT tunnel failed** for
`maven.neoforged.net`, `maven.architectury.dev`, `maven.fabricmc.net`,
`files.minecraftforge.net`, `api.modrinth.com`, and
`piston-meta.mojang.com`. The direct documentation domain is also denied;
official GitHub source mirrors provided the references above. The earlier
baseline Gradle build stopped resolving the Architectury plugin before Java
compilation. Java 25 and Gradle 9.5.1 themselves run successfully.

The denials **do not prove that any requested artifact is absent**. Network
requirements saved in the environment draft do not apply themselves to the
running instance. Activate the relevant environment settings, then:

1. Resolve every exact target and plugin artifact, inspect their actual source
   signatures, and run both NeoForge builds and actual test tasks. Investigate
   each failure without substituting different pins.
2. Finish and test immutable render snapshots and safe extreme heart counts.
   Verify backend-independent geometry, opacity, occlusion, text and assets.
3. Launch each exact client and validate all required Mixin injection points;
   compilation alone cannot validate Mixins.
4. Inspect final production JAR metadata, entrypoint, common classes, Mixins,
   resources and dependencies, then produce SHA-256 checksums. No source/dev
   JAR should be presented as a production artifact.
5. Execute actual OpenGL and native Vulkan game tests; confirm the backend in
   F3/system specifications to detect fallback. For 26.3 repeat both backends
   with Improved Transparency OFF and ON. These tests are **NOT TESTED** until
   such execution is recorded.
6. Check keybindings, commands, config persistence, filters, status textures,
   monitoring HUD and damage-direction behavior in game. Benchmark comparable
   1/20/100-entity scenes if graphics hardware permits. Performance is
   **NOT TESTED**; no Vulkan speedup is claimed.

Do not publish a release or mark the port fully compatible while these
requirements remain unresolved. Branch-level source changes and automated
logic checks are separate evidence from a successful production build and
graphics/runtime validation.
