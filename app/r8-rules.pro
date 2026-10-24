# Add project specific R8 rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   https://developer.android.com/build/shrink-code
#   https://developer.android.com/build/configure-code-shrinking

# Hail is an Xposed module
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# Android framework components instantiated by the system via reflection
# Declared in AndroidManifest.xml: HailApp, MainActivity, ApiActivity,
# DeviceAdminReceiver, ScreenOffReceiver, UnsuspendedReceiver,
# QSTileService, AutoFreezeService
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# Custom View subclasses inflated from XML layouts
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public <init>(android.content.Context, android.util.AttributeSet, int, int);
}

# ViewModel subclasses instantiated by ViewModelStore / SavedStateHandle
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Room entities instantiated by Room at runtime via their constructors
-keep class com.aistra.hail.utils.ActionEntity { *; }
-keep class com.aistra.hail.utils.AppMetadataEntity { *; }
-keep class com.aistra.hail.utils.ActionDependencyEntity { *; }
-keep class com.aistra.hail.utils.AppMetadataDatabase { *; }

# WorkManager Worker subclasses instantiated by WorkManager
-keepclassmembers class * extends androidx.work.Worker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Enums - keep values() and valueOf() for reflection-based access
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Native methods - keep classes that declare native methods
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Preserve annotations retained at runtime and accessed via reflection
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepattributes SourceFile,LineNumberTable

# Keep annotation constructors used via reflection (e.g. @Inject)
-keepclassmembers class * {
    @androidx.annotation.* <init>(...);
}

# Suppress warnings that are safe to ignore
-dontwarn kotlinx.coroutines.internal.*
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.kotlinx.**
