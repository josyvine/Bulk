# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep WorkManager and its internal classes
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Keep Room classes and generated database implementations
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**

# Keep Android App Startup classes
-keep class androidx.startup.** { *; }
-dontwarn androidx.startup.**

# Keep Google Play Services Ads classes
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Keep common annotations and reflection signatures
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
