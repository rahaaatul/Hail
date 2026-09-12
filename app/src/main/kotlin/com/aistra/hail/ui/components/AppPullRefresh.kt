package com.aistra.hail.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pullrefresh.PullRefreshIndicator
import androidx.compose.material3.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive PullRefresh wrapper.
 * Replaces SwipeRefresh from fragment_apps.xml and fragment_pager.xml.
 *
 * @param refreshing Whether the refresh indicator is showing
 * @param onRefresh Called when the user pulls to refresh
 * @param content The scrollable content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPullRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit
) {
    val state = rememberPullRefreshState(refreshing, onRefresh)
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        PullRefreshIndicator(
            refreshing = refreshing,
            state = state,
            modifier = Modifier.align(Alignment.TopCenter),
            threshold = 48.dp
        )
    }
}