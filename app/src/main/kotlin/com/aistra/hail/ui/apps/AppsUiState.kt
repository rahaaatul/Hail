package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Hoisted UI state shared between the fragment (toolbar menu) and the Compose list.
 * Snapshot-backed so both sides recompose/refresh on changes.
 */
class AppsUiState {
    // Bumped whenever the checked set changes so the list re-reads HailData.
    var checkedRevision by mutableIntStateOf(0)

    // Whether the filter bottom sheet is visible; the toolbar menu opens it.
    var filterSheetVisible by mutableStateOf(false)

    // The app whose long-press actions sheet is open, or null when hidden.
    var appActionsTarget by mutableStateOf<ApplicationInfo?>(null)

    // Set before a user-triggered refresh so an empty result surfaces a toast.
    var showEmptyToast by mutableStateOf(false)
}
