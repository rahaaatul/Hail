package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.aistra.hail.BuildConfig
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.components.HailBottomSheet
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.HPackages
import kotlinx.coroutines.flow.distinctUntilChanged

private val grayscaleColorFilter: ColorFilter =
    ColorFilter.colorMatrix(
        ColorMatrix(
            android.graphics
                .ColorMatrix()
                .apply { setSaturation(0f) }
                .getArray(),
        ),
    )

/**
 * Apps list screen: pull-to-refresh + grid of installed apps with per-app actions.
 */
data class AppsActions(
    val onToggleChecked: (packageName: String, checked: Boolean) -> Unit,
    val onAction: (info: ApplicationInfo, action: AppAction) -> Unit,
    val onCopyText: (text: String) -> Unit,
    val onExtractApk: (info: ApplicationInfo) -> Unit,
    val onLiftChange: (lifted: Boolean) -> Unit,
)

@Composable
fun AppsScreen(
    model: AppsViewModel,
    ui: AppsUiState,
    actions: AppsActions,
) {
    val apps by model.displayApps.observeAsState(emptyList())
    val isRefreshing by model.isRefreshing.observeAsState(false)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { model.updateAppList(true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            AppsGrid(
                apps = apps,
                ui = ui,
                onToggleChecked = actions.onToggleChecked,
                onLongClick = { ui.appActionsTarget = it },
                onLiftChange = actions.onLiftChange,
            )
        }
        if (ui.filterSheetVisible) {
            FilterBottomSheet(
                onDismissRequest = { ui.filterSheetVisible = false },
                onFilterChanged = {
                    ui.showEmptyToast = true
                    model.updateDisplayAppList()
                },
            )
        }
        ui.appActionsTarget?.let { target ->
            AppActionsSheet(
                info = target,
                onDismissRequest = { ui.appActionsTarget = null },
                onAction = { action ->
                    ui.appActionsTarget = null
                    actions.onAction(target, action)
                },
                onCopyText = actions.onCopyText,
                onExtractApk = { actions.onExtractApk(target) },
            )
        }
    }
}

@Composable
private fun AppsGrid(
    apps: List<ApplicationInfo>,
    ui: AppsUiState,
    onToggleChecked: (packageName: String, checked: Boolean) -> Unit,
    onLongClick: (info: ApplicationInfo) -> Unit,
    onLiftChange: (lifted: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val span = context.resources.getInteger(R.integer.apps_span)
    val gridState = rememberLazyGridState()
    val bottomInset =
        if (isLandscape) {
            with(LocalDensity.current) { WindowInsets.safeDrawing.getBottom(this).toDp() }
        } else {
            0.dp
        }

    LaunchedEffect(gridState, onLiftChange) {
        snapshotFlow { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0 }
            .distinctUntilChanged()
            .collect { onLiftChange(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(span),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            items(apps, key = { it.packageName }) { info ->
                AppItem(
                    info = info,
                    revision = ui.checkedRevision,
                    onCheckedChange = { checked -> onToggleChecked(info.packageName, checked) },
                    onLongClick = { onLongClick(info) },
                )
            }
        }
        if (apps.isEmpty()) {
            Text(
                text = stringResource(R.string.nothing_here),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun FilterBottomSheet(
    onDismissRequest: () -> Unit,
    onFilterChanged: () -> Unit,
) {
    HailBottomSheet(onDismissRequest = onDismissRequest) {
        FilterBottomSheetContent(onFilterChanged = onFilterChanged)
    }
}

@Composable
private fun AppItem(
    info: ApplicationInfo,
    revision: Int,
    onCheckedChange: (Boolean) -> Unit,
    onLongClick: () -> Unit,
) {
    val pkg = info.packageName
    val metadata = AppMetaCache.get(pkg)
    val state = metadata?.state ?: AppInfo.State.NOT_FOUND
    val disabled = state.isFrozen
    val uninstalled = state == AppInfo.State.UNINSTALLED
    val grayscale = HailData.grayscaleIcon
    val name = metadata?.name ?: pkg
    val isSelf = pkg == BuildConfig.APPLICATION_ID

    var checked by remember(pkg, revision) { mutableStateOf(HailData.isChecked(pkg)) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(info = info, dimmed = grayscale && disabled)
        AppTexts(
            modifier = Modifier.weight(1f),
            name = name,
            disabled = disabled,
            uninstalled = uninstalled,
            grayscale = grayscale,
        )
        Checkbox(
            checked = checked,
            onCheckedChange = {
                checked = it
                onCheckedChange(it)
            },
            enabled = !isSelf,
        )
    }
}

// TODO: Home migration — reuse HailBottomSheet + sheet styling; move AppIcon to ui/components at the second use case.
@Composable
internal fun AppIcon(
    info: ApplicationInfo,
    dimmed: Boolean,
    size: Dp? = null,
    internalPadding: Dp? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val iconSize = size ?: dimensionResource(R.dimen.app_icon_size)
    val iconSizePx = with(LocalDensity.current) { iconSize.roundToPx() }
    val icon by produceState<Bitmap?>(null, info.packageName, HailData.synthesizeAdaptiveIcons, iconSizePx) {
        value = AppIconCache.loadIconBitmap(context, info, HPackages.myUserId, iconSizePx)
    }
    val fallbackIcon = remember { context.packageManager.defaultActivityIcon?.toBitmap() }
    val displayedIcon = icon ?: fallbackIcon
    val iconModifier =
        modifier
            .size(iconSize)
            .padding(internalPadding ?: dimensionResource(R.dimen.space100))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.space100)))
    if (displayedIcon != null) {
        Image(
            bitmap = displayedIcon.asImageBitmap(),
            contentDescription = null,
            modifier = iconModifier,
            colorFilter = if (dimmed) grayscaleColorFilter else null,
        )
    } else {
        Spacer(modifier = iconModifier)
    }
}

@Composable
private fun AppTexts(
    modifier: Modifier = Modifier,
    name: String,
    disabled: Boolean,
    uninstalled: Boolean,
    grayscale: Boolean,
) {
    val dimmed = grayscale && disabled
    val textColor =
        when {
            uninstalled -> MaterialTheme.colorScheme.error
            dimmed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            else -> MaterialTheme.colorScheme.onSurface
        }

    Text(
        text = if (!grayscale && disabled) "❄️$name" else name,
        style = MaterialTheme.typography.bodyLarge,
        color = textColor,
        maxLines = 1,
        modifier = modifier.basicMarquee(),
    )
}

@Preview(showBackground = true)
@Composable
fun AppItemPreview() {
    AppTheme {
        AppItem(
            info = ApplicationInfo().apply { packageName = "com.example.app" },
            revision = 0,
            onCheckedChange = {},
            onLongClick = {},
        )
    }
}
