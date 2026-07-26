# Danger Sense

A Fabric mod that warns the player about dangerous situations (creepers, TNT, Wardens, low health, lack of air, lava, falls) before they become fatal. **Never blocks damage** — only provides a vignette overlay, heartbeat sound, and pulsing effects.

Built using your **FigureStonePlugin** (successor to MossyPlugin), on **official Mojang mappings** (not Yarn) — as intended in the new, post-rebranding scheme of your ecosystem.

## Package Structure

```
com.darkz.dangersense
├── DangerSenseMod          — common entrypoint, sound registration, config loading
├── client/
│   ├── DangerSenseClient   — client entrypoint, HUD/tick callback registration
│   └── ClientTickHandler   — links WarningManager, HeartbeatPlayer, and VignetteOverlay
├── config/
│   ├── DangerSenseConfig   — POJO for Gson
│   └── ConfigManager       — JSON load/save + optional hot reload
├── danger/
│   ├── DangerLevel         — NONE/LOW/MEDIUM/HIGH/CRITICAL with base intensity
│   ├── DangerWarning       — record: single tick result (source, level, intensity)
│   └── WarningManager      — ticks all detectors, selects the strongest warning
├── detector/
│   ├── DangerDetector      — common interface (shouldWarn + getLevel/getIntensity)
│   ├── CreeperDetector, TntDetector, WardenDetector,
│   │   LowHealthDetector, DrowningDetector, LavaDetector, FallDetector
├── render/
│   ├── VignetteOverlay     — procedural vignette at screen edges (no textures)
│   └── PulseAnimation      — sinusoidal alpha pulsation
├── sound/
│   └── HeartbeatPlayer     — heartbeat playback with interval based on intensity
└── util/
    ├── RaycastUtil         — line-of-sight without excessive allocations
    └── TickThrottle        — "once every N ticks" without extra logic
```

Package — `com.darkz.dangersense` (matching `com.darkz.skintotem` in your real SkinTotem), not `com.darkz70.*`.

## Build System (FigureStonePlugin)

All Gradle wrapping is handled via your `net.darkz70.figurestone-plugin-*` plugin (`settings` and `core` modules), rather than bare `fabric-loom`:

- **`settings.gradle`** — applies `net.darkz70.figurestone-plugin-settings`.
  It automatically creates Stonecutter subprojects `fabric-<version>` from `fabric.multi_versions` in the root `gradle.properties`, and generates/updates `versions/fabric-<version>/gradle.properties` (only `build.fabric_api` is needed there — the plugin fills it automatically if the file is empty).
