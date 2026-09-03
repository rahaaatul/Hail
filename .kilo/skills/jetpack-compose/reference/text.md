[Video](https://www.youtube.com/watch?v=_qls2CEAbxI)

Text is a central piece of any UI, and Jetpack Compose makes it easier to
display or write text. Compose leverages composition of its building blocks,
meaning you don't need to overwrite properties and methods or extend big classes
to have a specific composable design and logic working the way you want.

As its base, Compose provides a [`BasicText`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/BasicText.composable)
and [`BasicTextField`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/BasicTextField.composable#BasicTextField(androidx.compose.ui.text.input.TextFieldValue,kotlin.Function1,androidx.compose.ui.Modifier,kotlin.Boolean,kotlin.Boolean,androidx.compose.ui.text.TextStyle,androidx.compose.foundation.text.KeyboardOptions,androidx.compose.foundation.text.KeyboardActions,kotlin.Boolean,kotlin.Int,kotlin.Int,androidx.compose.ui.text.input.VisualTransformation,kotlin.Function1,androidx.compose.foundation.interaction.MutableInteractionSource,androidx.compose.ui.graphics.Brush,kotlin.Function1)),
which are the barebones to display text and handle user input. At a higher
level, Compose provides [`Text`](https://developer.android.com/reference/kotlin/androidx/compose/material/Text.composable#Text(androidx.compose.ui.text.AnnotatedString,androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Color,androidx.compose.ui.unit.TextUnit,androidx.compose.ui.text.font.FontStyle,androidx.compose.ui.text.font.FontWeight,androidx.compose.ui.text.font.FontFamily,androidx.compose.ui.unit.TextUnit,androidx.compose.ui.text.style.TextDecoration,androidx.compose.ui.text.style.TextAlign,androidx.compose.ui.unit.TextUnit,androidx.compose.ui.text.style.TextOverflow,kotlin.Boolean,kotlin.Int,kotlin.Int,kotlin.collections.Map,kotlin.Function1,androidx.compose.ui.text.TextStyle))
and [`TextField`](https://developer.android.com/reference/kotlin/androidx/compose/material/TextField.composable#TextField(androidx.compose.ui.text.input.TextFieldValue,kotlin.Function1,androidx.compose.ui.Modifier,kotlin.Boolean,kotlin.Boolean,androidx.compose.ui.text.TextStyle,kotlin.Function0,kotlin.Function0,kotlin.Function0,kotlin.Function0,kotlin.Boolean,androidx.compose.ui.text.input.VisualTransformation,androidx.compose.foundation.text.KeyboardOptions,androidx.compose.foundation.text.KeyboardActions,kotlin.Boolean,kotlin.Int,kotlin.Int,androidx.compose.foundation.interaction.MutableInteractionSource,androidx.compose.ui.graphics.Shape,androidx.compose.material.TextFieldColors)),
which are composables following Material Design guidelines. It's recommended to
use them as they have the right look and feel for users on Android, and include
other options to simplify their customization without having to write a lot of
code.

## Samples

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Display emoji](https://developer.android.com/develop/ui/compose/text/emoji)
- [Custom design systems in Compose](https://developer.android.com/develop/ui/compose/designsystems/custom)
- [Material Design 2 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material)