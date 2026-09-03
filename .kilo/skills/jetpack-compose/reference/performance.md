Jetpack Compose delivers excellent performance out of the box. Configure your
app using best practices to avoid common pitfalls and optimize your Compose
application's performance.

## Benchmarking Jetpack Compose

To validate Jetpack Compose performance against Views, we use hero
benchmarks---benchmarks that focus on important, real-world user journeys, like
cold starting an app or scrolling a list or grid with images. We conduct these
benchmarks using the open-source Pokedex app, comparing its [Views](https://github.com/skydoves/pokedex) and
[Compose](https://github.com/skydoves/pokedex-compose) implementations.

These benchmarks show that Compose 1.9 and later match the Views performance for
jank while scrolling.
![Since Compose 1.9.0, Compose and Views have the same jank rate](https://developer.android.com/static/develop/ui/compose/performance/images/jank.svg) Since Compose 1.9.0, Compose and Views have the same jank rate.

For additional metrics and to learn about our methodologies, see [Hero
benchmarks](https://developer.android.com/develop/ui/compose/performance/herobenchmark).

## Key concepts

These are some of the key concepts for performance in Compose:

- **[Phases](https://developer.android.com/develop/ui/compose/performance/phases):** Understanding the composition, layout, and drawing phases is crucial for optimizing how Compose updates your UI.
- **[Baseline Profiles](https://developer.android.com/develop/ui/compose/performance/baseline-profiles):** These profiles precompile essential code, leading to faster app launches and smoother interactions.
- **[Stability](https://developer.android.com/develop/ui/compose/performance/stability):** Increase the stability of your app to more efficiently skip unnecessary recompositions, improving performance.

## Properly configure your app

If your app is performing poorly, there might be a configuration problem. A good
first step is to check the following configuration options:

- **Build in Release Mode with R8:** Try running your app in [release
  mode](https://developer.android.com/studio/run#changing-variant). Debug mode is useful for spotting many problems, but it imposes a performance cost and can make it hard to spot other issues. You should also [enable optimizing and shrinking](https://developer.android.com/build/shrink-code#enable) with the R8 compiler to ensure a performant and efficient release build.
- **Use Baseline Profiles:** Baseline Profiles improve performance by precompiling code for critical user journeys. Compose includes a default profile, but ideally, you should create an app-specific one as well. [Learn
  more about Baseline Profiles in the general Android performance docs](https://developer.android.com/topic/performance/baselineprofiles/overview)

## Tools

Familiarize yourself with the suite of [tools](https://developer.android.com/develop/ui/compose/performance/tooling) available to help you measure
and analyze your Compose app's performance.

## Best Practices

When developing your app with Compose, keep these best practices in mind:

- **[Avoid expensive calculations](https://developer.android.com/develop/ui/compose/performance/bestpractices#use-remember):** Use `remember` to cache the results of expensive calculations.
- **[Help lazy layouts](https://developer.android.com/develop/ui/compose/performance/bestpractices#use-lazylist-keys):** Provide stable keys to lazy layouts using the `key` parameter to minimize unnecessary recompositions.
- **[Limit unnecessary recompositions](https://developer.android.com/develop/ui/compose/performance/bestpractices#use-derivedstateof):** Use `derivedStateOf` to limit recompositions when rapidly changing state.
- **[Defer state reads](https://developer.android.com/develop/ui/compose/performance/bestpractices#defer-reads):** Defer state reads as long as possible by wrapping them in lambda functions.
- **[Use lambda modifiers for changing state](https://developer.android.com/develop/ui/compose/performance/bestpractices#defer-reads):** Use lambda-based modifiers like `Modifier.offset { ... }` for frequently changing state variables.
- **[Avoid backwards writes](https://developer.android.com/develop/ui/compose/performance/bestpractices#avoid-backwards):** Never write to state that has already been read in a composable.

For more details, see the [best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices) guide.

## Views

If you are working with views instead of Compose, see the dedicated [Improve
layout performance](https://developer.android.com/develop/ui/views/layout/improving-layouts) guide.

## Additional resources

- **[App performance guide](https://developer.android.com/topic/performance/overview):** Discover best practices, libraries, and tools to improve performance on Android.
- **[Inspect Performance](https://developer.android.com/topic/performance/inspecting-overview):** Inspect app performance.
- **[Benchmarking](https://developer.android.com/topic/performance/benchmarking/benchmarking-overview):** Benchmark app performance.
- **[App startup](https://developer.android.com/topic/performance/appstartup/analysis-optimization):** Optimize app startup.
- **[Baseline profiles](https://developer.android.com/baseline-profiles):** Understand baseline profiles.