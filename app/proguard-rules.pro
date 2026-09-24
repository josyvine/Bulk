# ===================================================================
# 1. Anti-Debugging & Metadata Stripping (Anti-Zip Decompilation)
# ===================================================================

# Strip all Android Logcat calls (d, v, i, w, e, println) from release bytecode
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** println(...);
}

# Remove debugging source file names, line number tables, and local variables
-renamesourcefileattribute ''
-keepattributes !SourceFile,!LineNumberTable,!LocalVariableTable,!LocalVariableTypeTable,!MethodParameters

# Preserve required annotations and reflection signatures
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ===================================================================
# 2. In-Memory Loader & Stub Application Protection
# ===================================================================

# Keep StubApplication intact so Android can launch the shell
-keep class com.bulk.app.StubApplication {
    public <init>();
    protected void attachBaseContext(android.content.Context);
    *;
}

# Preserve ClassLoader reflection fields utilized by InMemoryDexClassLoader
-keepclassmembers class java.lang.ClassLoader {
    private java.lang.ClassLoader parent;
    *;
}

# Preserve standard Android entry components
-keep class * extends android.app.Application
-keep class * extends android.app.Activity
-keep class * extends androidx.fragment.app.Fragment

# Keep custom views and ViewHolders for safe XML inflation
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclassmembers class * extends androidx.recyclerview.widget.RecyclerView$ViewHolder {
    public <init>(...);
}

# ===================================================================
# 3. Existing App Dependencies (WorkManager, Room, Startup, Ads)
# ===================================================================

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