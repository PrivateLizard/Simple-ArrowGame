# Privacy

Simple Arrow Puzzle is designed to run fully offline.

- No `INTERNET` permission is requested.
- No ads, analytics, crash reporting or telemetry.
- No personal data is collected, stored or transmitted.
- Campaign progress, scores and settings are stored in the app's local
  SharedPreferences on the device. Uninstalling the app removes them.
- Optional backup, off by default: when you switch on "Backup" in Settings,
  the app copies your best scores, unlocked level and settings into one file
  that Android's own backup may include (your Google backup, end-to-end
  encrypted, or a phone-to-phone transfer). The app itself never sends it
  anywhere and the developer cannot see it. Switching backup off empties that
  file again. While it is off, nothing is included in any backup.
- The only bundled libraries are Android's own AndroidX/Jetpack components.
  They contain no ads or analytics.

Because the app has no network access, it cannot send data anywhere itself.
