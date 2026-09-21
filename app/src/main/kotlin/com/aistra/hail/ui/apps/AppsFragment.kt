package com.aistra.hail.ui.apps

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import com.aistra.hail.extensions.*
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

class AppsFragment : MainFragment(), MenuProvider {

    private val model: AppsViewModel by viewModels()

    private var exportApkPkg: String? = null
    private val exportApk =
        registerForActivityResult(CreateDocument("application/vnd.android.package-archive")) { uri ->
        val pkg = exportApkPkg
        this.exportApkPkg = null
        if (pkg == null || uri == null) return@registerForActivityResult
            lifecycleScope.launch {
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

    override fun onCreateView(
        inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    AppsScreen(model, activity)
                }
            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onResume() {
        super.onResume()
        model.updateAppList()
    }

    private fun extractApk(pkg: String) {
        exportApkPkg = pkg
        exportApk.launch(HPackages.getUnhiddenPackageInfoOrNull(pkg)?.exportFileName ?: pkg)
    }

    private fun uninstallApp(name: CharSequence, pkg: String) {
        when {
            HPackages.isAppUninstalled(pkg) -> HUI.showToast(R.string.app_not_installed)

            pkg == BuildConfig.APPLICATION_ID -> {
                when {
                    HPolicy.isDeviceOwnerActive -> activity.ownerRemoveDialog()
                    HPolicy.isProfileOwner -> HPolicy.removeProfileOwner()
                    HPolicy.isAdminActive -> HPolicy.removeActiveAdmin()
                    else -> showUninstallDialog(name, pkg)
                }
            }

            HailData.workingMode == HailData.MODE_DEFAULT -> AppManager.uninstallApp(pkg)
            else -> showUninstallDialog(name, pkg)
        }
    }

    private fun showUninstallDialog(name: CharSequence, pkg: String) {
        MaterialAlertDialogBuilder(activity).setTitle(name).setMessage(R.string.msg_uninstall)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (AppManager.uninstallApp(pkg)) model.updateAppList(true)
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    override fun onCreateMenu(menu: android.view.Menu, inflater: android.view.MenuInflater) {
        inflater.inflate(R.menu.menu_apps, menu)
        val searchView = menu.findItem(R.id.action_search).actionView as androidx.appcompat.widget.SearchView
        if (HailData.nineKeySearch) {
            val editText = searchView.findViewById<android.widget.EditText>(androidx.appcompat.R.id.search_src_text)
            editText.inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                model.postQuery(newText, if (newText.isEmpty()) 0L else 300L)
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                model.postQuery(query, 0L)
                return true
            }
        })
    }

    override fun onPrepareMenu(menu: android.view.Menu) {
        super.onPrepareMenu(menu)
        menu.findItem(
            when (HailData.sortBy) {
                HailData.SORT_INSTALL -> R.id.sort_by_install
                HailData.SORT_UPDATE -> R.id.sort_by_update
                else -> R.id.sort_by_name
            }
        ).isChecked = true
        menu.findItem(
            if (HailData.filterAllApps) R.id.filter_all_apps
            else if (HailData.filterSystemApps) R.id.filter_system_apps
            else R.id.filter_user_apps
        ).isChecked = true
        menu.findItem(R.id.filter_frozen_apps).isChecked = HailData.filterFrozenApps
        menu.findItem(R.id.filter_unfrozen_apps).isChecked = HailData.filterUnfrozenApps
    }

    override fun onMenuItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_select_all -> {
                val displayedApps = model.displayApps.value
                val selfPkg = BuildConfig.APPLICATION_ID
                val allChecked = displayedApps.isNotEmpty() && displayedApps.all { it.packageName == selfPkg || HailData.isChecked(it.packageName) }
                if (allChecked) {
                    displayedApps.forEach { if (it.packageName != selfPkg) HailData.removeCheckedApp(it.packageName, false) }
                } else {
                    displayedApps.forEach { if (it.packageName != selfPkg) HailData.addCheckedApp(it.packageName, 0, false) }
                }
                HailData.saveApps()
                return true
            }
            R.id.sort_by_name -> changeAppsSort(HailData.SORT_NAME, item)
            R.id.sort_by_install -> changeAppsSort(HailData.SORT_INSTALL, item)
            R.id.sort_by_update -> changeAppsSort(HailData.SORT_UPDATE, item)
            R.id.filter_user_apps -> changeAppsFilter(HailData.FILTER_USER_APPS, item)
            R.id.filter_system_apps -> changeAppsFilter(HailData.FILTER_SYSTEM_APPS, item)
            R.id.filter_all_apps -> changeAppsFilter(HailData.FILTER_ALL_APPS, item)

            R.id.filter_frozen_apps -> changeAppsFilter(HailData.FILTER_FROZEN_APPS, item)
            R.id.filter_unfrozen_apps -> changeAppsFilter(HailData.FILTER_UNFROZEN_APPS, item)
        }
        return false
    }

    private fun changeAppsSort(sort: String, item: android.view.MenuItem) {
        item.isChecked = true
        HailData.changeAppsSort(sort)
        model.updateDisplayAppList()
    }

    private fun changeAppsFilter(filter: String, item: android.view.MenuItem) {
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

    override fun onDestroy() {
        super.onDestroy()
    }
}
