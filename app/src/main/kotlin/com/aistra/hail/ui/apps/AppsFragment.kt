package com.aistra.hail.ui.apps

import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.aistra.hail.BuildConfig
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.FragmentAppsBinding
import com.aistra.hail.extensions.applyDefaultInsetter
import com.aistra.hail.extensions.exportFileName
import com.aistra.hail.extensions.isLandscape
import com.aistra.hail.extensions.isRtl
import com.aistra.hail.extensions.marginRelative
import com.aistra.hail.extensions.paddingRelative
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.HFiles
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HUI
import com.aistra.hail.views.HRecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream

class AppsFragment :
    MainFragment(),
    AppsAdapter.OnItemClickListener,
    AppsAdapter.OnItemCheckedChangeListener,
    MenuProvider {
    private val model: AppsViewModel by viewModels()

    private var bindingView: FragmentAppsBinding? = null
    private val binding get() = bindingView!!
    private lateinit var appsAdapter: AppsAdapter

    // Bottom sheet that shows the filter options and stays open across toggles.
    private var filterBottomSheet: BottomSheetDialog? = null

    // Prevent the same data from being filtered twice in `onCreateView`
    private var lastAppsHash: Int = 0
    private lateinit var lastQuery: String
    private val isAppsChanged get() = model.apps.value.hashCode() != lastAppsHash
    private val isQueryChanged get() = model.query.value != lastQuery
    private var contextMenuInfo: ContextMenu.ContextMenuInfo? = null

    // Set to true before a user-triggered (force) refresh so the next empty display list
    // surfaces an informational toast instead of silently showing nothing.
    private var showEmptyToast = false

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
        bindingView = FragmentAppsBinding.inflate(inflater, container, false)
        appsAdapter =
            AppsAdapter().apply {
                onItemClickListener = this@AppsFragment
                onItemCheckedChangeListener = this@AppsFragment
            }
        binding.refresh.apply {
            setOnRefreshListener {
                showEmptyToast = true
                model.updateAppList(true)
            }
            applyDefaultInsetter { marginRelative(isRtl, start = !isLandscape, end = true) }
        }
        binding.recyclerView.apply {
            activity.appbar.setLiftOnScrollTargetView(this)
            layoutManager = GridLayoutManager(activity, resources.getInteger(R.integer.apps_span))
            adapter = appsAdapter
            applyDefaultInsetter { paddingRelative(isRtl, bottom = isLandscape) }
            registerForContextMenu(this)
        }

        model.isRefreshing.observe(viewLifecycleOwner) {
            binding.refresh.isRefreshing = it
        }
        model.apps.apply {
            lastAppsHash = value.hashCode()
            observe(viewLifecycleOwner) {
                if (isAppsChanged) updateDisplayAppList()
                lastAppsHash = it.hashCode()
            }
        }
        model.query.apply {
            lastQuery = value ?: ""
            observe(viewLifecycleOwner) {
                if (isQueryChanged) updateDisplayAppList()
                lastQuery = it
            }
        }
        model.displayApps.observe(viewLifecycleOwner) {
            val empty = it.isNullOrEmpty()
            binding.empty.isVisible = empty
            // Consume the one-shot flag: surface an informational toast when a user-triggered
            // refresh yields no apps matching the selected filters.
            if (showEmptyToast) {
                showEmptyToast = false
                if (empty) {
                    HUI.showToast(R.string.msg_no_apps_match)
                }
            }
            appsAdapter.submitList(it)
        }

        // Background reload: whenever the app metadata cache revision changes
        // (app state changed via freeze/enable/disable/etc.), re-filter the displayed list.
        viewLifecycleOwner.lifecycleScope.launch {
            AppMetaCache.revision.collect { _ ->
                updateDisplayAppList()
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        showEmptyToast = true
        model.updateAppList(true)
    }

    override fun onItemClick(buttonView: CompoundButton) {
//        buttonView.toggle()
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?,
    ) {
        contextMenuInfo = menuInfo
        val viewHolder = ((menuInfo as HRecyclerView.RecyclerViewContextMenuInfo).viewHolder as AppsAdapter.ViewHolder)
        val pkg = viewHolder.info.packageName
        menu.setHeaderTitle(AppMetaCache.get(pkg)?.name ?: pkg)
        activity.menuInflater.inflate(R.menu.menu_apps_action, menu)
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val viewHolder =
            ((contextMenuInfo as HRecyclerView.RecyclerViewContextMenuInfo).viewHolder as AppsAdapter.ViewHolder)
        val info = viewHolder.info
        val name = info.loadLabel(app.packageManager)
        val pkg = info.packageName
        when (item.itemId) {
            R.id.action_details -> {
                HUI.startActivity(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    HPackages.packageUri(pkg),
                )
            }

            R.id.action_export_clipboard -> {
                HUI.copyText(pkg)
                HUI.showToast(R.string.msg_text_copied, pkg)
            }

            R.id.action_extract_apk -> {
                extractApk(pkg)
            }

            R.id.action_uninstall -> {
                uninstallApp(name, pkg)
            }

            R.id.action_reinstall -> {
                if (AppManager.reinstallApp(pkg)) {
                    updateAppList(true)
                } else {
                    HUI.showToast(R.string.operation_failed, name)
                }
            }

            else -> {
                return super.onContextItemSelected(item)
            }
        }
        return true
    }

    private fun extractApk(pkg: String) {
        exportApkPkg = pkg
        exportApk.launch(HPackages.getUnhiddenPackageInfoOrNull(pkg)?.exportFileName ?: pkg)
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

    override fun onItemCheckedChange(
        buttonView: CompoundButton,
        isChecked: Boolean,
        packageName: String,
    ) {
        if (isChecked) {
            HailData.addCheckedApp(packageName)
        } else {
            HailData.removeCheckedApp(packageName)
        }
        buttonView.isChecked = HailData.isChecked(packageName)
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

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_select_all -> {
                val displayedApps = model.displayApps.value.orEmpty()
                val selfPkg = BuildConfig.APPLICATION_ID
                val allChecked = displayedApps.isNotEmpty() && displayedApps.all { it.packageName == selfPkg || HailData.isChecked(it.packageName) }
                if (allChecked) {
                    displayedApps.forEach { if (it.packageName != selfPkg) HailData.removeCheckedApp(it.packageName, false) }
                } else {
                    displayedApps.forEach { if (it.packageName != selfPkg) HailData.addCheckedApp(it.packageName, 0, false) }
                }
                HailData.saveApps()
                appsAdapter.notifyDataSetChanged()
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

            R.id.filter_by -> {
                showFilterBottomSheet()
                return true
            }
        }
        return false
    }

    /**
     * Shows the filter bottom sheet (View-based Material 3). Stays open while the user toggles
     * System/User segment buttons and status chips; dismissed via swipe-down or tapping outside.
     * Filter changes re-filter the already-loaded app list — no PackageManager reload.
     */
    private fun showFilterBottomSheet() {
        val sheet = BottomSheetDialog(requireContext(), R.style.Theme_Hail_BottomSheet)
        val view = LayoutInflater.from(requireContext()).inflate(
            R.layout.bottom_sheet_apps_filter, null, false
        )

        // ── Type segment buttons ──
        val btnSystem = view.findViewById<Button>(R.id.btn_type_system)
        val btnUser = view.findViewById<Button>(R.id.btn_type_user)

        // Sync initial state from HailData filters
        btnSystem.isSelected = HailData.filterSystemApps
        btnUser.isSelected = HailData.filterUserApps

        // Type toggles are independent — both System and User can be selected together.
        btnSystem.setOnClickListener {
            val newSystem = !btnSystem.isSelected
            HailData.changeAppsFilter(HailData.FILTER_SYSTEM_APPS, newSystem)
            btnSystem.isSelected = newSystem
            showEmptyToast = true
            updateDisplayAppList()
        }

        btnUser.setOnClickListener {
            val newUser = !btnUser.isSelected
            HailData.changeAppsFilter(HailData.FILTER_USER_APPS, newUser)
            btnUser.isSelected = newUser
            showEmptyToast = true
            updateDisplayAppList()
        }

        // ── Status chips ──
        val chipEnabled = view.findViewById<Chip>(R.id.chip_enabled)
        val chipDisabled = view.findViewById<Chip>(R.id.chip_disabled)
        val chipSuspended = view.findViewById<Chip>(R.id.chip_suspended)
        val chipHidden = view.findViewById<Chip>(R.id.chip_hidden)
        val chipStopped = view.findViewById<Chip>(R.id.chip_stopped)

        // Sync initial state from HailData filters
        chipEnabled.isChecked = HailData.filterEnabledApps
        chipDisabled.isChecked = HailData.filterDisabledApps
        chipSuspended.isChecked = HailData.filterSuspendedApps
        chipHidden.isChecked = HailData.filterHiddenApps
        chipStopped.isChecked = HailData.filterStoppedApps

        val chips = listOf(
            chipEnabled to HailData.FILTER_ENABLED_APPS,
            chipDisabled to HailData.FILTER_DISABLED_APPS,
            chipSuspended to HailData.FILTER_SUSPENDED_APPS,
            chipHidden to HailData.FILTER_HIDDEN_APPS,
            chipStopped to HailData.FILTER_STOPPED_APPS,
        )

        chips.forEach { (chip, filter) ->
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (!isChecked) {
                    // Cannot uncheck the last status chip
                    val anyOtherChecked = chips.any { (c, _) -> c != chip && c.isChecked }
                    if (!anyOtherChecked) {
                        chip.isChecked = true
                        return@setOnCheckedChangeListener
                    }
                }
                HailData.changeAppsFilter(filter, isChecked)
                showEmptyToast = true
                updateDisplayAppList()
            }
        }

        sheet.setContentView(view)
        sheet.setOnDismissListener { filterBottomSheet = null }
        filterBottomSheet = sheet
        sheet.show()
    }

    private fun changeAppsSort(
        sort: String,
        item: MenuItem,
    ) {
        item.isChecked = true
        HailData.changeAppsSort(sort)
        updateDisplayAppList()
    }

    private fun updateAppList() = model.updateAppList()

    private fun updateAppList(forceRefresh: Boolean) = model.updateAppList(forceRefresh)

    private fun updateDisplayAppList() = model.updateDisplayAppList()

    override fun onDestroy() {
        appsAdapter.onDestroy()
        super.onDestroy()
        bindingView = null
    }
}
