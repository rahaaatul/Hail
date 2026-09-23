package com.aistra.hail.ui.home.pager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.home.PagerViewModel
import com.aistra.hail.ui.home.PagerViewModel.Tag

@Composable
fun PagerScreen(
    viewModel: PagerViewModel,
    tabType: String,
    isMultiSelect: Boolean = false,
    selectedApps: Set<String> = emptySet(),
    onAppClick: (AppInfo) -> Unit = {},
    onAppLongClick: (AppInfo) -> Unit = {},
    onMultiSelectToggle: () -> Unit = {},
    onTagSelected: () -> Unit = {},
    onCancelMultiselect: () -> Unit = {},
    onTagEdit: () -> Unit = {},
    onCheckedChange: (AppInfo) -> Unit = {},
    onDeleteTag: (AppInfo, Int) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val showTagBadge = tabType !in listOf("all", "frequent", "recent")
    val canEditTags = tabType !in listOf("all", "frequent", "recent")
    val title = remember(tabType, tags) {
        when (tabType) {
            "all" -> stringResource(R.string.filter_all_apps)
            "frequent" -> stringResource(R.string.filter_frequent_apps)
            "recent" -> stringResource(R.string.filter_recent_apps)
            else -> tags.find { it.label == tabType }?.label ?: tabType
        }
    }
    var showTagEditDialog by rememberSaveable { mutableStateOf(false) }
    var tagToEdit by rememberSaveable { mutableStateOf<Tag?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PagerHeader(
                title = title,
                onEditTagsClicked = {
                    tagToEdit = tags.find { it.label == tabType }
                    showTagEditDialog = true
                },
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
                    onCheckedChange = onCheckedChange,
                    onDeleteTag = onDeleteTag,
                )
            }
            if (isMultiSelect) {
                MultiSelectToolbar(
                    selectedCount = selectedApps.size,
                    onTagSelected = onTagSelected,
                    onCancel = onCancelMultiselect,
                )
            }
        }
        if (showTagEditDialog) {
            tagToEdit?.let { tag ->
                TagEditDialog(
                    currentTagName = tag.label,
                    onDismissed = { showTagEditDialog = false },
                    onSaved = { newName ->
                        val idx = HailData.tags.indexOf(tag.label to tag.id)
                        if (idx >= 0) {
                            HailData.tags[idx] = newName to tag.id
                            HailData.saveTags()
                            onTagEdit()
                        }
                        showTagEditDialog = false
                    },
                    onDeleted = {
                        HailData.tags.remove(tag.label to tag.id)
                        HailData.saveTags()
                        onTagEdit()
                        showTagEditDialog = false
                    },
                )
            }
        }
    }
}

