Tutorial

# Jetpack Compose Tutorial

- 4 lessons
- [Get set up \>](https://developer.android.com/develop/ui/compose/setup)

Jetpack Compose is a modern toolkit for building native Android UI. Jetpack Compose
simplifies and accelerates UI development on Android with less code, powerful tools,
and intuitive Kotlin APIs.


In this tutorial, you'll build a simple UI component with declarative functions. You won't
be editing any XML layouts or using the Layout Editor. Instead, you will call composable
functions to define what elements you want, and the Compose compiler will do the rest.
![Full Preview](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/00-hero-device.png)

- 4 lessons
- [Get setup \>](https://developer.android.com/develop/ui/compose/setup)
![Full Preview](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/hero.png) [](https://developer.android.com/develop/ui/compose/tutorial#top_of_page)

## Lesson 1: Composable functions


Jetpack Compose is built around composable functions. These functions let you define your
app's UI programmatically by describing how it should look and providing data dependencies,
rather than focusing on the process of the UI's construction (initializing an element,
attaching it to a parent, etc.). To create a composable function, just add the
`@Composable` annotation to the function name.
![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson1-01.svg)

#### Add a text element


To begin, download the most recent version of [Android Studio](https://developer.android.com/studio), and create an app by selecting **New Project** , and under the
**Phone and Tablet** category, select **Empty Activity** .
Name your app **ComposeTutorial** and click **Finish**. The default
template already contains some Compose elements, but in this tutorial you will build it up step
by step.


First, display a "Hello world!" text by adding a text element inside the
`onCreate` method. You do this by defining a content
block, and calling the [`Text`](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#Text(kotlin.String,androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Color,androidx.compose.ui.unit.TextUnit,androidx.compose.ui.text.font.FontStyle,androidx.compose.ui.text.font.FontWeight,androidx.compose.ui.text.font.FontFamily,androidx.compose.ui.unit.TextUnit,androidx.compose.ui.text.style.TextDecoration,androidx.compose.ui.text.style.TextAlign,androidx.compose.ui.unit.TextUnit,androidx.compose.ui.text.style.TextOverflow,kotlin.Boolean,kotlin.Int,kotlin.Function1,androidx.compose.ui.text.TextStyle)) composable function. The
`setContent` block defines the activity's layout where
composable functions are called. Composable functions can only be called from other composable
functions.


Jetpack Compose uses a Kotlin compiler plugin to transform these composable functions into the
app's UI elements. For example, the `Text` composable
function that is defined by the Compose UI library displays a text label on the screen.

```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Text("Hello world!")
        }
    }
}
  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson1-02.png)

#### Define a composable function


To make a function composable, add the `@Composable` annotation.
To try this out, define a `MessageCard` function which is
passed a name, and uses it to configure the text element.

```kotlin
// ...
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MessageCard("Android")
        }
    }
}

@Composable
fun MessageCard(name: String) {
    Text(text = "Hello $name!")
}

  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson1-03.png)

#### Preview your function in Android Studio


The `@Preview` annotation lets you preview your composable functions within Android
Studio without having to build and install the app to an Android device or emulator. The
annotation must be used on a composable function that does not take in parameters. For this
reason, you can't preview the `MessageCard` function
directly. Instead, make a second function named
`PreviewMessageCard`, which calls
`MessageCard` with an appropriate parameter. Add the
`@Preview` annotation before
`@Composable`.

```kotlin
// ...
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MessageCard(name: String) {
    Text(text = "Hello $name!")
}

@Preview
@Composable
fun PreviewMessageCard() {
    MessageCard("Android")
}
  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson1-03.png)
Rebuild your project. The app itself doesn't change, since the new
`PreviewMessageCard` function isn't called anywhere,
but Android Studio adds a preview window which you can expand by clicking on the split
(design/code) view. This window shows a preview of the UI elements created by composable
functions marked with the `@Preview` annotation. To update
the previews at any time, click the refresh button at the top of the preview window.
![Preview of a composable function in Android Studio](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson1-04.png)

```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Text("Hello world!")
        }
    }
}
  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson1-02.png)

