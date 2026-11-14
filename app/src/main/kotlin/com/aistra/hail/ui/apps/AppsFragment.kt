package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.aistra.hail.BuildConfig
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.extensions.exportFileName
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.HFiles
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream

class AppsFragment :
    MainFragment(),
    MenuProvider {
    private val model: AppsViewModel by viewModels()

    // Bumped whenever the checked set changes so the Compose list re-reads HailData.
    private var checkedRevision by mutableStateOf(0)

    private var exportApkPkg: String? = null
    private val exportApk =
        registerForActivityResult(CreateDocument("application/vnd.android.package-archive")) { uri ->
            val exportApkPkg = exportApkPkg
            this.exportApkPkg = null
            if (exportApkPkg == null || uri == null) return@registerForActivityResult
            lifecycleScope.launch {
                val applicationInfo = HPackages.getApplicationInfoOrNull(exportApkPkg) ?: return@launch
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // Re-filter the displayed list whenever the installed apps or the search query change.
        model.apps.observe(viewLifecycleOwner) { model.updateDisplayAppList() }
        model.query.observe(viewLifecycleOwner) { model.updateDisplayAppList() }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    AppsScreen(
                        model = model,
                        checkedRevision = checkedRevision,
                        onToggleChecked = { pkg, checked ->
                            if (checked) {
                                HailData.addCheckedApp(pkg)
                            } else {
                                HailData.removeCheckedApp(pkg)
                            }
                            checkedRevision++
                        },
                        onAction = ::onAppAction,
                        onLiftChange = { lifted -> activity.appbar.setLifted(lifted) },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.updateAppList()
    }

    private fun onAppAction(
        info: ApplicationInfo,
        action: AppAction,
    ) {
        val name = info.loadLabel(app.packageManager)
        val pkg = info.packageName
        when (action) {
            AppAction.DETAILS -> {
                HUI.startActivity(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    HPackages.packageUri(pkg),
                )
            }

            AppAction.COPY -> {
                HUI.copyText(pkg)
                HUI.showToast(R.string.msg_text_copied, pkg)
            }

            AppAction.EXTRACT -> {
                exportApkPkg = pkg
                exportApk.launch(HPackages.getUnhiddenPackageInfoOrNull(pkg)?.exportFileName ?: pkg)
            }

            AppAction.UNINSTALL -> {
                uninstallApp(name, pkg)
            }

            AppAction.REINSTALL -> {
                if (AppManager.reinstallApp(pkg)) {
                    updateAppList(true)
                } else {
                    HUI.showToast(R.string.operation_failed, name)
                }
            }
        }
    }

    private fun uninstallApp(
        name: CharSequence,
        pkg: String,
    ) {
        when {
            HPackages.isAppUninstalled(pkg) -> {
                HUI.showToast(R.string.app_not_installed)
            }

            pkg == app.packageName -> {
                when {
                    HPolicy.isDeviceOwnerActive -> activity.ownerRemoveDialog()
                    HPolicy.isProfileOwner -> HPolicy.removeProfileOwner()
                    HPolicy.isAdminActive -> HPolicy.removeActiveAdmin()
                    else -> showUninstallDialog(name, pkg)
                }
            }

            HailData.workingMode == HailData.MODE_DEFAULT -> {
                AppManager.uninstallApp(pkg)
            }

            else -> {
                showUninstallDialog(name, pkg)
            }
        }
    }

    private fun showUninstallDialog(
        name: CharSequence,
        pkg: String,
    ) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(name)
            .setMessage(R.string.msg_uninstall)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (AppManager.uninstallApp(pkg)) updateAppList(true)
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onCreateMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
        inflater.inflate(R.menu.menu_apps, menu)
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        if (HailData.nineKeySearch) {
            val editText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
            editText.inputType = InputType.TYPE_CLASS_PHONE
        }
        searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {
                    model.postQuery(newText, if (newText.isEmpty()) 0L else 300L)
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    model.postQuery(query, 0L)
                    return true
                }
            },
        )
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        menu
            .findItem(
                when (HailData.sortBy) {
                    HailData.SORT_INSTALL -> R.id.sort_by_install
                    HailData.SORT_UPDATE -> R.id.sort_by_update
                    else -> R.id.sort_by_name
                },
            ).isChecked = true
        menu
            .findItem(
                if (HailData.filterAllApps) {
                    R.id.filter_all_apps
                } else if (HailData.filterSystemApps) {
                    R.id.filter_system_apps
                } else {
                    R.id.filter_user_apps
                },
            ).isChecked = true
        menu.findItem(R.id.filter_frozen_apps).isChecked = HailData.filterFrozenApps
        menu.findItem(R.id.filter_unfrozen_apps).isChecked = HailData.filterUnfrozenApps
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_select_all -> {
                toggleSelectAll()
                return true
            }

            R.id.sort_by_name -> {
                changeAppsSort(HailData.SORT_NAME, item)
            }

            R.id.sort_by_install -> {
                changeAppsSort(HailData.SORT_INSTALL, item)
            }

            R.id.sort_by_update -> {
                changeAppsSort(HailData.SORT_UPDATE, item)
            }

            R.id.filter_user_apps -> {
                changeAppsFilter(HailData.FILTER_USER_APPS, item)
            }

            R.id.filter_system_apps -> {
                changeAppsFilter(HailData.FILTER_SYSTEM_APPS, item)
            }

            R.id.filter_all_apps -> {
                changeAppsFilter(HailData.FILTER_ALL_APPS, item)
            }

            R.id.filter_frozen_apps -> {
                changeAppsFilter(HailData.FILTER_FROZEN_APPS, item)
            }

            R.id.filter_unfrozen_apps -> {
                changeAppsFilter(HailData.FILTER_UNFROZEN_APPS, item)
            }
        }
        return false
    }

    private fun toggleSelectAll() {
        val displayedApps = model.displayApps.value.orEmpty()
        val selfPkg = BuildConfig.APPLICATION_ID
        val allChecked = displayedApps.isNotEmpty() && displayedApps.all { it.packageName == selfPkg || HailData.isChecked(it.packageName) }
        if (allChecked) {
            displayedApps.forEach { if (it.packageName != selfPkg) HailData.removeCheckedApp(it.packageName, false) }
        } else {
            displayedApps.forEach { if (it.packageName != selfPkg) HailData.addCheckedApp(it.packageName, 0, false) }
        }
        HailData.saveApps()
        checkedRevision++
    }

    private fun changeAppsSort(
        sort: String,
        item: MenuItem,
    ) {
        item.isChecked = true
        HailData.changeAppsSort(sort)
        model.updateDisplayAppList()
    }

    private fun changeAppsFilter(
        filter: String,
        item: MenuItem,
    ) {
        when (item.itemId) {
            R.id.filter_all_apps -> {
                item.isChecked = true
                HailData.changeAppsFilter(HailData.FILTER_ALL_APPS, true)
                HailData.changeAppsFilter(HailData.FILTER_USER_APPS, true)
                HailData.changeAppsFilter(HailData.FILTER_SYSTEM_APPS, true)
            }

            R.id.filter_user_apps -> {
                item.isChecked = true
                HailData.changeAppsFilter(filter, item.isChecked)
                HailData.changeAppsFilter(HailData.FILTER_SYSTEM_APPS, false)
                HailData.changeAppsFilter(HailData.FILTER_ALL_APPS, false)
            }

            R.id.filter_system_apps -> {
                item.isChecked = true
                HailData.changeAppsFilter(filter, item.isChecked)
                HailData.changeAppsFilter(HailData.FILTER_USER_APPS, false)
                HailData.changeAppsFilter(HailData.FILTER_ALL_APPS, false)
            }

            else -> {
                item.isChecked = !item.isChecked
                HailData.changeAppsFilter(filter, item.isChecked)
            }
        }
        model.updateDisplayAppList()
    }

    private fun updateAppList(forceRefresh: Boolean) = model.updateAppList(forceRefresh)
}
