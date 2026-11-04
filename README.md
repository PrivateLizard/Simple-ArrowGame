# Simple Arrow Puzzle

A fully offline line-art arrow puzzle for Android 7.0+ (API 24). Clear every
arrow. An arrow can only be tapped when no other arrow lies on the ray in front
of its arrowhead. Every board is generated and validated on-device.

**Download:** see the [GitHub Releases](https://github.com/PrivateLizard/Simple-ArrowGame/releases)
page for the signed APK.

## Rules

- Tap an open arrow to remove it; tapping a blocked arrow counts as a failure.
- Every arrow must be removed to complete a level.
- Several arrows can be open at once, so a level never locks up.
- Optional failure limits and time limits end a run.
- Pinch to zoom (0.45x - 5x), drag to pan.

## Features

- **2000 campaign levels** plus a Custom Mode
- Board grows from 3x3 to 8x8 logical grid; the actual point lattice grows from
  20x20 up to 72x72
- Late levels carry more, shorter arrows (160 at level 500, 240 at level 2000)
- Dense, fully covered arrow fields with no crossing or overlapping strokes
- Short and long, heavily bent arrows
- Removed arrows unspool head-first along their path
- Deterministic campaign boards, generated and validated on-device
- Resume an unfinished level from the main menu
- Optional periodic hint (an open arrow briefly wiggles)
- Light and dark themes; sound and haptics
- Language selector with automatic device-language detection
  (English, German, French, Spanish, Italian, Portuguese, Russian, Chinese,
  Japanese)
- No internet permission, no ads, no tracking, no third-party runtime libraries

## How levels are generated

Campaign levels use a fixed seed (`1_000_003 * levelId`), so a level always
looks the same on every device.

1. The board is filled with a **Hamiltonian path**: every lattice point is
   visited exactly once. The path is built from 2x2 block cycles that are merged
   along a random spanning tree, then broken at an outward-facing edge.
2. The path is cut into arrows. Cuts that point off the board produce the
   initially open arrows; the remaining cuts are chosen so their forward rays
   only hit later parts of the path.
3. Runs of very short, same-direction arrows are merged into one longer arrow.
   The arrow count is kept stable by splitting the longest remaining arrows at
   safe (forward-facing) cut points.
4. About 40% of the blocked arrows are reversed, so parallel rows do not all
   point the same way.
5. Arrows longer than 80 cells are split where the real blocking graph stays
   acyclic and the open count is unchanged (a soft cap: where no safe split
   exists, an arrow stays longer). A merge elsewhere keeps the arrow count.
6. Before a level is shown, the generator validates:
   - no overlapping or self-intersecting arrow strokes
   - every arrow has a valid cardinal direction
   - no dependency cycle
   - the requested number of open arrows
   - the board is fully clearable (removing an arrow never blocks another, so
     an acyclic board can always be cleared, in any order of open taps)

The blocking relation is computed once per board with a per-cell index, so a
level of 240 arrows on a 72x72 lattice generates in a few tens of milliseconds
on a desktop JVM.

If a random layout cannot be cut safely, the generator falls back to a plain
serpentine path for that level. The fallback is correct, just less irregular.

## Tech stack

- Kotlin 1.9.24, Jetpack Compose (Material 3), Compose Navigation
- MVVM with StateFlow, manual DI (`AppContainer`)
- SharedPreferences persistence
- AGP 8.13.2 / Gradle 8.14.6, compileSdk / targetSdk 36, minSdk 24

## Build

Use JDK 17+ (the Android Studio JBR works) and the Android SDK.

```bash
# Windows
gradlew.bat assembleRelease bundleRelease

# macOS / Linux
./gradlew assembleRelease bundleRelease
```

Release builds are shrunk with R8 (code and resources), so the APK is about
1 MB. Outputs, both unsigned:

- APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- App Bundle (AAB, for app stores): `app/build/outputs/bundle/release/app-release.aab`

Sign them with your own keystore (`apksigner` for the APK, `jarsigner` for the
AAB) before installing or uploading. The repository does **not** contain a
release keystore or signing password.

Tests and lint:

```bash
gradlew.bat testDebugUnitTest
gradlew.bat lint
```

The first Gradle run needs network access for Gradle/AndroidX; the installed app
itself is fully offline.

### Versioning

`versionCode` is set in `app/build.gradle.kts` and shown as the build number
on the Settings screen. Increase it by one for every release; app stores reject
an update whose `versionCode` is not higher than the installed one. (Up to
build 39 it was derived from the git commit count.) `versionName` is `1.0`.

## Privacy

The app runs fully offline and does not collect or transmit any data. See
[PRIVACY.md](PRIVACY.md).

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## AI assistance & disclaimer

This project was built with the help of AI coding assistants (large language
models). The code, the level generator and the UI were reviewed and tested, but
**no guarantee** is given for correctness, completeness or fitness for any
purpose.

The software is provided **"as is"**, without warranty of any kind. Use it at
your own risk. The author accepts no liability for any damage, data loss or
other issues resulting from its use.

## License

Source-available under the
[PolyForm Noncommercial License 1.0.0](https://polyformproject.org/licenses/noncommercial/1.0.0).

You may use, study, modify and share the source for **noncommercial** purposes.
**Commercial use is not permitted.** See [LICENSE](LICENSE) for the full terms.
This is not an OSI-approved "open source" license; the copyright stays with the
author.