- **`build.gradle`** (root, applied to each version subproject) — connects `net.darkz70.figurestone-plugin-core` and configures the `figurestoneDependencies { minecraft, fabricApi, fabricLoader, lombok }` block.
  FigureStoneLib is connected separately (it doesn't go through the plugin's `dep.*` mechanism — that's for Modrinth mods, while FigureStoneLib is in your private GitHub Packages).
- **`gradle.properties`** — follows the `data.*` / `mod_loaders` / `fabric.multi_versions` scheme, as seen in your `FigureStonePlugin_repository.zip` archive.
- Version folders — **`versions/fabric-1.20.1`**, not `versions/1.20.1` (Stonecutter project name = `<loader>-<version>`).

### ✅ Plugin Version

`settings.gradle`/`build.gradle` use `version "1.2.0"` — confirmed by you directly (the provided `FigureStonePlugin_repository.zip` archive had `3.7.0-beta.19` hardcoded, which was likely an outdated snapshot).

### ✅ `figurestone-plugin-stonecutter` Module Integration

The second archive included the `stonecutter` module (which was missing from the first). It provides aggregate tasks like `buildAndCollect+<loader>+All`, `buildAndCollect+All`, `publish+All`, etc. Specifically, `buildAndCollect+All` is called in your real SkinTotem `build.yml` (`./gradlew buildAndCollect+All || ...`). It is applied in the same `build.gradle` as `-core`: with `dev.kikugie.stonecutter.hard_mode=true`, the root project acts as the currently active version, so a single file works as both a per-version script and a root script (with visibility of child projects).

`buildAndCollect` (per-version, from `-core`) copies the resulting jar to the root `libs/` — `.github/workflows/build.yml` has been updated to reflect this (`path: libs/*.jar` instead of `versions/fabric-*/build/libs/*.jar`).

## ⚠️ Key Note: Official Mojang Mappings, not Yarn

`FabricLoaderManager` (plugin's core module) connects mappings as follows:
```java
dependencies.add("mappings", loom.officialMojangMappings());
```
Therefore, all Java code here is written using Mojang class/method names (`Player`, `ClientLevel`, `Creeper`, `PrimedTnt`, `Warden`, `getDeltaMovement()`, `onGround()`, `distanceToSqr()`, etc.), not Yarn (`PlayerEntity`, `ClientWorld`, `squaredDistanceTo()`, etc.).

**Some method names cannot be confirmed without access to real mappings or a compiled project** — such places are marked in the code with comments like `⚠ MOJMAP UNCERTAINTY` / `⚠ MOJMAP:`. List of most "at-risk" locations:

| File | Location | Risk |
|---|---|---|
| `DangerSenseMod.java` | `SoundEvent.createVariableRangeEvent(...)`, `new ResourceLocation(...)` | Medium — `ResourceLocation` constructor might be deprecated/private in late 1.21.x |
| `CreeperDetector.java` | `creeper.getSwellFactor(0f)` | High — uncertain of the exact name; fallback is noted in comments (`isIgnited()`) |
| `TntDetector.java` | `tnt.getFuse()` | Low-Medium |
| `FallDetector.java` | `result.getLocation()` | Medium |
| `HeartbeatPlayer.java` | `player.level()` | Medium |
| `RaycastUtil.java`, `FallDetector.java` | `ClipContext.Block` / `ClipContext.Fluid` | Medium |

Other replacements (`getHealth()`, `getAirSupply()`, `getDeltaMovement()`, `onGround()`, `distanceToSqr()`, `getEntitiesOfClass()`, `inflate()`, `position()`, `blockPosition()`, `getEyePosition()`, `BlockPos.containing()`, `fluidState.is()`, `guiWidth()/guiHeight()`) have higher confidence but remain unverified by compilation. **This will inevitably require a CI run and fixes based on error logs** — just as you usually work with new APIs.

## Java Version by MC Version (Auto, from your plugin)

`FigureStonePluginCore.getJavaVersion()` automatically selects the JDK based on the MC version:
`< 1.16.5 → 8`, `< 1.18 → 16`, `< 1.20.5 → 17`, `< 26.1 → 21`, `≥ 26.1 → 25`.
Thus, for the entire Danger Sense range (1.20.1–1.21.11), **JDK 21** will actually be used, not 25 — the plugin will select it regardless of the original requirements.

## Performance

- Detectors are called every `scanIntervalTicks` (default 4) via `TickThrottle` in `WarningManager`, not every single tick.
- Entity searching — `getEntitiesOfClass` with a limited `AABB` around the player, avoiding full-world scans.
- `RaycastUtil` — uses a single `ClipContext` without intermediate lists.
- Vignette is rendered procedurally via `fillGradient`, without textures.

## Configuration

`config/dangersense.json` is created automatically on the first launch (default values are taken from `config-default.json` in the root). Hot reload (`hotReloadEnabled`) checks the file approximately once per second, not every tick.

## FigureStoneLib — Required Dependency

FigureStoneLib is connected as a **hard dependency**, not optional:

- `build.gradle`: `modImplementation "com.darkz:figurestonelib:${prop("dep.figurestonelib")}"`
- `gradle.properties`: `dep.figurestonelib = 1.0.0` (update to the current version)
- `fabric.mod.json`: `"figurestonelib": ">=${figurestonelib}"` in `depends` — the game will not start the mod without it (Fabric Loader will show a missing dependency screen).

⚠ I have the source code for **FigureStonePlugin** (build plugin), but not for **FigureStoneLib** (the library) itself — therefore, I cannot substitute real API calls (methods, classes) into the code, only leave integration points as comments. If you provide the library's source code, I can perform a proper integration.

Potential integration points for FigureStoneLib:
1. **`config/ConfigManager`** — if the library has a common `JsonConfig<T>`/file-watcher, replace manual Gson I/O with it.
2. **`util/RaycastUtil`** — if it already contains raycast/line-of-sight utilities.
3. **Logging** — `DangerSenseMod.LOGGER` can be replaced with a common logger.

## Gradle Wrapper

`gradlew` / `gradlew.bat` / `gradle-wrapper.properties` (Gradle 8.14) and `gradle-wrapper.jar` are all included (the jar is the one you uploaded, scripts are copied from your real SkinTotem).

## GitHub Actions

`.github/workflows/build.yml`: Uses JDK 21 (verified option, see Java version section), `chmod +x ./gradlew`, `./gradlew build`. The `GH_PACKAGES_TOKEN` secret is required for both FigureStoneLib and FigureStonePlugin (`settings.gradle` uses it too) — ensure it has read permissions for both private packages.

## Missing / Unverified Items

- All `⚠ MOJMAP` locations in the code (see table above) — the first CI run will indicate what needs fixing.
- Real FigureStoneLib calls (see section above) — only build plugin sources are available, not the library itself.
