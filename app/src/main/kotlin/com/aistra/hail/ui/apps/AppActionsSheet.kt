package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import androidx.annotation.StringRes
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.BuildConfig
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.ui.components.HailBottomSheet
import com.aistra.hail.utils.AppMetaCache

@Composable
fun AppActionsSheet(
    info: ApplicationInfo,
    onDismissRequest: () -> Unit,
    onAction: (AppAction) -> Unit,
    onCopyText: (String) -> Unit,
    onExtractApk: () -> Unit,
) {
    val pkg = info.packageName
    val name = AppMetaCache.get(pkg)?.name ?: pkg
    val canForceStop =
        AppManager.supportsForceStop &&
            info.flags and ApplicationInfo.FLAG_INSTALLED != 0 &&
            pkg != BuildConfig.APPLICATION_ID
    HailBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        AppActionsHeader(
            info = info,
            name = name,
            pkg = pkg,
            onCopyText = onCopyText,
            onExtractApk = onExtractApk,
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.space300)))
        AppActionItems
            .filter { it.action != AppAction.FORCE_STOP || canForceStop }
            .forEach { item ->
                AppActionRow(
                    item = item,
                    onClick = {
                        onAction(item.action)
                        onDismissRequest()
                    },
                )
            }
    }
}

@Composable
private fun AppActionsHeader(
    info: ApplicationInfo,
    name: String,
    pkg: String,
    onCopyText: (String) -> Unit,
    onExtractApk: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.space300),
                    top = dimensionResource(R.dimen.space300),
                    end = dimensionResource(R.dimen.space300),
                    bottom = dimensionResource(R.dimen.space150),
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space200)),
    ) {
        AppIcon(
            info = info,
            dimmed = false,
            internalPadding = 0.dp,
            modifier =
                Modifier.combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false),
                    onClick = {},
                    onLongClick = onExtractApk,
                ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier =
                    Modifier
                        .basicMarquee()
                        .combinedClickable(onClick = {}, onLongClick = { onCopyText(name) }),
            )
            Text(
                text = pkg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier =
                    Modifier
                        .basicMarquee()
                        .combinedClickable(onClick = {}, onLongClick = { onCopyText(pkg) }),
            )
        }
    }
}

@Composable
private fun AppActionRow(
    item: AppActionItem,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.space600))
                .clickable(onClick = onClick)
                .padding(horizontal = dimensionResource(R.dimen.space300)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            modifier = Modifier.offset(x = -dimensionResource(R.dimen.space75)),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.space200)))
        Text(text = stringResource(item.labelRes), style = MaterialTheme.typography.bodyLarge)
    }
}

private data class AppActionItem(
    val action: AppAction,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

private val AppActionItems =
    listOf(
        AppActionItem(AppAction.DETAILS, R.string.action_details, Info),
        AppActionItem(AppAction.FORCE_STOP, R.string.action_force_stop, Close),
        AppActionItem(AppAction.UNINSTALL, R.string.action_uninstall, Delete),
        AppActionItem(AppAction.REINSTALL, R.string.action_reinstall, Autorenew),
    )
