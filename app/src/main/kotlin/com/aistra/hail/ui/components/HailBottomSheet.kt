package com.aistra.hail.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HailBottomSheet(
    onDismissRequest: () -> Unit,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    hideHandleOnExpand: Boolean = false,
    fillMaxHeight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.PartiallyExpanded, SheetValue.Expanded),
        )
    val handleAlpha by animateFloatAsState(targetValue = if (hideHandleOnExpand && sheetState.currentValue == SheetValue.Expanded) 0f else 1f, label = "handleAlpha")
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = if (fillMaxHeight) Modifier.fillMaxHeight() else Modifier,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = containerColor,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
        content = {
            HailDragHandle(modifier = Modifier.alpha(handleAlpha), fillMaxHeight = fillMaxHeight)
            content()
        },
    )
}

@Composable
private fun HailDragHandle(
    modifier: Modifier = Modifier,
    fillMaxHeight: Boolean = false,
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(top = 22.dp, bottom = if (fillMaxHeight) 0.dp else 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
        )
    }
}
