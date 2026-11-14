package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.aistra.hail.BuildConfig
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
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
@Composable
fun AppsScreen(
    model: AppsViewModel,
    ui: AppsUiState,
    onToggleChecked: (packageName: String, checked: Boolean) -> Unit,
    onAction: (info: ApplicationInfo, action: AppAction) -> Unit,
    onLiftChange: (lifted: Boolean) -> Unit,
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
                onToggleChecked = onToggleChecked,
                onAction = onAction,
                onLiftChange = onLiftChange,
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
    }
}

@Composable
private fun AppsGrid(
    apps: List<ApplicationInfo>,
    ui: AppsUiState,
    onToggleChecked: (packageName: String, checked: Boolean) -> Unit,
    onAction: (info: ApplicationInfo, action: AppAction) -> Unit,
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
                    onAction = { action -> onAction(info, action) },
                )
            }
        }
        if (apps.isEmpty()) {
            Text(
                text = stringResource(R.string.nothing_here),
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    onDismissRequest: () -> Unit,
    onFilterChanged: () -> Unit,
) {
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        FilterBottomSheetContent(onFilterChanged = onFilterChanged)
    }
}

@Composable
private fun AppItem(
    info: ApplicationInfo,
    revision: Int,
    onCheckedChange: (Boolean) -> Unit,
    onAction: (AppAction) -> Unit,
) {
    val pkg = info.packageName
    val metadata = AppMetaCache.get(pkg)
    val disabled = metadata?.state == AppInfo.State.DISABLED
    val grayscale = HailData.grayscaleIcon
    val name = metadata?.name ?: pkg
    val isSelf = pkg == BuildConfig.APPLICATION_ID

    var menuExpanded by remember { mutableStateOf(false) }
    var checked by remember(pkg, revision) { mutableStateOf(HailData.isChecked(pkg)) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { menuExpanded = true },
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(info = info, dimmed = grayscale && disabled)
        AppTexts(
            modifier = Modifier.weight(1f),
            pkg = pkg,
            name = name,
            disabled = disabled,
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
        AppActionMenu(
            title = name,
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            onAction = {
                menuExpanded = false
                onAction(it)
            },
        )
    }
}

@Composable
private fun AppIcon(
    info: ApplicationInfo,
    dimmed: Boolean,
) {
    val context = LocalContext.current
    val iconSize = dimensionResource(R.dimen.app_icon_size)
    val iconSizePx = with(LocalDensity.current) { iconSize.roundToPx() }
    val icon by produceState<Bitmap?>(null, info.packageName, HailData.synthesizeAdaptiveIcons, iconSizePx) {
        value = AppIconCache.loadIconBitmap(context, info, HPackages.myUserId, iconSizePx)
    }
    val fallbackIcon = remember { context.packageManager.defaultActivityIcon?.toBitmap() }
    val displayedIcon = icon ?: fallbackIcon
    val iconModifier =
        Modifier
            .size(iconSize)
            .padding(dimensionResource(R.dimen.padding_small))
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
    pkg: String,
    name: String,
    disabled: Boolean,
    grayscale: Boolean,
) {
    val dimmed = grayscale && disabled
    val textColor =
        when {
            HPackages.isAppUninstalled(pkg) -> MaterialTheme.colorScheme.error
            dimmed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            else -> MaterialTheme.colorScheme.onSurface
        }
    val descColor =
        if (dimmed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        Text(
            text = if (!grayscale && disabled) "❄️$name" else name,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = pkg,
            style = MaterialTheme.typography.bodySmall,
            color = descColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppActionMenu(
    title: String,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onAction: (AppAction) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_details)) },
            onClick = { onAction(AppAction.DETAILS) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_export_clipboard)) },
            onClick = { onAction(AppAction.COPY) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_extract_apk)) },
            onClick = { onAction(AppAction.EXTRACT) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_uninstall)) },
            onClick = { onAction(AppAction.UNINSTALL) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_reinstall)) },
            onClick = { onAction(AppAction.REINSTALL) },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AppItemPreview() {
    AppTheme {
        AppItem(
            info = ApplicationInfo().apply { packageName = "com.example.app" },
            revision = 0,
            onCheckedChange = {},
            onAction = {},
        )
    }
}
