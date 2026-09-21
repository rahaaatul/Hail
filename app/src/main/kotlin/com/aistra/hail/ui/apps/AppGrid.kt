package com.aistra.hail.ui.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistra.hail.R
import com.aistra.hail.utils.HPackages


@Composable
fun AppGrid(
    apps: List<ApplicationInfo>,
    onItemClick: (ApplicationInfo) -> Unit,
    onItemLongClick: (ApplicationInfo) -> Boolean,
    onItemCheckedChange: (ApplicationInfo, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    emptyMessage: String? = null
) {
    val context = LocalContext.current
    val span = remember { context.resources.getInteger(R.integer.apps_span) }

    Box(modifier = modifier.fillMaxSize()) {
        if (apps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emptyMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    } ?: Text(
                        text = stringResource(R.string.nothing_here),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(span),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 8.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(apps) { info ->
                    AppGridItem(
                        info = info,
                        onClick = { onItemClick(info) },
                        onLongClick = { onItemLongClick(info) },
                        onCheckedChange = { checked -> onItemCheckedChange(info, checked) }
                    )
                }
            }
        }

        if (isRefreshing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ProgressIndicator()
            }
        }
    }
}
