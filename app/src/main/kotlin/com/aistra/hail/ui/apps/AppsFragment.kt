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

    // Hoisted UI state shared with the Compose list (checked set, filter sheet, empty toast).
    private val ui = AppsUiState()

    // Prevent the same data from being filtered twice in `onCreateView`
    private var lastAppsHash: Int = 0
    private lateinit var lastQuery: String

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

        model.apps.apply {
            lastAppsHash = value.hashCode()
            observe(viewLifecycleOwner) {
                if (it.hashCode() != lastAppsHash) model.updateDisplayAppList()
                lastAppsHash = it.hashCode()
            }
        }
        model.query.apply {
            lastQuery = value ?: ""
            observe(viewLifecycleOwner) {
                if (it != lastQuery) model.updateDisplayAppList()
                lastQuery = it
            }
        }
        model.displayApps.observe(viewLifecycleOwner) {
            val empty = it.isNullOrEmpty()
            // Consume the one-shot flag: surface an informational toast when a user-triggered
            // refresh yields no apps matching the selected filters.
            if (ui.showEmptyToast) {
                ui.showEmptyToast = false
                if (empty) {
                    HUI.showToast(R.string.msg_no_apps_match)
                }
            }
        }
        model.isRefreshing.observe(viewLifecycleOwner) { refreshing ->
            if (refreshing) ui.showEmptyToast = true
        }

        // Background reload: whenever the app metadata cache revision changes
        // (app state changed via freeze/enable/disable/etc.), re-filter the displayed list.
        viewLifecycleOwner.lifecycleScope.launch {
            AppMetaCache.revision.collect { _ ->
                model.updateDisplayAppList()
            }
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    AppsScreen(
                        model = model,
                        ui = ui,
                        onToggleChecked = { pkg, checked ->
                            if (checked) {
                                HailData.addCheckedApp(pkg)
                            } else {
                                HailData.removeCheckedApp(pkg)
                            }
                            ui.checkedRevision++
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
        ui.showEmptyToast = true
        model.updateAppList(true)
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
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_select_all -> {
                toggleSelectAll()
                true
            }

            R.id.sort_by_name -> {
                changeAppsSort(HailData.SORT_NAME, item)
                true
            }

            R.id.sort_by_install -> {
                changeAppsSort(HailData.SORT_INSTALL, item)
                true
            }

            R.id.sort_by_update -> {
                changeAppsSort(HailData.SORT_UPDATE, item)
                true
            }

            R.id.filter_by -> {
                ui.filterSheetVisible = true
                true
            }

            else -> {
                false
            }
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
        ui.checkedRevision++
    }

    private fun changeAppsSort(
        sort: String,
        item: MenuItem,
    ) {
        item.isChecked = true
        HailData.changeAppsSort(sort)
        model.updateDisplayAppList()
    }

    private fun updateAppList(forceRefresh: Boolean) = model.updateAppList(forceRefresh)
}
