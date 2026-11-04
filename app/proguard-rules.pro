# R8 rules for the release build.
#
# Compose, AndroidX and Kotlin ship their own consumer rules. The app itself
# uses no reflection, except enum names: ComplexityLevel / DefeatReason are
# stored in SharedPreferences and navigation arguments via `name` and read back
# with `valueOf`. Keep enum constants unchanged so saved data stays readable.
-keepclassmembers enum com.arrowpuzzle.game.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Readable stack traces in crash reports (line numbers, no source file names).
-keepattributes LineNumberTable
-renamesourcefileattribute SourceFile
