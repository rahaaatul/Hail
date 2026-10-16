package com.aistra.hail.ui.home

import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

private val grayscaleIconFilter = ColorFilter.colorMatrix(
    ColorMatrix().apply { setToSaturation(0f) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PagerScreen(
    apps: List<AppInfo>,
    selectedPackages: Set<String>,
    grayscaleIcon: Boolean,
    homeFontSize: Float,
    columns: Int,
    isLandscape: Boolean,
    onRefresh: () -> Unit,
    onItemClick: (AppInfo) -> Unit,
    onItemLongClick: (AppInfo) -> Unit,
    onScrolled: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val iconSize = dimensionResource(R.dimen.app_icon_size)
    val iconSizePx = with(density) { iconSize.roundToPx() }
    val tileMargin = dimensionResource(R.dimen.padding_extra_small)
    val iconPadding = dimensionResource(R.dimen.padding_small)
    val userId = HPackages.myUserId
    val defaultIcon = remember(context, iconSizePx) {
        context.packageManager.defaultActivityIcon
            .toBitmap(iconSizePx, iconSizePx)
            .asImageBitmap()
    }
    val insets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val startInset = with(density) {
        (if (layoutDirection == LayoutDirection.Ltr) {
            insets.getLeft(this, layoutDirection)
        } else {
            insets.getRight(this, layoutDirection)
        }).toDp()
    }
    val endInset = with(density) {
        (if (layoutDirection == LayoutDirection.Ltr) {
            insets.getRight(this, layoutDirection)
        } else {
            insets.getLeft(this, layoutDirection)
        }).toDp()
    }
    val bottomInset = with(density) { insets.getBottom(this).toDp() }
    val gridState = rememberLazyGridState()
    val currentOnScrolled by rememberUpdatedState(onScrolled)

    LaunchedEffect(gridState, apps.isEmpty()) {
        if (apps.isEmpty()) {
            currentOnScrolled(false)
        } else {
            snapshotFlow {
                gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
            }.distinctUntilChanged().collect(currentOnScrolled)
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRefresh()
            isRefreshing = false
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = if (isLandscape) 0.dp else startInset,
                end = endInset,
            ),
    ) {
        if (apps.isEmpty()) {
            Text(
                text = stringResource(R.string.nothing_here),
                modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns.coerceAtLeast(1)),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(rememberNestedScrollInteropConnection()),
                contentPadding = PaddingValues(
                    bottom = if (isLandscape) bottomInset else 0.dp,
                ),
                verticalArrangement = Arrangement.Top,
            ) {
                items(
                    items = apps,
                    key = { it.packageName },
                    contentType = { "app" },
                ) { info ->
                    PagerAppTile(
                        context = context,
                        defaultIcon = defaultIcon,
                        iconSize = iconSize,
                        iconSizePx = iconSizePx,
                        tileMargin = tileMargin,
                        iconPadding = iconPadding,
                        packageName = info.packageName,
                        applicationInfo = info.applicationInfo,
                        userId = userId,
                        name = info.name,
                        state = info.state,
                        whitelisted = info.whitelisted,
                        selected = info.packageName in selectedPackages,
                        grayscaleIcon = grayscaleIcon,
                        homeFontSize = homeFontSize,
                        onClick = remember(info, onItemClick) { { onItemClick(info) } },
                        onLongClick = remember(info, onItemLongClick) { { onItemLongClick(info) } },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagerAppTile(
    context: android.content.Context,
    defaultIcon: ImageBitmap,
    iconSize: androidx.compose.ui.unit.Dp,
    iconSizePx: Int,
    tileMargin: androidx.compose.ui.unit.Dp,
    iconPadding: androidx.compose.ui.unit.Dp,
    packageName: String,
    applicationInfo: ApplicationInfo?,
    userId: Int,
    name: String,
    state: AppInfo.State,
    whitelisted: Boolean,
    selected: Boolean,
    grayscaleIcon: Boolean,
    homeFontSize: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val icon by produceState(
        defaultIcon,
        context,
        packageName,
        applicationInfo,
        userId,
        iconSizePx,
        HailData.iconPack,
        HailData.synthesizeAdaptiveIcons,
    ) {
        if (applicationInfo == null) {
            value = defaultIcon
            return@produceState
        }
        try {
            value = withContext(Dispatchers.IO) {
                AppIconCache.getOrLoadBitmap(context, applicationInfo, userId, iconSizePx)
            }.asImageBitmap()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.w("PagerScreen", "Unable to load app icon for $packageName", exception)
            value = defaultIcon
        }
    }
    val grayscale = grayscaleIcon && state == AppInfo.State.FROZEN
    val colorFilter = if (grayscale) grayscaleIconFilter else null
    val textColor = when {
        selected -> MaterialTheme.colorScheme.primary
        state == AppInfo.State.NOT_FOUND -> MaterialTheme.colorScheme.error
        grayscale -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val displayName = buildString {
        if (!grayscaleIcon && state == AppInfo.State.FROZEN) append("\u2744\uFE0F")
        if (whitelisted) append("\uD83D\uDD12")
        append(name)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(tileMargin)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier
                .size(iconSize)
                .padding(iconPadding),
            colorFilter = colorFilter,
        )
        Text(
            text = displayName,
            modifier = Modifier.fillMaxWidth(),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = homeFontSize.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PagerScreenPreview() {
    AppTheme {
        PagerScreen(
            apps = emptyList(),
            selectedPackages = emptySet(),
            grayscaleIcon = true,
            homeFontSize = 14f,
            columns = 4,
            isLandscape = false,
            onRefresh = {},
            onItemClick = {},
            onItemLongClick = {},
            onScrolled = {},
        )
    }
}