```kotlin
// ...
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MessageCard("Android")
        }
    }
}

@Composable
fun MessageCard(name: String) {
    Text(text = "Hello $name!")
}

  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson1-03.png)

```kotlin
// ...
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MessageCard(name: String) {
    Text(text = "Hello $name!")
}

@Preview
@Composable
fun PreviewMessageCard() {
    MessageCard("Android")
}
  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson1-03.png) ![Preview of a composable function in Android Studio](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson1-04.png) [](https://developer.android.com/develop/ui/compose/tutorial#top_of_page)

## Lesson 2: Layouts


UI elements are hierarchical, with elements contained in other elements. In Compose, you
build a UI hierarchy by calling composable functions from other composable functions.
![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson2-01.svg)

#### Add multiple texts


So far you've built your first composable function and preview! To discover more Jetpack Compose
capabilities, you're going to build a simple messaging screen containing a list of messages that
can be expanded with some animations.


Start by making the message composable richer by displaying the name of its author and a
message content. You need to first change the composable parameter to accept a
`Message` object instead of a
`String`, and add another
`Text` composable inside the
`MessageCard` composable. Make sure to update the preview
as well.

```kotlin
// ...

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MessageCard(Message("Android", "Jetpack Compose"))
        }
    }
}

data class Message(val author: String, val body: String)

@Composable
fun MessageCard(msg: Message) {
    Text(text = msg.author)
    Text(text = msg.body)
}

@Preview
@Composable
fun PreviewMessageCard() {
    MessageCard(
        msg = Message("Lexi", "Hey, take a look at Jetpack Compose, it's great!")
    )
}

  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson2-02.png)
This code creates two text elements inside the content view. However, since you haven't provided
any information about how to arrange them, the text elements are drawn on top of each other,
making the text unreadable.

#### Using a Column


The [`Column`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Column.composable#Column(androidx.compose.ui.Modifier,androidx.compose.foundation.layout.Arrangement.Vertical,androidx.compose.ui.Alignment.Horizontal,kotlin.Function1)(androidx.compose.ui.Modifier,androidx.compose.foundation.layout.Arrangement.Vertical,androidx.compose.ui.Alignment.Horizontal,kotlin.Function1)) function lets you arrange elements vertically.
Add `Column` to the
`MessageCard` function.   

You can use [`Row`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Row.composable#Row(androidx.compose.ui.Modifier,androidx.compose.foundation.layout.Arrangement.Horizontal,androidx.compose.ui.Alignment.Vertical,kotlin.Function1)(androidx.compose.ui.Modifier,androidx.compose.foundation.layout.Arrangement.Horizontal,androidx.compose.ui.Alignment.Vertical,kotlin.Function1)) to arrange items horizontally and
[`Box`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Box.composable#Box(androidx.compose.ui.Modifier)) to stack elements.

```kotlin
// ...
import androidx.compose.foundation.layout.Column

@Composable
fun MessageCard(msg: Message) {
    Column {
        Text(text = msg.author)
        Text(text = msg.body)
    }
}
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson2-04.png)

#### Add an image element


