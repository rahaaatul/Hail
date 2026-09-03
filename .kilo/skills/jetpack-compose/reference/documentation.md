Jetpack Compose is the modern toolkit for building Android UI, simplifying the
development of apps that adapt to any display size.

- [Overview](https://developer.android.com/develop/ui/compose): See the resources available to Compose developers.
- [Tutorial](https://developer.android.com/develop/ui/compose/tutorial): Get started with Compose by building a basic UI.
- [Quick Guides](https://developer.android.com/quick-guides): Try out our fast and focused guides, designed to get you to your goal as quickly as possible.

## Foundation

- [Thinking in Compose](https://developer.android.com/develop/ui/compose/mental-model): How the declarative approach of Compose is different from the view-based approach you may have used in the past. Build a mental model of working with Compose.
- [Managing state](https://developer.android.com/develop/ui/compose/state): Setting and using state in your Compose app.
- [Lifecycle of composables](https://developer.android.com/develop/ui/compose/lifecycle): Lifecycle of a composable, and how Compose determines whether it needs to be redrawn.
- [Modifiers](https://developer.android.com/develop/ui/compose/modifiers): Use modifiers to augment or decorate your composables.
- [Side-effects in Compose](https://developer.android.com/develop/ui/compose/side-effects): Ways to manage side-effects.
- [Jetpack Compose Phases](https://developer.android.com/develop/ui/compose/phases): The steps Compose goes through to render your app's UI, and how to use that information to write efficient code.
- [Architectural layering](https://developer.android.com/develop/ui/compose/layering): The architectural layers that make up Jetpack Compose and the core principles that informed the design of Compose.
- [Performance](https://developer.android.com/develop/ui/compose/performance): Avoid the common programming pitfalls that can degrade app performance.
- [Semantics in Compose](https://developer.android.com/develop/ui/compose/semantics): The semantics tree, which organizes your UI in a way that can be used by accessibility services and testing frameworks.
- [Locally scoped data with CompositionLocal](https://developer.android.com/develop/ui/compose/compositionlocal): Use `CompositionLocal` to pass data through the composition.

## Adaptive UI

- [Build adaptive apps](https://developer.android.com/develop/adaptive-apps): Learn the core principles of creating layouts optimized for any display size, including phones, tablets, foldables, and more.
- [Apply proven layouts](https://developer.android.com/develop/adaptive-apps/guides/canonical-layouts): Use canonical layouts like list-detail and supporting pane for optimized apps on large screens.
- [Adaptive navigation](https://developer.android.com/guide/topics/large-screens/navigation-for-responsive-uis): Implement navigation patterns that automatically adjust to the available display space.

## Development environment

- [Android Studio with Compose](https://developer.android.com/develop/ui/compose/setup): How to set up your development environment to use Compose.
- [Tooling for Compose](https://developer.android.com/develop/ui/compose/tooling): Android Studio's new features to support Compose.
- [Kotlin for Compose](https://developer.android.com/develop/ui/compose/kotlin): Kotlin-specific idioms work with Compose.
- [Compare Compose and view metrics](https://developer.android.com/develop/ui/compose/migrate/compare-metrics): How migrating to Compose can affect your app's APK size and runtime performance.
- [Bill of Materials](https://developer.android.com/develop/ui/compose/bom): Manage all your Compose dependencies by specifying only the BOM's version.

## Design

- [Layouts](https://developer.android.com/develop/ui/compose/layouts): Compose layout components and how to design your own.
  - [Layout basics](https://developer.android.com/develop/ui/compose/layouts/basics): The building blocks for a straightforward app UI.
  - [Material Components and layouts](https://developer.android.com/develop/ui/compose/components): Material components and layouts in Compose.
  - [Custom layouts](https://developer.android.com/develop/ui/compose/layouts/custom): Take control of your app's layout and design a custom layout of your own.
  - [Alignment lines](https://developer.android.com/develop/ui/compose/layouts/alignment-lines): Create custom alignment guides to precisely align and position your UI elements.
  - [Intrinsic measurements](https://developer.android.com/develop/ui/compose/layouts/intrinsic-measurements): How to query for information about child elements before measuring them because Compose measures UI elements only once per pass.
  - [ConstraintLayout](https://developer.android.com/develop/ui/compose/layouts/constraintlayout): Use `ConstraintLayout` in your Compose UI.
- [Design Systems](https://developer.android.com/develop/ui/compose/designsystems): Implement a design system and give your app a consistent look and feel.
  - [Material Design 3](https://developer.android.com/develop/ui/compose/designsystems/material3): Implement Material You with the Compose implementation of [Material Design 3](https://m3.material.io/).
  - [Migrating from Material 2 to Material 3](https://developer.android.com/develop/ui/compose/designsystems/material2-material3): Migrate your app from Material Design 2 to Material Design 3 in Compose.
  - [Material Design 2](https://developer.android.com/develop/ui/compose/designsystems/material): Customize the Compose implementation of [Material Design 2](https://material.io/) to fit your product's brand.
  - [Custom design systems](https://developer.android.com/develop/ui/compose/designsystems/custom): Implement a custom design system in Compose and adapt existing Material Design composables for the new design system.
  - [Anatomy of a theme](https://developer.android.com/develop/ui/compose/designsystems/anatomy): Lower-level constructs and APIs used by `MaterialTheme` and custom design systems.
- [Lists and grids](https://developer.android.com/develop/ui/compose/lists): Compose options for managing and displaying lists and grids of data.
- [Text](https://developer.android.com/develop/ui/compose/text): Main options in Compose for displaying and editing text.
- [Graphics](https://developer.android.com/develop/ui/compose/graphics): Compose features for building and working with custom graphics.
- [Animation](https://developer.android.com/develop/ui/compose/animation/introduction): Compose options for animating your UI elements.
- [Gestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input): Build a Compose UI that detects and interacts with user gestures.
- [Handling user interactions](https://developer.android.com/develop/ui/compose/touch-input/user-interactions/handling-interactions): How Compose abstracts low-level input into higher-level interactions so you can customize how your components respond to user actions.

## Adopting Compose

- [Migrate view-based apps](https://developer.android.com/develop/ui/compose/migrate): Migrate your view-based app to Compose.
  - [Migration strategy](https://developer.android.com/develop/ui/compose/migrate/strategy): How to safely and incrementally introduce Compose into your codebase.
  - [Interoperability APIs](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis): Compose APIs to help you combine Compose with a view-based UI.
  - [Other considerations](https://developer.android.com/develop/ui/compose/migrate/other-considerations): Theming, architecture, testing, and other considerations while migrating your view-based app to Compose.
- [Compose and other libraries](https://developer.android.com/develop/ui/compose/libraries): How to use view-based libraries in your Compose content.
- [Compose architecture](https://developer.android.com/develop/ui/compose/architecture): Implement the unidirectional flow pattern in Compose, implement events and state holders, and work with `ViewModel` in Compose.
- [Navigation](https://developer.android.com/guide/navigation#framework-options): Use `NavController` to integrate the Navigation component with your Compose UI.
- [Resources](https://developer.android.com/develop/ui/compose/resources): Work with your app's resources in your Compose code.
- [Accessibility](https://developer.android.com/develop/ui/compose/accessibility): Accommodate users with accessibility requirements.
- [Testing](https://developer.android.com/develop/ui/compose/testing): Test your Compose code.
  - [Testing cheat sheet](https://developer.android.com/develop/ui/compose/testing-cheatsheet): A quick reference of useful Compose testing APIs.

## Additional resources

- [Get setup](https://developer.android.com/develop/ui/compose/setup)
- [Curated learning pathway](https://developer.android.com/courses/pathways/compose)
- [Compose API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md)
- [API reference](https://developer.android.com/reference/kotlin/androidx/compose)
- [Codelabs](https://goo.gle/compose-codelabs)
- [Sample apps](https://github.com/android/compose-samples)
- [Videos](https://www.youtube.com/user/androiddevelopers/search?query=%23jetpackcompose)

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Locally scoped data with CompositionLocal](https://developer.android.com/develop/ui/compose/compositionlocal)
- [Other considerations](https://developer.android.com/develop/ui/compose/migrate/other-considerations)
- [Anatomy of a theme in Compose](https://developer.android.com/develop/ui/compose/designsystems/anatomy)