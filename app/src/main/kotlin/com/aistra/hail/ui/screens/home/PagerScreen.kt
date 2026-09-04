@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
)

package com.aistra.hail.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.theme.HailTheme
import com.aistra.hail.ui.theme.HailThemeState

@Composable
fun PagerScreen(
    tagId: Int,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PagerViewModel = viewModel(),
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val multiselect by viewModel.multiselect.collectAsStateWithLifecycle()
    val selectedList by viewModel.selectedList.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var expandedForPackage by remember { mutableStateOf<String?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    HailTheme(state = remember { HailThemeState() }) {
        LaunchedEffect(tagId) {
            viewModel.setTagId(tagId)
        }
        LaunchedEffect(searchQuery) {
            viewModel.setQuery(searchQuery)
        }
        LaunchedEffect(selectedTabIndex) {
            viewModel.setTagId(HailData.tags[selectedTabIndex].second)
        }
        if (multiselect) {
            androidx.activity.compose.BackHandler {
                viewModel.setMultiselect(false)
            }
        }
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { viewModel.refresh() },
        ) {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.action_search)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    singleLine = true,
                )
                if (HailData.tags.size > 1) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    HailData.tags.forEachIndexed { index, (name, _) ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(name) },
                        )
                    }
                }
                }
                if (apps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.nothing_here),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(HailData.iconColumns),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                    ) {
                        items(apps, key = { it.packageName }) { appInfo ->
                            Box {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = if (appInfo in selectedList)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                                ) {
                                    HomeAppItem(
                                        appInfo = appInfo,
                                        isSelected = appInfo in selectedList,
                                        multiselectMode = multiselect,
                                        onClick = {
                                            viewModel.launchApp(appInfo.packageName) { intent ->
                                                if (intent != null) {
                                                    context.startActivity(intent)
                                                }
                                            }
                                        },
                                        onLongClick = { expandedForPackage = appInfo.packageName },
                                        onToggleSelection = {
                                            viewModel.toggleSelection(appInfo)
                                        },
                                    )
                                }
                                DropdownMenu(
                                    expanded = expandedForPackage == appInfo.packageName,
                                    onDismissRequest = { expandedForPackage = null },
                                ) {
                                    val isFrozen = appInfo.state == AppInfo.State.FROZEN
                                    DropdownMenuItem(
                                        onClick = {
                                            expandedForPackage = null
                                            viewModel.setListFrozen(!isFrozen, listOf(appInfo))
                                        },
                                        text = {
                                            Text(
                                                stringResource(
                                                    if (isFrozen) R.string.action_unfreeze
                                                    else R.string.action_freeze,
                                                ),
                                            )
                                        },
                                    )
                                    DropdownMenuItem(
                                        onClick = {
                                            expandedForPackage = null
                                            viewModel.removeCheckedApp(appInfo.packageName)
                                        },
                                        text = { Text(stringResource(R.string.action_remove_home)) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
