# Arrow Puzzle

A fully offline line-art arrow puzzle for Android 7.0+ (API 24). Clear every
arrow. An arrow can only be tapped when no other arrow lies on the ray in front
of its arrowhead. Every board is generated and validated on-device.

## Rules

- Tap an open arrow to remove it; tapping a blocked arrow counts as a failure.
- Every arrow must be removed to complete a level.
- Several arrows can be open at once, so a level never locks up.
- Optional failure limits and time limits end a run.
- Pinch to zoom, drag to pan.

## Features

- 2000 campaign levels (3x3 → 8x8 boards) plus a Custom Mode
- Dense, fully covered arrow fields with no crossing strokes
- Short and very long, heavily bent arrows
- Removed arrows unspool head-first along their path
- Deterministic campaign boards, validated before display
- Resume an unfinished level from the main menu
- Optional periodic hint (an open arrow briefly wiggles)
- Light and dark themes; sound and haptics
- Language selector with automatic device-language detection
  (English/German complete, more languages with English fallback)
- No internet permission, no ads, no tracking, no third-party runtime libraries

## Tech stack

- Kotlin 1.9.24, Jetpack Compose (Material 3), Compose Navigation
- MVVM with StateFlow, manual DI (`AppContainer`)
- SharedPreferences persistence
- AGP 8.4.2 / Gradle 8.6, compileSdk 34, minSdk 24

## Build

Use JDK 17+ (the Android Studio JBR works) and the Android SDK.

```bash
# Windows
gradlew.bat assembleRelease

# macOS / Linux
./gradlew assembleRelease
```

The unsigned APK is written to
`app/build/outputs/apk/release/app-release-unsigned.apk`. Sign it with your own
keystore before installing on a device.

Tests and lint:

```bash
gradlew.bat testDebugUnitTest
gradlew.bat lint
```

The first Gradle run needs network access for Gradle/AndroidX; the installed app
itself is fully offline.

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
