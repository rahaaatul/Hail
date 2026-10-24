// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    configurations.classpath {
        resolutionStrategy {
            // Force patched versions of AGP buildscript-classpath transitives
            // flagged by Dependabot (#22-#29). Build-time only — not packaged
            // in the APK — but the graph still reports them.
            force("org.apache.commons:commons-lang3:3.21.0")
            force("org.bouncycastle:bcprov-jdk18on:1.79")
            force("org.bouncycastle:bcpkix-jdk18on:1.79")
            force("org.bitbucket.b_c:jose4j:0.9.2")
            force("org.jdom:jdom2:2.0.6.1")
            force("org.apache.httpcomponents:httpclient:4.5.16")
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
}