Enrich your message card by adding a profile picture of the sender. Use the
[Resource Manager](https://developer.android.com/studio/write/resource-manager#import)
to import an image from your photo library or use [this one](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/profile_picture.png). Add a
`Row` composable to have a well structured design and an
[`Image`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/Image.composable#Image(androidx.compose.ui.graphics.painter.Painter,kotlin.String,androidx.compose.ui.Modifier,androidx.compose.ui.Alignment,androidx.compose.ui.layout.ContentScale,kotlin.Float,androidx.compose.ui.graphics.ColorFilter)) composable inside it.

```kotlin
// ...
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.res.painterResource

@Composable
fun MessageCard(msg: Message) {
    Row {
        Image(
            painter = painterResource(R.drawable.profile_picture),
            contentDescription = "Contact profile picture",
        )
    
       Column {
            Text(text = msg.author)
            Text(text = msg.body)
        }
  
    }
  
}
  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson2-05.png)

#### Configure your layout


Your message layout has the right structure but its elements aren't well spaced and the image is
too big! To decorate or configure a composable, Compose uses **modifiers**. They
allow you to change the composable's size, layout, appearance or add high-level interactions,
such as making an element clickable. You can chain them to create richer composables. You'll use
some of them to improve the layout.

```kotlin
// ...
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun MessageCard(msg: Message) {
    // Add padding around our message
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Image(
            painter = painterResource(R.drawable.profile_picture),
            contentDescription = "Contact profile picture",
            modifier = Modifier
                // Set image size to 40 dp
                .size(40.dp)
                // Clip image to be shaped as a circle
                .clip(CircleShape)
        )

        // Add a horizontal space between the image and the column
        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(text = msg.author)
            // Add a vertical space between the author and message texts
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = msg.body)
        }
    }
}
  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson2-06.png)

```kotlin
// ...

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MessageCard(Message("Android", "Jetpack Compose"))
        }
    }
}

data class Message(val author: String, val body: String)

@Composable
fun MessageCard(msg: Message) {
    Text(text = msg.author)
    Text(text = msg.body)
}

@Preview
@Composable
fun PreviewMessageCard() {
    MessageCard(
        msg = Message("Lexi", "Hey, take a look at Jetpack Compose, it's great!")
    )
}

  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson2-02.png) ![Preview of two overlapping Text composables](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson2-03.png)

```kotlin
// ...
import androidx.compose.foundation.layout.Column

@Composable
fun MessageCard(msg: Message) {
    Column {
        Text(text = msg.author)
        Text(text = msg.body)
    }
}
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson2-04.png)

