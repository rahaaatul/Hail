package com.aistra.hail.ui.apps

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aistra.hail.BuildConfig
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.extensions.isLandscape
import com.aistra.hail.extensions.isRtl
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.AppActions
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.HFiles
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.HUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.FileInputStream

@Composable
fun AppsScreen(
    viewModel: AppsViewModel,
    activity: Activity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRtl = isRtl
    val isLandscape = isLandscape

    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val displayApps by viewModel.displayApps.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    var contextMenuInfo by remember { mutableStateOf<ApplicationInfo?>(null) }
    var pendingExportPkg by remember { mutableStateOf<String?>(null) }

    val exportApkLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")) { uri ->
        val pkg = pendingExportPkg ?: return@rememberLauncherForActivityResult
        pendingExportPkg = null
        activity.lifecycleScope.launch {
            val applicationInfo = HPackages.getApplicationInfoOrNull(pkg) ?: return@launch
            val dialog =
                MaterialAlertDialogBuilder(activity).setView(R.layout.dialog_progress).setCancelable(false).show()
            runCatching {
                withContext(Dispatchers.IO) {
                    FileInputStream(applicationInfo.sourceDir).use { source ->
                        activity.contentResolver.openOutputStream(uri, "rwt").use { target ->
                            if (target == null) return@withContext
                            HFiles.copy(source, target)
                        }
                    }
                }
            }.onSuccess {
                HUI.showToast(R.string.msg_extract_apk, uri.toString())
            }.onFailure {
                HUI.showToast(R.string.operation_failed, it.localizedMessage ?: "Unknown", true)
            }
            dialog.dismiss()
        }
    }

    val pullRefreshState = remember {
        androidx.compose.material3.PullRefreshState(
            refreshing = isRefreshing,
            onRefresh = { viewModel.updateAppList(true) }
        )
    }

    val pullRefresh = remember {
        androidx.compose.material3.PullRefresh(
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .paddingRelative(isRtl, bottom = if (isLandscape) 16.dp else 0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (displayApps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (query.isNotBlank()) stringResource(R.string.no_items_to_select) else stringResource(R.string.nothing_here),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    AppGrid(
                        apps = displayApps,
                        onItemClick = { info ->
                            if (info.packageName == BuildConfig.APPLICATION_ID) return@AppGrid
                            launchApp(activity, info.packageName)
                        },
                        onItemLongClick = { info ->
                            contextMenuInfo = info
                            true
                        },
                        onItemCheckedChange = { info, checked ->
                            if (checked) HailData.addCheckedApp(info.packageName)
                            else HailData.removeCheckedApp(info.packageName)
                        },
                        isRefreshing = isRefreshing
                    )
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        pullRefresh
    }

    contextMenuInfo?.let { info ->
        val name = AppMetaCache.get(info.packageName)?.name ?: info.packageName
        val pkg = info.packageName
        AlertDialog(
            title = { Text(text = name) },
            text = {
                Column {
                    TextButton(onClick = {
                        HUI.startActivity(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, HPackages.packageUri(pkg))
                        contextMenuInfo = null
                    }) {
                        Text(text = stringResource(R.string.action_details))
                    }
                    TextButton(onClick = {
                        HUI.copyText(pkg)
                        HUI.showToast(R.string.msg_text_copied, pkg)
                        contextMenuInfo = null
                    }) {
                        Text(text = stringResource(R.string.action_export_clipboard))
                    }
                    TextButton(onClick = {
                        pendingExportPkg = pkg
                        exportApkLauncher.launch(HPackages.getUnhiddenPackageInfoOrNull(pkg)?.exportFileName ?: pkg)
                        contextMenuInfo = null
                    }) {
                        Text(text = stringResource(R.string.action_extract_apk))
                    }
                    TextButton(onClick = {
                        uninstallApp(activity, name, pkg, viewModel)
                        contextMenuInfo = null
                    }) {
                        Text(text = stringResource(R.string.action_uninstall))
                    }
                    TextButton(onClick = {
                        if (AppManager.reinstallApp(pkg)) viewModel.updateAppList(true)
                        else HUI.showToast(R.string.operation_failed, name)
                        contextMenuInfo = null
                    }) {
                        Text(text = stringResource(R.string.action_reinstall))
                    }
                }
            },
            onDismissRequest = { contextMenuInfo = null }
        )
    }

    LaunchedEffect(viewModel) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                AppMetaCache.revision.collect { viewModel.updateDisplayAppList() }
            }
        }
    }
}

private fun launchApp(activity: Activity, packageName: String) {
    activity.lifecycleScope.launch {
        if (AppManager.isAppFrozen(packageName)) {
            AppActions.ensureUnfrozen(packageName).onSuccess {
                AppActions.getLaunchIntent(packageName).getOrNull()?.let {
                    activity.startActivity(it)
                }
            }.onFailure {
                HUI.showToast(R.string.activity_not_found)
            }
        } else {
            AppActions.getLaunchIntent(packageName).onSuccess { intent ->
                HShortcuts.addDynamicShortcut(packageName)
                activity.startActivity(intent)
            }.onFailure {
                HUI.showToast(R.string.activity_not_found)
            }
        }
    }
}

private fun uninstallApp(activity: Activity, name: CharSequence, pkg: String, viewModel: AppsViewModel) {
    when {
        HPackages.isAppUninstalled(pkg) -> HUI.showToast(R.string.app_not_installed)

        pkg == BuildConfig.APPLICATION_ID -> {
            when {
                HPolicy.isDeviceOwnerActive -> (activity as? MainActivity)?.ownerRemoveDialog()
                HPolicy.isProfileOwner -> HPolicy.removeProfileOwner()
                HPolicy.isAdminActive -> HPolicy.removeActiveAdmin()
                else -> showUninstallDialog(activity, name, pkg, viewModel)
            }
        }

        HailData.workingMode == HailData.MODE_DEFAULT -> {
            AppManager.uninstallApp(pkg)
            viewModel.updateAppList(true)
        }
        else -> showUninstallDialog(activity, name, pkg, viewModel)
    }
}

private fun showUninstallDialog(activity: Activity, name: CharSequence, pkg: String, viewModel: AppsViewModel) {
    MaterialAlertDialogBuilder(activity).setTitle(name).setMessage(R.string.msg_uninstall)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            if (AppManager.uninstallApp(pkg)) {
                viewModel.updateAppList(true)
            }
        }.setNegativeButton(android.R.string.cancel, null).show()
}
