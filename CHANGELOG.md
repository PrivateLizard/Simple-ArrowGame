# Changelog

All notable changes to Simple Arrow Puzzle. `versionName` was `1.0` up to build 39,
`1.0.1` for build 40 and is `1.1.0` from build 41. The build
number (`versionCode`) is shown on the Settings screen. Up to build 39 it was the
git commit count; from build 40 on it is set by hand in `app/build.gradle.kts`.

## 1.1.0 (Build 41) - 2026-11-05

- The resume button on the main menu always stays on one line.
- Clearer pencil, feather and envelope shapes.
- **3000 levels:** the difficulty curve is stretched, so level 3000 is as hard
  as level 2000 was. Nearly all campaign boards look different; progress and
  scores are kept, an unfinished level from an older build is discarded. Best
  scores from older builds can be higher than the new maximum of a level.
- **Picture levels:** every fifth level is cut into one of 200 shapes - geometric
  forms, nature, animals and everyday things. Each block of 1000 levels shows
  all 200 shapes once: small, then medium, then large. The board grows to make
  up for the empty space around a shape. All levels stay fully solvable.
- **Picture Test:** try any shape in any size. Only visible with the developer
  options on.
- Smoother play, with no visible change: the board no longer redraws for the
  clock or for animations, and resting arrows are drawn from cached paths.
- Redesigned level-complete screen: animated check badge, counting-up score,
  breakdown and stats revealed one after another, plus a short, subtle confetti
  shower in theme colours (skipped when system animations are off).
- **Fix:** the level-complete screen recomputed the score breakdown from the
  planned arrow count instead of the real one, so the rows did not add up to
  the total and the arrow count was off. It now uses the real count.
- Redesigned level-failed screen in the same style: animated cross badge with
  a soft shake, the reason with one tip, failures and time (the value that hit
  its limit is tinted), "try again" as the main action.
- **Fix:** the failed screen showed `1/1` instead of `1/0` with zero failures
  allowed; a time-limit loss now shows elapsed / limit.
- Arrow counts removed from the game header and from both result screens.
- Home: "Personal Best" is now "Total Points" (it always was the sum of all
  campaign best scores).
- Thousands separators follow the app language (3,195 in English, 3.195 in
  German, narrow space in French/Russian) on Home, level select and the
  level-complete screen.
- Dev options: switchable "win buttons" in levels - tap the next open arrow, or
  win the level instantly with a perfect result (counts as a normal win).
- Translations reviewed: the Complexity texts in Custom Mode now say what they
  really do (how many arrows are open at the start), Portuguese is consistently
  European Portuguese, no two shapes share a name in any language, and Chinese
  and Japanese headlines lose a stray space.

## 1.0.1 (Build 40) - 2026-11-04

- Release builds are shrunk with R8: the APK drops from about 6.2 MB to 1.1 MB.
- An Android App Bundle (AAB) for app stores is built alongside the APK.
- `versionCode` is now a fixed number in `app/build.gradle.kts` instead of the
  git commit count, so it can never decrease.
- Level select: locked levels show only their dimmed number (no "locked" text).
- Custom Mode uses the same Max Failures chips as Settings (0-20 and OFF).
- **Target SDK 36 (Android 16)**, compile SDK 36; Android Gradle Plugin 8.13.2,
  Gradle 8.14.6. Minimum stays Android 7.0 (API 24).
- Edge-to-edge: the background runs behind the transparent status and
  navigation bars, the content keeps clear of them and of display cutouts.

## Build 39 - 2026-11-04

- **Much faster level generation:** the blocking check uses a per-cell index
  instead of testing every arrow against every other one. Level 2000 now
  generates in ~25 ms on a desktop JVM (was ~1.2 s) and opens in about a
  second on the emulator (was 5-15 s). The speed-up alone produced
  bit-identical boards; only the length cap below changes them.
- **Soft arrow length cap (80 cells):** very long, self-coiled arrows are split
  wherever the board stays acyclic and the open-arrow count is unchanged; a
  merge of two short neighbours keeps the arrow count (and the score maximum)
  the same. Across all 2000 levels, arrows over 300 cells dropped from 584 to
  42 and the longest arrow from 2067 to 839 cells.
- Campaign boards therefore look different from earlier builds. Scores and
  progress are kept; an unfinished level saved by an older build is discarded
  instead of being resumed on a board whose arrows no longer match.
- French, Spanish, Italian, Portuguese, Russian, Chinese and Japanese are now
  complete. A unit test checks every table for missing keys and placeholders.
- Cleanup: unused resources and translation keys removed; backup / device
  transfer explicitly disabled. Lint: 0 warnings (was 70).

## Build 35 - 2026-11-04

- Merge runs of at least 3 very short (<= 8 cells) same-direction arrows into
  one arrow (max 80 cells).
- Keep the arrow count stable by splitting the longest remaining arrows at
  forward-facing cut points; revert the merge if the count cannot be restored.
- Never merge away an open arrow.

## Build 34 - 2026-11-04

- **Fix:** the blocking rule checks every arrow on the forward ray again. A
  removed arrow no longer lets another arrow "pass through" a gap; the open
  counter and hint match what the player sees.
- Late levels carry more, shorter arrows (160 at level 500, 240 at level 2000).
- Cuts are spread evenly over forward-facing candidates; unsafe after-the-fact
  splitting was removed.

## Build 33 - 2026-11-03

- Regression test: an arrow farther down the ray still blocks even when the
  nearest arrow is removed.

## Build 31/32 - 2026-11-03

- New path generator: a Hamiltonian cycle built from 2x2 block cycles merged
  along a random spanning tree, then broken into a path. No more long parallel
  serpentine rows.
- Layout sizes are rounded up to even numbers (20..72).
- Blocking rule fix (all arrows on the ray).

## Build 29 - 2026-11-02

- Point lattice grows by one row/column every 50 levels after level 100
  (level 2000 = 72x72).

## Build 25-27 - 2026-11-02

- Level select: header text removed, larger level numbers, score below the
  number, max score dimmed underneath.
- Completed tiles use the standard number color; score is green.
- Home: Personal Best and Progress in green.
- Dev options: number input + SET instead of a slider; no orange.
- In-game stat pills and the top-right percentage removed.

## Build 24 - 2026-11-02

- **2000 levels** (was 500). Levels 1-500 keep their curve; 501-2000 grow the
  arrow target.
- Level list only shows locked levels up to the next full hundred.
- Achieved points are larger, maximum underneath.
- Dev unlock by level number instead of a drag slider.

## Build 21-23 - 2026-11-02

- Smooth animated scroll to the current level in the level list.
- Zoom margin is a fixed 21% of the viewport and stays constant at every zoom.
- Removed the hard snap at scale 1.

## Build 20 - 2026-11-01

- 500 levels, per-level max score display, dev unlock slider, extra zoom margin.

## Earlier builds

- Resume of an unfinished level, saved on every tap and on app stop.
- Hint system (first hint after 20 s, then every 10 s).
- New launcher icon (light/dark split, connected Z arrow path).
- Multi-language UI with English fallback (EN/DE complete).
- Settings: max failures (0/1/2/3/5/10/20/AUS), night mode, sound, haptics,
  hints, language.
- Sound and haptic feedback, including the bottle-cap "plop" for a correct tap.
- Wrong-tap feedback: red flash and short shake.
- Board as a full-screen background, square cells, pinch zoom and pan.
- 1-click launcher scripts (Pinokio) for local builds.