```kotlin
// ...
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.res.painterResource

@Composable
fun MessageCard(msg: Message) {
    Row {
        Image(
            painter = painterResource(R.drawable.profile_picture),
            contentDescription = "Contact profile picture",
        )
    
       Column {
            Text(text = msg.author)
            Text(text = msg.body)
        }
  
    }
  
}
  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson2-05.png)

```kotlin
// ...
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun MessageCard(msg: Message) {
    // Add padding around our message
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Image(
            painter = painterResource(R.drawable.profile_picture),
            contentDescription = "Contact profile picture",
            modifier = Modifier
                // Set image size to 40 dp
                .size(40.dp)
                // Clip image to be shaped as a circle
                .clip(CircleShape)
        )

        // Add a horizontal space between the image and the column
        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(text = msg.author)
            // Add a vertical space between the author and message texts
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = msg.body)
        }
    }
}
  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson2-06.png) [](https://developer.android.com/develop/ui/compose/tutorial#top_of_page)

## Lesson 3: Material Design


Compose is built to support Material Design principles. Many of its UI elements implement
Material Design out of the box. In this lesson, you'll style your app with Material Design
widgets.
![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson3-01.svg)

#### Use Material Design


Your message design now has a layout, but it doesn't look great yet.


Jetpack Compose provides an implementation of Material Design 3 and its UI elements out of the
box. You'll improve the appearance of our `MessageCard`
composable using Material Design styling.


To start, wrap the `MessageCard` function with the
Material theme created in your project, `ComposeTutorialTheme`,
as well as a `Surface`.
Do it both in the `@Preview` and in the
`setContent` function. Doing so will allow your composables
to inherit styles as defined in your app's theme ensuring consistency across your app.


Material Design is built around three pillars: `Color`,
`Typography`, and `Shape`.
You will add them one by one.


**Note:** the Empty Compose Activity template generates a default theme for your project that
allows you to customize [`MaterialTheme`](https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialTheme.composable#MaterialTheme(androidx.compose.material3.ColorScheme,androidx.compose.material3.Shapes,androidx.compose.material3.Typography,kotlin.Function0)).

If you named your project anything different from
**ComposeTutorial** , you can find your custom theme in the
`Theme.kt` file in the
`ui.theme` subpackage.

```kotlin
// ...

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeTutorialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MessageCard(Message("Android", "Jetpack Compose"))
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewMessageCard() {
    ComposeTutorialTheme {
        Surface {
            MessageCard(
                msg = Message("Lexi", "Take a look at Jetpack Compose, it's great!")
            )
        }
    }
}


  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson3-02.png)

#### Color


Use `MaterialTheme.colorScheme` to style with colors from the
wrapped theme. You can use these values from the theme anywhere a color is needed. This example uses dynamic theming colors (defined by device preferences).
You can set `dynamicColor` to `false` in the `MaterialTheme.kt` file to change this.


Style the title and add a border to the image.

```kotlin
// ...
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme

@Composable
fun MessageCard(msg: Message) {
   Row(modifier = Modifier.padding(all = 8.dp)) {
       Image(
           painter = painterResource(R.drawable.profile_picture),
           contentDescription = null,
           modifier = Modifier
               .size(40.dp)
               .clip(CircleShape)
               .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
       )

       Spacer(modifier = Modifier.width(8.dp))

       Column {
           Text(
               text = msg.author,
               color = MaterialTheme.colorScheme.secondary
           )

           Spacer(modifier = Modifier.height(4.dp))
           Text(text = msg.body)
       }
   }
}

  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson3-03.png)

#### Typography


Material Typography styles are available in the `MaterialTheme`,
just add them to the `Text` composables.

```kotlin
// ...

@Composable
fun MessageCard(msg: Message) {
   Row(modifier = Modifier.padding(all = 8.dp)) {
       Image(
           painter = painterResource(R.drawable.profile_picture),
           contentDescription = null,
           modifier = Modifier
               .size(40.dp)
               .clip(CircleShape)
               .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
       )
       Spacer(modifier = Modifier.width(8.dp))

       Column {
           Text(
               text = msg.author,
               color = MaterialTheme.colorScheme.secondary,
               style = MaterialTheme.typography.titleSmall
           )

           Spacer(modifier = Modifier.height(4.dp))

           Text(
               text = msg.body,
               style = MaterialTheme.typography.bodyMedium
           )
       }
   }
}

  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson3-04.png)

#### Shape


With `Shape`you can add the final touches. First, wrap the
message body text around a
[`Surface`](https://developer.android.com/reference/kotlin/androidx/compose/material/Surface.composable#Surface(androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Shape,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.foundation.BorderStroke,androidx.compose.ui.unit.Dp,kotlin.Function0)) composable. Doing so allows customizing the
message body's shape and elevation. Padding is also added to the message for a better layout.

```kotlin
// ...
import androidx.compose.material3.Surface

@Composable
fun MessageCard(msg: Message) {
   Row(modifier = Modifier.padding(all = 8.dp)) {
       Image(
           painter = painterResource(R.drawable.profile_picture),
           contentDescription = null,
           modifier = Modifier
               .size(40.dp)
               .clip(CircleShape)
               .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
       )
       Spacer(modifier = Modifier.width(8.dp))

       Column {
           Text(
               text = msg.author,
               color = MaterialTheme.colorScheme.secondary,
               style = MaterialTheme.typography.titleSmall
           )

           Spacer(modifier = Modifier.height(4.dp))

           Surface(shape = MaterialTheme.shapes.medium, shadowElevation = 1.dp) {
               Text(
                   text = msg.body,
                   modifier = Modifier.padding(all = 4.dp),
                   style = MaterialTheme.typography.bodyMedium
               )
           }
       }
   }
}

  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson3-05.png)

#### Enable dark theme


