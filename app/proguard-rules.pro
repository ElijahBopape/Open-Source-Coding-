# R8 rules for the release build (minifyEnabled true).
# https://developer.android.com/build/shrink-code (Android Developers, 2026)

# Gson maps JSON to these classes by field name via reflection, so their fields
# must keep their names. (Retrofit, OkHttp, Room, WorkManager and Firebase ship
# their own consumer rules.)
-keep class com.bopape.studentassistant.data.model.** { *; }
-keepclassmembers class com.bopape.studentassistant.data.local.** { *; }

# Keep generic signatures so Gson/Retrofit can read List<Task> etc.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep line numbers so crash reports from the release build are readable.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
