package com.aistra.hail.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HailBottomSheet(
    onDismissRequest: () -> Unit,
    dragHandle: (@Composable () -> Unit)? = { BottomSheetDefaults.DragHandle() },
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = dragHandle,
        containerColor = containerColor,
        content = content,
    )
}
