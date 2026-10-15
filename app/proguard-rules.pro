# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Xposed Module rules
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# Aggressive size optimization rules
# Remove unused resources
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.* <init>(...);
}

# Preserve essential annotations that might be used via reflection
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Keep native methods
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep enum values and class methods
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable creators
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep Application subclasses
-keep public class * extends android.app.Application

# Keep Activity subclasses
-keep public class * extends android.app.Activity

# Keep Service subclasses
-keep public class * extends android.app.Service

# Keep BroadcastReceiver subclasses
-keep public class * extends android.content.BroadcastReceiver

# Keep ContentProvider subclasses
-keep public class * extends android.content.ContentProvider

# Keep custom View subclasses
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public <init>(android.content.Context, android.util.AttributeSet, int, int);
}

# Keep custom ViewModel subclasses
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep custom Repository subclasses
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}

# Keep custom data classes used with Gson/Json
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Suppress specific warnings that are safe to ignore
-dontwarn kotlinx.coroutines.internal.*
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.kotlinx.**
-dontwarn kotlinx.serialization.**
