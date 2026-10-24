package com.aistra.hail.ui.actions

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.databinding.FragmentActionsBinding
import com.aistra.hail.extensions.applyDefaultInsetter
import com.aistra.hail.extensions.isLandscape
import com.aistra.hail.extensions.isRtl
import com.aistra.hail.extensions.paddingRelative
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.utils.ActionExecutor
import com.aistra.hail.utils.ActionsRepository
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.HUI
import com.aistra.hail.utils.LaunchAction
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActionsFragment : MainFragment() {
    private var _binding: FragmentActionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ActionsAdapter
    private var actions: List<LaunchAction> = emptyList()

    private const val ACTION_MENU_DELETE = 3
    private const val EDITOR_PADDING_HORIZONTAL = 32
    private const val APP_PICKER_GRID_SPAN_COUNT = 4
    private const val PICKER_LIST_HEIGHT = 176
    private const val PICKER_CONTAINER_PADDING_HORIZONTAL = 24
    private const val PICKER_CONTAINER_PADDING_VERTICAL = 8
    private const val PICKER_CARD_RADIUS = 48f
    private const val PICKER_CARD_CONTENT_PADDING_HORIZONTAL = 20
    private const val PICKER_SEARCH_MIN_HEIGHT = 64
    private const val PICKER_SEARCH_CONTAINER_HEIGHT = 72
    private const val PICKER_SEARCH_CONTAINER_BOTTOM_MARGIN = 16

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentActionsBinding.inflate(inflater, container, false)
        adapter = ActionsAdapter(::execute, ::showActionMenu)
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.applyDefaultInsetter { paddingRelative(isRtl, bottom = isLandscape) }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadActions()
    }

    private fun loadActions() = viewLifecycleOwner.lifecycleScope.launch {
        actions = ActionsRepository.loadAll()
        adapter.submitList(actions)
    }

    private fun execute(action: LaunchAction) = viewLifecycleOwner.lifecycleScope.launch {
        ActionExecutor.prepare(action).onSuccess { startActivity(it) }
            .onFailure { HUI.showToast(it.message ?: getString(R.string.action_unavailable), true) }
    }

    private fun showActionMenu(action: LaunchAction) {
        MaterialAlertDialogBuilder(activity).setTitle(action.launchPackage).setItems(
            arrayOf(
                getString(R.string.action_edit_action),
                getString(R.string.action_create_shortcut),
                getString(R.string.action_duplicate),
                getString(R.string.action_delete),
            ),
        ) { _, which ->
            when (which) {
                0 -> showEditor(action)

                1 -> HShortcuts.addActionShortcut(action)

                2 -> viewLifecycleOwner.lifecycleScope.launch {
                    ActionsRepository.duplicate(action)
                    loadActions()
                }

                ACTION_MENU_DELETE -> confirmDelete(action)
            }
        }.show()
    }

    private fun confirmDelete(action: LaunchAction) {
        MaterialAlertDialogBuilder(activity).setTitle(R.string.action_delete)
            .setMessage(action.launchPackage)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    ActionsRepository.delete(action.id)
                    loadActions()
                }
            }.show()
    }

    fun showEditor(existing: LaunchAction?) {
        val selectedDependencies = existing?.unfreezePackages?.toMutableSet() ?: mutableSetOf()
        var selectedLaunch = existing?.launchPackage
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(EDITOR_PADDING_HORIZONTAL, 0, EDITOR_PADDING_HORIZONTAL, 0)
        }
        val unfreeze = MaterialButton(activity).apply { text = getString(R.string.action_select_unfreeze) }
        val launch = MaterialButton(activity).apply { text = getString(R.string.action_select_launch) }
        content.addView(unfreeze)
        content.addView(launch)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(if (existing == null) R.string.action_create_action else R.string.action_edit_action)
            .setView(content)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()

        fun updateLabels() {
            unfreeze.text = selectedDependencies.joinToString(", ") { AppInfo(it).name }.ifEmpty {
                getString(R.string.action_select_unfreeze)
            }
            launch.text = selectedLaunch?.let { AppInfo(it).name } ?: getString(R.string.action_select_launch)
        }
        unfreeze.setOnClickListener {
            showAppPicker(true, selectedDependencies) { selected ->
                selectedDependencies.clear()
                selectedDependencies.addAll(selected)
                updateLabels()
            }
        }
        launch.setOnClickListener {
            showAppPicker(false, selectedLaunch?.let(::setOf).orEmpty()) { selected ->
                selectedLaunch = selected.firstOrNull()
                updateLabels()
            }
        }
        dialog.setOnShowListener {
            dialog.getButton(-1).setOnClickListener {
                if (selectedDependencies.isEmpty()) {
                    HUI.showToast(R.string.action_required_unfreeze)
                    return@setOnClickListener
                }
                if (selectedLaunch == null) return@setOnClickListener
                val finalDependencies = selectedDependencies.filterNot { it == selectedLaunch }.distinct()
                if (finalDependencies.isEmpty()) {
                    HUI.showToast(R.string.action_required_unfreeze)
                    return@setOnClickListener
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val savedAction = ActionsRepository.save(
                        existing?.id ?: java.util.UUID.randomUUID().toString(),
                        selectedLaunch!!,
                        finalDependencies,
                    )
                    HShortcuts.updateActionShortcut(savedAction)
                    dialog.dismiss()
                    loadActions()
                }
            }
        }
        updateLabels()
        dialog.show()
    }

    private fun showAppPicker(multi: Boolean, selected: Set<String>, onSelected: (Set<String>) -> Unit) {
        val selectedPackages = selected.toMutableSet()
        val search = EditText(activity).apply {
            hint = getString(R.string.action_search_apps)
            setSingleLine(true)
            background = null
            minHeight = PICKER_SEARCH_MIN_HEIGHT
            setPadding(0, 0, 0, 0)
        }
        val list = RecyclerView(activity).apply {
            layoutManager = GridLayoutManager(activity, APP_PICKER_GRID_SPAN_COUNT)
            layoutParams = LinearLayout.LayoutParams(-1, PICKER_LIST_HEIGHT)
        }
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                PICKER_CONTAINER_PADDING_HORIZONTAL,
                PICKER_CONTAINER_PADDING_VERTICAL,
                PICKER_CONTAINER_PADDING_HORIZONTAL,
                PICKER_CONTAINER_PADDING_VERTICAL,
            )
            val searchContainer = com.google.android.material.card.MaterialCardView(activity).apply {
                radius = PICKER_CARD_RADIUS
                cardElevation = 0f
                setContentPadding(PICKER_CARD_CONTENT_PADDING_HORIZONTAL, 0, PICKER_CARD_CONTENT_PADDING_HORIZONTAL, 0)
                addView(search, LinearLayout.LayoutParams(-1, PICKER_SEARCH_MIN_HEIGHT))
            }
            addView(
                searchContainer,
                LinearLayout.LayoutParams(-1, PICKER_SEARCH_CONTAINER_HEIGHT).apply {
                    bottomMargin = PICKER_SEARCH_CONTAINER_BOTTOM_MARGIN
                },
            )
            addView(list)
        }
        lateinit var pickerDialog: AlertDialog
        val adapter = AppPickerAdapter(multi, selectedPackages) { packageName ->
            if (!multi) {
                onSelected(setOf(packageName))
            }
        }
        list.adapter = adapter
        var allApps: List<ApplicationInfo> = emptyList()
        var visibleApps: List<ApplicationInfo> = emptyList()
        var labelMap: Map<String, String> = emptyMap()
        fun updateFilter(query: String) {
            visibleApps = if (query.isBlank()) {
                allApps
            } else {
                allApps.filter {
                    labelMap[it.packageName]?.contains(query, true) == true || it.packageName.contains(query, true)
                }
            }
            adapter.submitList(visibleApps)
        }
        pickerDialog = MaterialAlertDialogBuilder(activity)
            .setTitle(if (multi) R.string.action_select_unfreeze else R.string.action_select_launch)
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .apply {
                setPositiveButton(android.R.string.ok) { _, _ -> onSelected(selectedPackages) }
            }.create()
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
                updateFilter(s.toString())
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        pickerDialog.show()
        viewLifecycleOwner.lifecycleScope.launch {
            // Capture PackageManager on the main thread (requireContext() is non-null or throws)
            val pm = requireContext().packageManager

            // Do the heavy IO work off the main thread, but don't call Fragment APIs from IO
            val (sortedApps, labels) = withContext(kotlinx.coroutines.Dispatchers.IO) {
                // Load each label once: loadLabel() hits PackageManager, and the
                // old code called it again per app while building the label map.
                val labeled = AppMetaCache.getInstalledApplicationsCacheFirst()
                    .map { it to it.loadLabel(pm).toString() }
                val sorted = labeled.sortedBy { (_, label) -> label }
                sorted.map { it.first } to sorted.associate { (app, label) -> app.packageName to label }
            }

            // Update UI state on the main thread
            allApps = sortedApps
            labelMap = labels
            updateFilter(search.text.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
