Compose provides a variety of APIs to help you detect gestures that are
generated from user interactions. The APIs cover a wide range of use cases:

- Some of them are **high-level** and designed to cover the most commonly used
  gestures. For example, the
  [`clickable`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/package-summary#(androidx.compose.ui.Modifier).clickable(kotlin.Boolean,kotlin.String,androidx.compose.ui.semantics.Role,kotlin.Function0))
  modifier allows easy
  detection of a click, and it also provides accessibility features and
  displays visual indicators when tapped (such as ripples).

- There are also less commonly used gesture detectors that offer more
  flexibility on a **lower level** , like
  [`PointerInputScope.detectTapGestures`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/package-summary#(androidx.compose.ui.input.pointer.PointerInputScope).detectTapGestures(kotlin.Function1,kotlin.Function1,kotlin.coroutines.SuspendFunction2,kotlin.Function1)) or
  [`PointerInputScope.detectDragGestures`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/package-summary#(androidx.compose.ui.input.pointer.PointerInputScope).detectDragGestures(kotlin.Function1,kotlin.Function0,kotlin.Function0,kotlin.Function2))
  but don't include the extra features.

Learn more about pointer input on the following pages:

- [Understand gestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures) gives an explanation of the core concepts playing a role when handling pointer input.
- [Tap and press](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/tap-and-press) expands on single pointer, single position events.
- [Scroll](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/scroll) explains how to implement scrolling containers, and handles interoperability concerns.
- [Drag, swipe, and fling](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-swipe-fling) shows different types of dragging of a single pointer.
- [Multi-touch](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch) dives into situations where more than one pointer is used.

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Enable user interactions](https://developer.android.com/develop/ui/compose/text/user-interactions)
- [Semantics in Compose](https://developer.android.com/develop/ui/compose/semantics)
- [Compose modifiers](https://developer.android.com/develop/ui/compose/modifiers)