[Dark theme](https://developer.android.com/guide/topics/ui/look-and-feel/darktheme)
(or night mode) can be enabled to avoid a bright display especially at night, or simply to save
the device battery. Thanks to the Material Design support, Jetpack Compose can handle the dark
theme by default. Having used Material Design colors, text and backgrounds will automatically
adapt to the dark background.


You can create multiple previews in your file as separate functions, or add multiple
annotations to the same function.


Add a new preview annotation and enable night mode.

```kotlin
// ...
import android.content.res.Configuration

@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun PreviewMessageCard() {
   ComposeTutorialTheme {
    Surface {
      MessageCard(
        msg = Message("Lexi", "Hey, take a look at Jetpack Compose, it's great!")
      )
    }
   }
}
  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson3-06.png)
Color choices for the light and dark themes are defined in the IDE-generated
`Theme.kt` file.


So far, you've created a message UI element that displays an image and two texts with different
styles, and it looks good both in light and dark themes!

```kotlin
// ...
import android.content.res.Configuration

@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun PreviewMessageCard() {
   ComposeTutorialTheme {
    Surface {
      MessageCard(
        msg = Message("Lexi", "Hey, take a look at Jetpack Compose, it's great!")
      )
    }
   }
}
  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson3-06.png)

```kotlin
// ...

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeTutorialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MessageCard(Message("Android", "Jetpack Compose"))
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewMessageCard() {
    ComposeTutorialTheme {
        Surface {
            MessageCard(
                msg = Message("Lexi", "Take a look at Jetpack Compose, it's great!")
            )
        }
    }
}


  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson3-02.png)

```kotlin
// ...
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme

@Composable
fun MessageCard(msg: Message) {
   Row(modifier = Modifier.padding(all = 8.dp)) {
       Image(
           painter = painterResource(R.drawable.profile_picture),
           contentDescription = null,
           modifier = Modifier
               .size(40.dp)
               .clip(CircleShape)
               .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
       )

       Spacer(modifier = Modifier.width(8.dp))

       Column {
           Text(
               text = msg.author,
               color = MaterialTheme.colorScheme.secondary
           )

           Spacer(modifier = Modifier.height(4.dp))
           Text(text = msg.body)
       }
   }
}

  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson3-03.png)

```kotlin
// ...

@Composable
fun MessageCard(msg: Message) {
   Row(modifier = Modifier.padding(all = 8.dp)) {
       Image(
           painter = painterResource(R.drawable.profile_picture),
           contentDescription = null,
           modifier = Modifier
               .size(40.dp)
               .clip(CircleShape)
               .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
       )
       Spacer(modifier = Modifier.width(8.dp))

       Column {
           Text(
               text = msg.author,
               color = MaterialTheme.colorScheme.secondary,
               style = MaterialTheme.typography.titleSmall
           )

           Spacer(modifier = Modifier.height(4.dp))

           Text(
               text = msg.body,
               style = MaterialTheme.typography.bodyMedium
           )
       }
   }
}

  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson3-04.png)

```kotlin
// ...
import androidx.compose.material3.Surface

@Composable
fun MessageCard(msg: Message) {
   Row(modifier = Modifier.padding(all = 8.dp)) {
       Image(
           painter = painterResource(R.drawable.profile_picture),
           contentDescription = null,
           modifier = Modifier
               .size(40.dp)
               .clip(CircleShape)
               .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
       )
       Spacer(modifier = Modifier.width(8.dp))

       Column {
           Text(
               text = msg.author,
               color = MaterialTheme.colorScheme.secondary,
               style = MaterialTheme.typography.titleSmall
           )

           Spacer(modifier = Modifier.height(4.dp))

           Surface(shape = MaterialTheme.shapes.medium, shadowElevation = 1.dp) {
               Text(
                   text = msg.body,
                   modifier = Modifier.padding(all = 4.dp),
                   style = MaterialTheme.typography.bodyMedium
               )
           }
       }
   }
}

  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson3-05.png)

