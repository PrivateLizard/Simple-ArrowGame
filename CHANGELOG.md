# Changelog

All notable changes to Simple Arrow Puzzle. `versionName` is `1.0`; the build
number is the git commit count and is shown on the Settings screen.

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
