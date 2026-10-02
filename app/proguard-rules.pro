# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html
# Project specific ProGuard rules.

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
# Keep generic type signatures and runtime annotations used by Gson.
-keepattributes Signature
-keepattributes *Annotation*

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable
# Keep Gson core and its TypeToken subclasses used for reflective generic parsing.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# Kanji API responses are deserialized by Gson through reflection. These classes are
# in data.remote (not data.model), so they must keep their JSON field names in release.
-keep class com.app.kanjistudy.data.remote.KanjiDto { *; }

# Retrofit reads this interface's HTTP annotations at runtime.
-keep interface com.app.kanjistudy.data.remote.KanjiApiService { *; }

# Keep Room database, DAO and type converters metadata/classes.
-keep class com.app.kanjistudy.data.local.AppDatabase { *; }
-keep class com.app.kanjistudy.data.local.Converters { *; }
-keep class com.app.kanjistudy.data.local.KanjiDao { *; }

# Keep Room generated implementation classes.
-keep class * extends androidx.room.RoomDatabase
-keep class **_Impl { *; }
