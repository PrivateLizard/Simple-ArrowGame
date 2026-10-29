# Arrow Puzzle

A fully offline line-art arrow puzzle for Android 7.0+ (API 24). Clear every
arrow. An arrow can only be tapped when no other arrow lies on the ray in front
of its arrowhead.

Every board is generated and validated on-device.

## Game rules

- Tap an arrow to remove it. Pinch to zoom and drag the board to inspect busy areas.
- Every arrow must be removed to complete the level.
- An arrow is open when no other arrow is in its forward path.
- Several arrows can be open at the same time.
- There are always at least two open arrows, so a level can never lock up.
- Tapping a blocked arrow counts as a failure.
- Reaching the configured failure limit ends the level.
- Optional time limits end a level when the timer expires.

## Features

- Dense, chaotic line-art arrow fields with 100% lattice coverage and no empty cells
- Very short and very long, heavily bent arrows in every level
- Late long arrows can use dozens of bends and snake across the whole board
- Removed arrows unspool head-first: the arrowhead leaves in a straight line while the tail follows the original path
- Pinch-to-zoom and drag-to-pan on the game board
- 100 campaign levels on a 100-step difficulty scale; level 20 sits at
difficulty 20 of 100, the finale at 100
- No overlapping or crossing arrow strokes
- Acyclic blocking graph, validated before the board is shown
- Difficulty controls how many arrows are open at the start:
  - EASY: 4 open arrows
  - MEDIUM: 2 open arrows
  - HARD: 3 open arrows
- 20 campaign levels with growing boards; later levels have longer arrows
- The layout area grows every two levels; the number of open arrows drops
- Custom mode: 6-40 arrows, 3x3-8x8 board scale, 1-99 failures,
  optional 30-180 s timer, three complexity levels
- Light and dark palettes, switchable in Settings; Night Mode is on by default
- Persistent high scores, per-level bests, level unlocking, and settings
- Score formula implemented as specified:
  - `base = arrows * 10`
  - `time = max(0, 60 - floor(elapsedSeconds)) * 5`
  - `combo = base * 0.5` when there are no failures
- No internet permission, no ads, no analytics, no third-party runtime libraries

## Technical stack

- Kotlin 1.9.24
- Jetpack Compose (BOM 2024.06.00, Material 3)
- Compose Navigation
- MVVM with StateFlow and repository classes
- Manual dependency injection through `AppContainer` / `SimpleViewModelFactory`
- SharedPreferences-backed persistence
- AGP 8.4.2 / Gradle 8.6
- compileSdk 34, targetSdk 34, minSdk 24

## Project structure

```text
app/src/main/java/com/arrowpuzzle/game/
|-- MainActivity.kt
|-- ArrowPuzzleApplication.kt
|-- navigation/
|   |-- AppNavHost.kt
|   `-- AppRoutes.kt
|-- ui/
|   |-- screens/
|   |   |-- HomeScreen.kt
|   |   |-- LevelSelectScreen.kt
|   |   |-- GameScreen.kt
|   |   |-- VictoryScreen.kt
|   |   |-- DefeatScreen.kt
|   |   |-- SettingsScreen.kt
|   |   `-- CustomModeScreen.kt
|   |-- components/Common.kt
|   |-- theme/Theme.kt
|   `-- viewmodel/
|       |-- GameViewModel.kt
|       |-- HomeViewModel.kt
|       |-- LevelSelectViewModel.kt
|       |-- SettingsViewModel.kt
|       `-- SimpleViewModelFactory.kt
|-- game/
|   |-- LevelGenerator.kt
|   |-- LevelCatalog.kt
|   |-- ArrowGeometry.kt
|   |-- ArrowLogic.kt
|   |-- PathValidator.kt
|   |-- GameState.kt
|   `-- GeneratedLevel.kt
|-- scoring/ScoreCalculator.kt
|-- data/
|   |-- ScoreRepository.kt
|   |-- SettingsRepository.kt
|   |-- AppContainer.kt
|   `-- model/
|       |-- Arrow.kt
|       |-- LevelConfig.kt
|       |-- ScoreRecord.kt
|       `-- AppSettings.kt
`-- util/
    |-- GridUtils.kt
    `-- SoundEffects.kt
```

## Colors and themes

All colors are centralised in `ui/theme/Theme.kt` as `DarkArrowPalette` and
`LightArrowPalette`. Screens use the palette through `ArrowTheme` and the
`App...` color properties. Night Mode is a persisted setting and defaults to on.
The app also updates the system status/navigation bar appearance when the theme
changes.

## Build

Use JDK 17 or newer (the Android Studio JBR works).

```bash
# Windows
gradlew.bat assembleRelease

# macOS / Linux
./gradlew assembleRelease
```

The unsigned release APK is written to:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

A verified prebuilt copy is included at `dist/app-release-unsigned.apk`.
The signed release is `dist/app-release-signed.apk`, signed with
`keystore/arrowpuzzle-release.jks` (alias `arrowpuzzle`).

No signing configuration is included in Gradle. Sign future APKs with the same
keystore:

```bash
apksigner sign --ks my-release-key.jks \
  --out app-release-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

Build and test:

```bash
gradlew.bat testDebugUnitTest
gradlew.bat lint
```

The first Gradle run needs network access to download the Gradle distribution
and AndroidX dependencies. The installed APK itself is fully offline.

## Generation notes

The board is a completely filled lattice. There are no free lattice points.

1. A Hamiltonian path is built through every lattice point exactly once.
2. The path is randomized with backbite moves while its open end stays on the
   boundary and points outwards.
3. The path is split into arrows at straight-through points. The next arrow is
   then directly in front of the arrowhead, so the arrow is blocked.
4. For the K open arrows, K-1 additional cuts are placed at outward-facing
   boundary points. The randomized path end is the final open arrow.
5. Because every cut is chosen so that the forward ray cannot hit an earlier
   arrow, the blocking graph is acyclic.
6. `PathValidator` checks the stroke geometry, the acyclic graph, the initial
   open count and a full clearing simulation before the level is returned.

Coverage is 100%. The only visual breaks are the one-cell gaps between
neighbouring arrow strokes at their cut points.

The forward ray is defined as the ray starting at the arrowhead tip and
continuing in the direction of the final stroke segment to the board edge. If
another arrow stroke intersects that ray, the arrow is blocked.

Campaign arrow targets grow smoothly from 34 arrows (level 1) to 150 arrows
(level 100); the board size grows from 3x3 to 8x8 along the way. Custom mode
uses exactly the selected arrow count. The `OPEN` count in the game screen is
always based on the actual generated board.

No level starts with a single open arrow. The finale keeps 3 open arrows on
purpose: one open arrow among ~150 is close to guessing, while 3 keeps the
board tangled but solvable by reasoning.

Campaign boards use a deterministic seed per level. Custom boards are generated
with a fresh random seed.
## Reference fixtures

Three illustrative JSON files are included:

```text
app/src/main/assets/sample_levels/level_01.json
app/src/main/assets/sample_levels/level_02.json
app/src/main/assets/sample_levels/level_03.json
```

They document the serialisable shape (layout size, arrow routes, final
directions, configuration). The runtime generator is the source of truth for
actual gameplay.

## Persistence

- `ScoreRepository`: global best, per-level bests, custom-mode best, highest
  unlocked level
- `SettingsRepository`: failure limit, Night Mode, sound, haptics
- Reset Progress clears scores and progression but keeps settings

## Permissions

The app requests no runtime permissions and no INTERNET permission. There is no
analytics SDK and no ad SDK. The only manifest permission is the signature-level
`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` added automatically by AndroidX for
safe internal broadcast receiver registration.