```kotlin
// ...
import android.content.res.Configuration

@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun PreviewMessageCard() {
   ComposeTutorialTheme {
    Surface {
      MessageCard(
        msg = Message("Lexi", "Hey, take a look at Jetpack Compose, it's great!")
      )
    }
   }
}
  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson3-06.png) ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson3-07.png) Preview showing both light and dark themed composables. [](https://developer.android.com/develop/ui/compose/tutorial#top_of_page)

## Lesson 4: Lists and animations


Lists and animations are everywhere in apps. In this lesson, you will learn how Compose
makes it easy to create lists and fun to add animations.
![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson4-01.svg)

#### Create a list of messages


A chat with one message feels a bit lonely, so we are going to change the conversation to have more than
one message. You'll need to create a `Conversation` function
that will show multiple messages. For this use case, use Compose's
[`LazyColumn`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/package-summary#LazyColumn(androidx.compose.ui.Modifier,androidx.compose.foundation.lazy.LazyListState,androidx.compose.foundation.layout.PaddingValues,kotlin.Boolean,androidx.compose.foundation.layout.Arrangement.Vertical,androidx.compose.ui.Alignment.Horizontal,androidx.compose.foundation.gestures.FlingBehavior,kotlin.Boolean,kotlin.Function1)) and
[`LazyRow`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/package-summary#LazyRow(androidx.compose.ui.Modifier,androidx.compose.foundation.lazy.LazyListState,androidx.compose.foundation.layout.PaddingValues,kotlin.Boolean,androidx.compose.foundation.layout.Arrangement.Horizontal,androidx.compose.ui.Alignment.Vertical,androidx.compose.foundation.gestures.FlingBehavior,kotlin.Boolean,kotlin.Function1)). These composables render only the elements
that are visible on screen, so they are designed to be very efficient for long lists.


In this code snippet, you can see that `LazyColumn` has an
`items` child. It takes a
`List` as a parameter and its lambda
receives a parameter we've named `message` (we could have
named it whatever we want) which is an instance of `Message`.
In short, this lambda is called for each item of the provided
`List`. Copy the
[sample dataset](https://developer.android.com/static/develop/ui/compose/tutorial/lessons/lesson-4/steps/code/SampleData.kt)
into your project to help bootstrap the conversation quickly.

```kotlin
// ...
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@Composable
fun Conversation(messages: List<Message>) {
    LazyColumn {
        items(messages) { message ->
            MessageCard(message)
        }
    }
}

@Preview
@Composable
fun PreviewConversation() {
    ComposeTutorialTheme {
        Conversation(SampleData.conversationSample)
    }
}

  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson4-02.png)

#### Animate messages while expanding


The conversation is getting more interesting. It's time to play with animations! You will add
the ability to expand a message to show a longer one, animating both the content size and the
background color. To store this local UI state, you need to keep track of whether a message has
been expanded or not. To keep track of this state change, you have to use the functions
`remember` and
`mutableStateOf`.


