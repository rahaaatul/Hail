package com.aistra.hail.ui.actions

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aistra.hail.utils.LaunchAction

// What the app picker is choosing.
sealed interface PickerTarget {
    val multi: Boolean
    val selected: Set<String>
    val exclude: Set<String>

    data class Single(
        override val selected: Set<String> = emptySet(),
        override val exclude: Set<String> = emptySet(),
    ) : PickerTarget {
        override val multi: Boolean = false
    }

    data class Multi(
        override val selected: Set<String> = emptySet(),
        override val exclude: Set<String> = emptySet(),
    ) : PickerTarget {
        override val multi: Boolean = true
    }
}

class ActionsUiState {
    var actions by mutableStateOf<List<LaunchAction>>(emptyList())
    var editorLaunch by mutableStateOf("")
    var editorUnfreeze by mutableStateOf(emptyList<String>())
    var editorTarget by mutableStateOf<EditorTarget?>(null)
    var pickerTarget by mutableStateOf<PickerTarget?>(null)
    var pickerApps by mutableStateOf<List<ApplicationInfo>>(emptyList())
    var menuTarget by mutableStateOf<LaunchAction?>(null)
    var deleteTarget by mutableStateOf<LaunchAction?>(null)
}

sealed interface EditorTarget {
    val existing: LaunchAction?

    object New : EditorTarget {
        override val existing get() = null
    }

    data class Edit(
        override val existing: LaunchAction,
    ) : EditorTarget
}
