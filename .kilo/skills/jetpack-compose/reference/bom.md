The Compose Bill of Materials (BOM) lets you manage all of your Compose library
versions by specifying only the BOM's version. The BOM itself has links to the
stable versions of the different Compose libraries, in such a way that they work
well together. When using the BOM in your app, you don't need to add any
version to the Compose library dependencies themselves. When you update the BOM
version, all the libraries that you're using are automatically updated to their
new versions.

### Kotlin

```kotlin
dependencies {
    // Specify the Compose BOM with a version definition
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)

    // Specify Compose library dependencies without a version definition
    implementation("androidx.compose.foundation:foundation")
    // ..
    testImplementation("androidx.compose.ui:ui-test-junit4")
    // ..
    androidTestImplementation("androidx.compose.ui:ui-test")
}
```

### Groovy

```groovy
dependencies {
    // Specify the Compose BOM with a version definition
    Dependency composeBom = platform('androidx.compose:compose-bom:2026.08.00')
    implementation composeBom
    testImplementation composeBom
    androidTestImplementation composeBom

    // Specify Compose library dependencies without a version definition
    implementation 'androidx.compose.foundation:foundation'
    // ..
    testImplementation 'androidx.compose.ui:ui-test-junit4'
    // ..
    androidTestImplementation 'androidx.compose.ui:ui-test'
}
```

To find out which Compose library versions are mapped to a specific BOM version,
check out the [BOM to library version mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping).

## Why isn't the Compose Compiler library included in the BOM?

The Compose Kotlin compiler extension (androidx.compose.compiler) is not linked
to the Compose library versions. Instead, it is linked to versions of the Kotlin
compiler plugin and released in a separate cadence from the rest of Compose.

As of Kotlin 2.0, the Compose compiler is managed alongside the Kotlin
compiler and uses the same version as the Kotlin compiler.
See [Compose Compiler Gradle plugin](https://developer.android.com/develop/ui/compose/compiler) for
configuration details.

In versions lower than Kotlin 2.0, consult the [Compose to Kotlin Compatibility
Map](https://developer.android.com/jetpack/androidx/releases/compose-kotlin) to identify a compiler version that's compatible
with your Kotlin version and see [Compose Compiler](https://developer.android.com/jetpack/androidx/releases/compose-compiler) for
guidance on configuring it.

## How do I use a different library version than what's designated in the BOM?

In the `build.gradle` dependencies section, keep the import of the BOM
platform. On the library dependency import, specify the overriding version. For
example, here's how to declare dependencies if you want to use a newer version
of the animation library, no matter what version is designated in the BOM:

### Kotlin

```kotlin
dependencies {
    // Specify the Compose BOM with a version definition
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)

    // Override the BOM version when needed
    implementation("androidx.compose.animation:animation:1.12.0-beta01")

    // ..
}
```

### Groovy

```groovy
dependencies {
    // Specify the Compose BOM with a version definition
    Dependency composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation composeBom

    // Override the BOM version when needed
    implementation 'androidx.compose.animation:animation:1.12.0-beta01'

    // ..
}
```

> [!NOTE]
> **Note:** Overriding the BOM to use an alpha version of a Compose library will update your build to use the required dependencies of that alpha library (which in turn, could be alpha).

## Does the BOM automatically add all the Compose libraries to my app?

No. To actually add and use Compose libraries in your app, you must declare each
library as a separate dependency line in your module (app-level) Gradle file
(usually app/build.gradle).

Using the BOM helps verify that the versions of any Compose libraries in your
app are compatible, but the BOM doesn't actually add those Compose libraries to
your app.

## Why is the BOM the recommended way to manage Compose library versions?

Going forward, Compose libraries will be versioned independently, which means
version numbers will start to be incremented at their own pace. The latest
stable releases of each library are tested together. However, finding the
latest stable versions of each library can be difficult, and the BOM helps you
to automatically use these latest versions.

## Am I forced to use the BOM?

No. You can still choose to add each dependency version manually. However, we
recommend using the BOM as it will make it easier to use all of the latest
stable versions at the same time.

## Does the BOM work with version catalogs?

Yes. You can include the BOM itself in the version catalog, and omit the other
Compose library versions:

    [libraries]
    androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "androidxComposeBom" }
    androidx-compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }

Don't forget to import the BOM in your module's `build.gradle`:

### Kotlin

```kotlin
dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // import Compose dependencies as usual
}
```

### Groovy

```groovy
dependencies {
    Dependency composeBom = platform(libs.androidx.compose.bom)
    implementation composeBom
    androidTestImplementation(composeBom)

    // import Compose dependencies as usual
}
```

## What if I want to try out alpha or beta releases of Compose libraries?

There are three available Compose BOMs. Each BOM is a point-in-time snapshot
of the latest-available versions of Compose libraries.

- Stable - contains latest stable versions of each library.
- Beta - contains latest beta, release candidate (RC), or stable versions of each library.
- Alpha - contains latest alpha, beta, RC, or stable versions of each library.

The Alpha and Beta versions of the BOM are specified by adding `-alpha` and
`-beta` to the BOM artifact name. The stable version has no suffix.

> [!NOTE]
> **Note:** The alpha and beta BOMs are provided for testing of upcoming features and bug fixes. They are not intended for production use. These BOMs do not *only* contain alpha and beta versions. If the latest stable version of a library is higher than the latest alpha, beta, or RC versions, that stable version will appear in the alpha and beta BOMs. If the latest version is a Beta or RC, that version will appear in the alpha and beta BOMs.

### Kotlin

```kotlin
dependencies {
    // Specify the Compose BOM with a version definition
    val composeBom = platform("androidx.compose:compose-bom-alpha:2026.08.00")
    //            or platform("androidx.compose:compose-bom-beta:2026.08.00")
    implementation(composeBom)
    // ..
}
```

### Groovy

```groovy
dependencies {
    // Specify the Compose BOM with a version definition
    Dependency composeBom = platform('androidx.compose:compose-bom-alpha:2026.08.00')
    //                   or platform('androidx.compose:compose-bom-beta:2026.08.00')
    implementation composeBom
    // ..
}
```

## How do I report an issue or offer feedback on the BOM?

You can file issues on our [issue tracker](https://issuetracker.google.com/issues/new?component=612128&template=1253476).

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Material Design 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [ConstraintLayout in Compose](https://developer.android.com/develop/ui/compose/layouts/constraintlayout)
- [Resources in Compose](https://developer.android.com/develop/ui/compose/resources)