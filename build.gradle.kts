// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.protobuf) apply false
}

// Force patched versions of AGP buildscript-classpath transitives
// flagged by Dependabot (#22-#33). Build-time only — not packaged
// in the APK — but the graph still reports them.
// Configured outside the buildscript block because Gradle 9+ forbids
// mutating buildscript configurations inside buildscript (error in Gradle 10).
buildscript.configurations.named("classpath").configure {
    resolutionStrategy {
        force("org.freemarker:freemarker:2.3.35")
        force("org.apache.commons:commons-lang3:3.21.0")
        force("org.bouncycastle:bcprov-jdk18on:1.86")
        force("org.bouncycastle:bcpkix-jdk18on:1.86")
        force("org.bitbucket.b_c:jose4j:0.9.7")
        force("org.jdom:jdom2:2.0.6.1")
        force("org.apache.httpcomponents:httpclient:4.5.14")
    }
}
>>>>>>> 648ccb7c (fix(security): force patched freemarker 2.3.35 on buildscript classpath to resolve Dependabot #33)
    }
}
