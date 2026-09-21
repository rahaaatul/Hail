package com.aistra.hail.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.home.PagerViewModel

@Composable
fun PagerScreen(
    viewModel: PagerViewModel,
    tabType: String,
    isMultiSelect: Boolean = false,
    selectedApps: Set<String> = emptySet(),
    onAppClick: (AppInfo) -> Unit = {},
    onAppLongClick: (AppInfo) -> Unit = {},
    onMultiSelectToggle: () -> Unit = {},
    onTagSelected: (String) -> Unit = {},
    onCancelMultiselect: () -> Unit = {},
    onTagEdit: () -> Unit = {},
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val tags = HailData.tags.map { Tag(it.first, it.second) }
    val showTagBadge = tabType !in listOf("all", "frequent", "recent")
    val canEditTags = tabType !in listOf("all", "frequent", "recent")
    val title = when {
        tabType == "all" -> "All"
        tabType == "frequent" -> "Frequent"
        tabType == "recent" -> "Recent"
        else -> HailData.tags.find { it.first == tabType }?.first ?: tabType
    }

    Column(modifier = modifier.fillMaxSize()) {
        PagerHeader(
            title = title,
            onEditText = { /* handled by parent */ },
            onEditTagsClicked = onTagEdit,
            canEditTags = canEditTags,
        )
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            AppGrid(
                apps = uiState.apps,
                onAppClicked = onAppClick,
                onAppLongClicked = onAppLongClick,
                isMultiSelect = isMultiSelect,
                selectedApps = selectedApps,
                showTagBadge = showTagBadge,
                tags = tags,
                onRefresh = onRefresh,
                isRefreshing = isRefreshing,
            )
        }
        if (isMultiSelect) {
            MultiSelectToolbar(
                selectedCount = selectedApps.size,
                onTagSelected = onTagSelected,
                onCancel = onCancelMultiselect,
            )
        }
        if (tabType !in listOf("all", "frequent", "recent")) {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}