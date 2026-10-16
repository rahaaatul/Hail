package com.aistra.hail.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp

private const val TAG_LIST_TOP_SPACING_DP = 16
private const val TAG_ROW_HORIZONTAL_PADDING_DP = 24
private const val TAG_ROW_VERTICAL_PADDING_DP = 12
private const val TAG_CHECKBOX_TEXT_SPACING_DP = 24

@Composable
fun TriStateTagList(
    tagNames: List<String>,
    initialStates: Array<ToggleableState>,
    states: MutableList<ToggleableState>
) = Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
    Spacer(modifier = Modifier.height(TAG_LIST_TOP_SPACING_DP.dp))
    tagNames.forEachIndexed { index, name ->
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                states[index] = if (initialStates[index] == ToggleableState.Indeterminate) {
                    when (states[index]) {
                        ToggleableState.On -> ToggleableState.Off
                        ToggleableState.Off -> ToggleableState.Indeterminate
                        ToggleableState.Indeterminate -> ToggleableState.On
                    }
                } else if (states[index] == ToggleableState.On) {
                    ToggleableState.Off
                } else {
                    ToggleableState.On
                }
            }.padding(
                horizontal = TAG_ROW_HORIZONTAL_PADDING_DP.dp,
                vertical = TAG_ROW_VERTICAL_PADDING_DP.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TriStateCheckbox(
                state = states[index],
                onClick = null,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
            )
            Spacer(modifier = Modifier.width(TAG_CHECKBOX_TEXT_SPACING_DP.dp))
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
