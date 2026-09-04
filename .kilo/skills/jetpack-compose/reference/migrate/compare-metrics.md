Jetpack Compose accelerates UI development and [improves Android
development](https://developer.android.com/develop/ui/compose/why-adopt). However, take into consideration how
adding Compose to an existing app can affect metrics such as an app's APK size,
build, and runtime performance.

## APK size and build times

This section goes over the impact to APK size and build time by looking at the
[Sunflower](https://github.com/android/sunflower/tree/compose_recyclerview) sample app---an app that demonstrates best practices
with migrating a View-based app to Compose.

> [!NOTE]
> **Note:** The results here are specific to the Sunflower sample app. Your metrics may vary as APK size and build time depend on several factors, such as number of modules, dependencies and versions you use, etc.

### APK size

Adding libraries to your project increases its APK size. The following results
are for the minified release APK of each project with [resource and code
shrinking enabled](https://developer.android.com/studio/build/shrink-code#shrink-resources), using R8 full mode, and measured using [APK Analyzer](https://developer.android.com/studio/debug/apk-analyzer).

|   | **Views only** | **Mixed Views and Compose** | **Compose-only** |
|---|---|---|---|
| Download size | 2,252 KB | 3,034 KB | 2,966 KB |

When first adding Compose to Sunflower, the APK size increased from 2,252 KB to
3,034 KB---a **782 KB increase**. The generated APK consisted of the UI build with
a mix of Views and Compose. This increase is to be expected as additional
dependencies were added to Sunflower.

Conversely, when Sunflower was migrated to a Compose-only app, the APK size
decreased from 3,034 KB to 2,966 KB---a **68 KB decrease** . This decrease was due
to removing unused View dependencies, such as `AppCompat` and
`ConstraintLayout`.

### Build time

Adding Compose increases the build time of your app as the Compose compiler
processes composables in your app. The following results were obtained using the
standalone [`gradle-profiler`](https://developer.android.com/build/profile-your-build#gradle-profiler) tool, which executes a build several times so
that a mean build time can be obtained for the debug build duration of
Sunflower:

    gradle-profiler --benchmark --project-dir . :app:assembleDebug

|   | **Views only** | **Mixed Views and Compose** | **Compose-only** |
|---|---|---|---|
| Mean build time | 299.47 ms | 399.09 ms | 342.16 ms |

When first adding Compose to Sunflower, the mean build time increased from 299
ms to 399 ms---a **100 ms increase**. This duration is due to the Compose compiler
performing additional tasks to transform Compose code defined in the project.

Conversely, the mean build time dropped to 342 ms, a **57 ms decrease** , when
Sunflower's migration to Compose was completed. This reduction can be attributed
to several factors which collectively reduce build time such as removing [data
binding](https://developer.android.com/topic/libraries/data-binding), migrating dependencies that use [kapt to KSP](https://developer.android.com/build/migrate-to-ksp), and updating
several dependencies to their latest versions.

### Summary

Adopting Compose will effectively increase the APK size of your app and also
increase the build time performance of your app due to the compilation process
of Compose code. These tradeoffs, however, need to be weighed against the
[benefits of Compose](https://developer.android.com/develop/ui/compose/why-adopt), especially around developer productivity increases
when adopting Compose. For example, the [Play Store team](https://android-developers.googleblog.com/2022/03/play-time-with-jetpack-compose.html) found
that **writing UI requires much less code, sometimes up to 50%**, thereby
increasing productivity and maintainability of code.

You can read more case studies in [Adopt Compose for Teams](https://developer.android.com/develop/ui/compose/adopt/for-large-teams).

## Runtime performance

This section covers topics related to runtime performance in Jetpack Compose to
help understand how Jetpack Compose compares to the View system's performance,
and how you can measure it.

### Smart recompositions

When portions of the UI are invalid, Compose tries to recompose just the
portions that need to be updated. Read more about this in the [Lifecycle of
composables](https://developer.android.com/develop/ui/compose/lifecycle) and [Jetpack Compose
phases](https://developer.android.com/develop/ui/compose/phases) documentation.

### Baseline Profiles

[Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/overview) are an
excellent way of speeding up common user journeys. Including a Baseline
Profile in your app can improve code execution speed by about 30% from the first
launch by avoiding interpretation and just-in-time (JIT) compilation steps for
included code paths.

The Jetpack Compose library includes its own Baseline Profile and you
automatically get these optimizations when you use Compose in your app. However,
these optimizations only affect code paths within the Compose library, so we
recommend that you [add a Baseline Profile](https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile) to your
app to cover code paths outside of Compose.

### Comparison with the View system

Jetpack Compose has many improvements over the View system. These improvements
are described in the following sections.

#### Everything extends View

Every `View` that draws on the screen, such as `TextView`, `Button`, or
`ImageView`, requires memory allocations, explicit state tracking, and various
callbacks to support all use cases. Furthermore, the custom `View` owner needs
to implement explicit logic to prevent redrawing when it isn't
necessary---for example, for repetitive data processing.

Jetpack Compose addresses this in a few ways. Compose doesn't have explicit
updatable objects for drawing views. UI elements are simple composable functions
whose information is written to the composition in a replayable way. This helps
cut down explicit state tracking, memory allocations, and callbacks to only the
composables that require said features instead of requiring them by all
extensions of a given `View` type.

Furthermore, Compose provides [smart recompositions](https://developer.android.com/develop/ui/compose/migrate/compare-metrics#smart-recomposition),
replaying the previously drawn result if you don't need to make changes.

#### Multiple layout passes

Traditional ViewGroups have a lot of expressiveness in their measure and layout
APIs that make them prone to multiple layout passes. These multiple layout
passes can cause exponential work if done at specific nested points in the view
hierarchy.

Jetpack Compose enforces a [single layout pass](https://developer.android.com/develop/ui/compose/layouts/custom)
for all layout composables via its API contract. This lets Compose efficiently
handle deep UI trees. If multiple measurements are needed, Compose has
[intrinsic measurements](https://developer.android.com/develop/ui/compose/layouts/intrinsic-measurements).

#### View startup performance

The View system needs to inflate XML layouts when showing a particular layout
for the first time. This cost is saved in Jetpack Compose since layouts are
written in Kotlin and compiled like the rest of your app.

### Benchmark Compose

In Jetpack Compose 1.0, there are notable differences between the performance of
an app in `debug` and `release` modes. For representative timings, **always**
use the `release` build instead of `debug` when profiling your app.

To check how your Jetpack Compose code is performing, you can use the
[Jetpack Macrobenchmark](https://developer.android.com/studio/profile/macrobenchmark) library. To learn how
to use it with Jetpack Compose, see the
[MacrobenchmarkSample project](https://github.com/android/performance-samples/tree/main/MacrobenchmarkSample).

The Jetpack Compose team also uses Macrobenchmark to catch any regressions that
can happen. For example, see the [benchmark for lazy column](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/integration-tests/macrobenchmark/src/main/java/androidx/compose/integration/macrobenchmark/TrivialListScrollBenchmark.kt)
and its [dashboard](https://androidx-perf.skia.org/e/?queries=test%3Dandroidx.benchmark.integration.macrobenchmark.SmallListStartupBenchmark_startup_startup_HOT_compilation_None_%26test%3Dandroidx.benchmark.integration.macrobenchmark.SmallListStartupBenchmark_startup_startup_WARM_compilation_None_%26test%3Dandroidx.benchmark.integration.macrobenchmark.SmallListStartupBenchmark_startup_startup_WARM_compilation_SpeedProfile_iterations_3__%26test%3Dandroidx.benchmark.integration.macrobenchmark.SmallListStartupBenchmark_startup_startup_HOT_compilation_SpeedProfile_iterations_3__%26test%3Dandroidx.benchmark.integration.macrobenchmark.SmallListStartupBenchmark_startup_startup_COLD_compilation_SpeedProfile_iterations_3__%26test%3Dandroidx.benchmark.integration.macrobenchmark.SmallListStartupBenchmark_startup_startup_COLD_compilation_None_)
to track regressions.

### Compose profile installation

Since Jetpack Compose is an unbundled library, it doesn't benefit from [Zygote](https://developer.android.com/topic/performance/memory-overview#SharingRAM) that preloads the View system's
UI Toolkit classes and drawables. Jetpack Compose 1.0 utilizes profile
installation for *release* builds. [Profile installers](https://developer.android.com/jetpack/androidx/releases/profileinstaller) let apps specify critical code to
be ahead-of-time (AOT) compiled at installation time. Compose ships profile
installation rules which reduce startup time and jank in Compose apps.

> [!NOTE]
> **Note:** Compose profiles aren't available on Android L or M, since the app is compiled AOT by Android Framework on devices running these versions. The [Making apps blazing fast with Baseline Profiles
> video](https://www.youtube.com/watch?v=yJm5On5Gp4c) explains this further.

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Other considerations](https://developer.android.com/develop/ui/compose/migrate/other-considerations)
- [Using Compose in Views](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/compose-in-views)
- [Scroll](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/scroll)