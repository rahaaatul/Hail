package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import androidx.annotation.StringRes
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.aistra.hail.R
import com.aistra.hail.ui.components.HailBottomSheet
import com.aistra.hail.utils.AppMetaCache

@Composable
fun AppActionsSheet(
    info: ApplicationInfo,
    onDismissRequest: () -> Unit,
    onAction: (AppAction) -> Unit,
) {
    val pkg = info.packageName
    val name = AppMetaCache.get(pkg)?.name ?: pkg
    HailBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            AppActionsHeader(info = info, name = name, pkg = pkg)
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.space300)))
        AppActionItems.forEach { item ->
            ListItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAction(item.action)
                            onDismissRequest()
                        },
                leadingContent = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                content = { Text(text = stringResource(item.labelRes), style = MaterialTheme.typography.bodyLarge) },
                contentPadding =
                    PaddingValues(
                        start = dimensionResource(R.dimen.space300),
                        top = dimensionResource(R.dimen.space50),
                        end = dimensionResource(R.dimen.space300),
                        bottom = dimensionResource(R.dimen.space50),
                    ),
            )
        }
    }
}

@Composable
private fun AppActionsHeader(
    info: ApplicationInfo,
    name: String,
    pkg: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.space300),
                    vertical = dimensionResource(R.dimen.space300),
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space200)),
    ) {
        AppIcon(
            info = info,
            dimmed = false,
            shape = RoundedCornerShape(dimensionResource(R.dimen.space200)),
            size = dimensionResource(R.dimen.space900),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .height(dimensionResource(R.dimen.space700)),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
            Text(
                text = pkg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
        }
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
        AppActionItem(AppAction.COPY, R.string.action_export_clipboard, Assignment),
        AppActionItem(AppAction.EXTRACT, R.string.action_extract_apk, Download),
        AppActionItem(AppAction.UNINSTALL, R.string.action_uninstall, Delete),
        AppActionItem(AppAction.REINSTALL, R.string.action_reinstall, Autorenew),
    )
