@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
)

package com.aistra.hail.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
    tagId: Long,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PagerViewModel = viewModel(),
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val multiselect by viewModel.multiselect.collectAsStateWithLifecycle()
    val selectedList = viewModel.selectedList

    HailTheme(state = HailThemeState()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (apps.isEmpty()) {
                Text(text = stringResource(R.string.nothing_here))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(HailData.iconColumns),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                ) {
                    items(apps, key = { it.packageName }) { appInfo ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (appInfo in selectedList)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                            onClick = { viewModel.launchApp(appInfo.packageName) },
                        ) {
                            HomeAppItem(
                                appInfo = appInfo,
                                isSelected = appInfo in selectedList,
                                multiselectMode = multiselect,
                                onClick = { viewModel.launchApp(appInfo.packageName) },
                                onLongClick = { /* TODO: context menu */ },
                            )
                        }
                    }
                }
            }
        }
    }
}
