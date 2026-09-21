package com.aistra.hail.ui.home.pager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.material3.SwipeToRefresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistra.hail.app.AppInfo

data class Tag(val label: String, val id: Int)

@Composable
fun AppGrid(
    apps: List<AppInfo>,
    onAppClicked: (AppInfo) -> Unit,
    onAppLongClicked: (AppInfo) -> Unit,
    isMultiSelect: Boolean,
    selectedApps: Set<String>,
    showTagBadge: Boolean = false,
    tags: List<Tag> = emptyList(),
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    SwipeToRefresh(
        onRefresh = onRefresh,
        isRefreshing = isRefreshing,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 72.dp),
            modifier = modifier.fillMaxWidth().padding(8.dp),
        ) {
            if (apps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Nothing here")
                    }
                }
            } else {
                items(apps) { app ->
                    AppGridItem(
                        app = app,
                        onClick = { onAppClicked(app) },
                        onLongClick = { onAppLongClicked(app) },
                        isSelected = app.packageName in selectedApps,
                        isMultiSelect = isMultiSelect,
                        tags = tags,
                        showTagBadge = showTagBadge,
                    )
                }
            }
        }
    }
}
