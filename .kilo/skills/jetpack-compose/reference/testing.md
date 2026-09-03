## Android skills

[View on GitHub](https://github.com/android/skills/tree/main/testing/testing-setup)

### Android testing setup

Use an Android skill to analyze and set up test infrastructure, test harnesses, unit tests, and UI tests for native Android apps. To install the skill from the [Android CLI](https://developer.android.com/tools/agents/android-cli), run:

    android skills add testing-setup

<br />

[Video](https://www.youtube.com/watch?v=JyUJZvJ-OV8)

Test your app's UI to verify that behavior of your Compose code is
correct. This lets you catch errors early and improve the quality of your app.

Compose provides a set of testing APIs to find elements, verify their
attributes, and perform user actions. The APIs also include advanced features
such as time manipulation. Use these APIs to create robust tests that verify
your app's behavior.

> [!IMPORTANT]
> **Important:** For more on testing Android apps in general, including testing `View` elements, see the [general testing section](https://developer.android.com/training/testing). A good place to start is the [Fundamentals of testing Android apps](https://developer.android.com/training/testing/fundamentals) guide.

## Views

If you are working with views instead of Compose, see the general [Test apps on
Android](https://developer.android.com/training/testing) section.

In particular, a good place to start is the [Automate UI tests](https://developer.android.com/training/testing/ui-tests) guide. It
lays out how you can automate tests that run on-device, including when using
views.

## Key Concepts

The following are some key concepts for testing your Compose code:

- **[Semantics](https://developer.android.com/develop/ui/compose/testing/semantics)**: Semantics give meaning to your UI, allowing tests to interact with specific elements.
- **[Testing APIs](https://developer.android.com/develop/ui/compose/testing/apis)**: Testing APIs let you find elements, verify their attributes, and perform user actions.
- **[Synchronization](https://developer.android.com/develop/ui/compose/testing/synchronization)**: Synchronization verifies that tests wait for the UI to be idle before performing actions or making assertions.
- **[Interoperability](https://developer.android.com/develop/ui/compose/testing/interoperability)**: Interoperability enables tests to work with both Compose and View-based elements in the same app.

## Testing cheatsheet

See the [testing cheatsheet](https://developer.android.com/develop/ui/compose/testing/testing-cheatsheet) for an overview of all the key topics you should
learn about testing in Compose.

## Setup

Set up your app to let you test Compose code.

First, add the following dependencies to the `build.gradle` file of the module
containing your UI tests:

    // Test rules and transitive dependencies:
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$compose_version")
    // Needed for createComposeRule(), but not for createAndroidComposeRule<YourActivity>():
    debugImplementation("androidx.compose.ui:ui-test-manifest:$compose_version")

This module includes a [`ComposeTestRule`](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/junit4/ComposeTestRule) and an implementation for Android
called [`AndroidComposeTestRule`](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/junit4/AndroidComposeTestRule). Through this rule you can set Compose
content or access the activity. You construct the rules using factory functions,
either [`createComposeRule`](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/junit4/package-summary#createComposeRule()) or, if you need access to an activity,
[`createAndroidComposeRule`](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/junit4/package-summary#createAndroidComposeRule()). A typical UI test for Compose looks like this:

    // file: app/src/androidTest/java/com/package/MyComposeTest.kt

    class MyComposeTest {

        @get:Rule val composeTestRule = createComposeRule()
        // use createAndroidComposeRule<YourActivity>() if you need access to
        // an activity

        @Test
        fun myTest() {
            // Start the app
            composeTestRule.setContent {
                MyAppTheme {
                    MainScreen(uiState = fakeUiState, /*...*/)
                }
            }

            composeTestRule.onNodeWithText("Continue").performClick()

            composeTestRule.onNodeWithText("Welcome").assertIsDisplayed()
        }
    }

## Additional Resources

- **[Test apps on Android](https://developer.android.com/training/testing)**: The main Android testing landing page provides a broader view of testing fundamentals and techniques.
- **[Fundamentals of testing](https://developer.android.com/training/testing/fundamentals):** Learn more about the core concepts behind testing an Android app.
- **[Local tests](https://developer.android.com/training/testing/local-tests):** You can run some tests locally, on your own workstation.
- **[Instrumented tests](https://developer.android.com/training/testing/instrumented-tests):** It is good practice to also run instrumented tests. That is, tests that run directly on-device.
- **[Continuous integration](https://developer.android.com/training/testing/continuous-integration):** Continuous integration lets you integrate your tests into your deployment pipeline.
- **[Test different screen sizes](https://developer.android.com/training/testing/different-screens):** With some many devices available to users, you should test for different screen sizes.
- **[Espresso](https://developer.android.com/training/testing/espresso)**: While intended for View-based UIs, Espresso knowledge can still be helpful for some aspects of Compose testing.

## Codelab

To learn more, try the [Jetpack Compose Testing codelab](https://developer.android.com/codelabs/jetpack-compose-testing).

### Samples

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Semantics in Compose](https://developer.android.com/develop/ui/compose/semantics)
- [Window insets in Compose](https://developer.android.com/develop/ui/compose/layouts/insets)
- [Other considerations](https://developer.android.com/develop/ui/compose/migrate/other-considerations)