Composable functions can store local state in memory by using
`remember`, and track changes to the value passed to
`mutableStateOf`. Composables (and their children) using
this state will get redrawn automatically when the value is updated. This is called
[recomposition](https://developer.android.com/develop/ui/compose/mental-model#recomposition).


By using Compose's state APIs like `remember` and
`mutableStateOf`, any changes to state automatically update the UI.


**Note:** You need to add the following imports to correctly use Kotlin's
delegated property syntax (the `by` keyword). Alt+Enter or Option+Enter adds them
for you.  

`
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
`

```kotlin
// ...
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       setContent {
           ComposeTutorialTheme {
               Conversation(SampleData.conversationSample)
           }
       }
   }
}

@Composable
fun MessageCard(msg: Message) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Image(
            painter = painterResource(R.drawable.profile_picture),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))

        // We keep track if the message is expanded or not in this
        // variable
        var isExpanded by remember { mutableStateOf(false) }

        // We toggle the isExpanded variable when we click on this Column
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Text(
                text = msg.author,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    // If the message is expanded, we display all its content
                    // otherwise we only display the first line
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

  
```
show preview hide preview
Now you can change the background of the message content based on
`isExpanded` when we click on a message. You will use the
`clickable` modifier to handle click events on the
composable. Instead of just toggling the background color of the
`Surface`, you will animate the background color by
gradually modifying its value from
`MaterialTheme.colorScheme.surface` to
`MaterialTheme.colorScheme.primary` and vice versa. To do so,
you will use the `animateColorAsState` function. Lastly, you
will use the `animateContentSize` modifier to animate the
message container size smoothly:

```kotlin
// ...
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize

@Composable
fun MessageCard(msg: Message) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Image(
            painter = painterResource(R.drawable.profile_picture),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))

        // We keep track if the message is expanded or not in this
        // variable
        var isExpanded by remember { mutableStateOf(false) }
        // surfaceColor will be updated gradually from one color to the other
        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        )

        // We toggle the isExpanded variable when we click on this Column
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Text(
                text = msg.author,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
                // surfaceColor color will be changing gradually from primary to surface
                color = surfaceColor,
                // animateContentSize will change the Surface size gradually
                modifier = Modifier.animateContentSize().padding(1.dp)
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    // If the message is expanded, we display all its content
                    // otherwise we only display the first line
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

  
```
show preview hide preview

```kotlin
// ...
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@Composable
fun Conversation(messages: List<Message>) {
    LazyColumn {
        items(messages) { message ->
            MessageCard(message)
        }
    }
}

@Preview
@Composable
fun PreviewConversation() {
    ComposeTutorialTheme {
        Conversation(SampleData.conversationSample)
    }
}

  
```
show preview hide preview ![](https://developer.android.com/static/develop/ui/compose/images/compose-tutorial/lesson4-02.png)

```kotlin
// ...
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       setContent {
           ComposeTutorialTheme {
               Conversation(SampleData.conversationSample)
           }
       }
   }
}

@Composable
fun MessageCard(msg: Message) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Image(
            painter = painterResource(R.drawable.profile_picture),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))

        // We keep track if the message is expanded or not in this
        // variable
        var isExpanded by remember { mutableStateOf(false) }

        // We toggle the isExpanded variable when we click on this Column
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Text(
                text = msg.author,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    // If the message is expanded, we display all its content
                    // otherwise we only display the first line
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

  
```
show preview hide preview

```kotlin
// ...
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize

@Composable
fun MessageCard(msg: Message) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Image(
            painter = painterResource(R.drawable.profile_picture),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))

        // We keep track if the message is expanded or not in this
        // variable
        var isExpanded by remember { mutableStateOf(false) }
        // surfaceColor will be updated gradually from one color to the other
        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        )

        // We toggle the isExpanded variable when we click on this Column
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Text(
                text = msg.author,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
                // surfaceColor color will be changing gradually from primary to surface
                color = surfaceColor,
                // animateContentSize will change the Surface size gradually
                modifier = Modifier.animateContentSize().padding(1.dp)
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    // If the message is expanded, we display all its content
                    // otherwise we only display the first line
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

  
```
show preview hide preview

## Next steps


Congratulations, you've finished the Compose tutorial! You've built a simple chat screen
efficiently showing a list of expandable \& animated messages containing an image and
texts, designed using Material Design principles with a dark theme included and
previews---all in **under 100 lines of code!**

Here's what you've learned so far:

- Defining composable functions
- Adding different elements in your composable
- Structuring your UI component using layout composables
- Extending composables by using modifiers
- Creating an efficient list
- Keeping track of state and modifying it
- Adding user interaction on a composable
- Animating messages while expanding them


If you want to dig deeper on some of these steps, explore the resources below.

## Next steps

Setup

### [Get set up](http://developer.android.com/develop/ui/compose/setup)

Now that you've finished the Compose tutorial, you're ready to start building with Compose. [Create a new app](http://developer.android.com/develop/ui/compose/setup) [Add to an existing app](http://developer.android.com/develop/ui/compose/setup#setup-compose) Pathway

### [Continue learning](http://developer.android.com/courses/pathways/compose)

Check out our curated pathway of codelabs and videos that will help you learn and master Jetpack Compose. [Start course](http://developer.android.com/courses/pathways/compose)