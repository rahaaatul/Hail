package com.aistra.hail.ui.actions

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.databinding.FragmentActionsBinding
import com.aistra.hail.extensions.applyDefaultInsetter
import com.aistra.hail.extensions.isLandscape
import com.aistra.hail.extensions.isRtl
import com.aistra.hail.extensions.paddingRelative
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.utils.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ActionsFragment : MainFragment() {
    private var _binding: FragmentActionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ActionsAdapter
    private var actions: List<LaunchAction> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentActionsBinding.inflate(inflater, container, false)
        adapter = ActionsAdapter(::execute, ::showActionMenu)
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.applyDefaultInsetter { paddingRelative(isRtl, bottom = isLandscape) }
        activity.fab.setOnClickListener { showEditor(null) }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activity.fab.setOnClickListener { showEditor(null) }
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
                getString(R.string.action_delete)
            )
        ) { _, which ->
            when (which) {
                0 -> showEditor(action)
                1 -> HShortcuts.addActionShortcut(action)
                2 -> viewLifecycleOwner.lifecycleScope.launch {
                    ActionsRepository.duplicate(action)
                    loadActions()
                }
                3 -> confirmDelete(action)
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

    private fun showEditor(existing: LaunchAction?) {
        val selectedDependencies = existing?.unfreezePackages?.toMutableSet() ?: mutableSetOf()
        var selectedLaunch = existing?.launchPackage
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 0, 32, 0)
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
                    ActionsRepository.save(existing?.id ?: java.util.UUID.randomUUID().toString(), selectedLaunch!!, finalDependencies)
                    dialog.dismiss()
                    loadActions()
                }
            }
        }
        updateLabels()
        dialog.show()
    }

    private fun showAppPicker(multi: Boolean, selected: Set<String>, onSelected: (Set<String>) -> Unit) {
        val apps = HPackages.getInstalledApplications().sortedBy { it.loadLabel(activity.packageManager).toString() }
        val selectedPackages = selected.toMutableSet()
        val search = EditText(activity).apply {
            hint = getString(R.string.action_search_apps)
            setSingleLine(true)
            setPadding(32, 0, 32, 16)
        }
        val list = ListView(activity).apply {
            choiceMode = if (multi) ListView.CHOICE_MODE_MULTIPLE else ListView.CHOICE_MODE_SINGLE
        }
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(search)
            addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        val adapter = ArrayAdapter<String>(activity, android.R.layout.simple_list_item_multiple_choice)
        list.adapter = adapter
        var visibleApps = apps
        fun updateFilter(query: String) {
            visibleApps = apps.filter {
                it.loadLabel(activity.packageManager).toString().contains(query, true) ||
                    it.packageName.contains(query, true)
            }
            adapter.clear()
            adapter.addAll(visibleApps.map { it.loadLabel(activity.packageManager).toString() })
            adapter.notifyDataSetChanged()
        }
        updateFilter("")
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(if (multi) R.string.action_select_unfreeze else R.string.action_select_launch)
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .apply {
                if (multi) setPositiveButton(R.string.save) { _, _ -> onSelected(selectedPackages) }
            }.create()
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = updateFilter(s.toString())
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        list.setOnItemClickListener { _, _, position, _ ->
            val packageName = visibleApps[position].packageName
            if (multi) {
                if (!selectedPackages.add(packageName)) selectedPackages.remove(packageName)
            } else {
                onSelected(setOf(packageName))
                dialog.dismiss()
            }
        }
        dialog.setOnShowListener {
            visibleApps.forEachIndexed { index, appInfo ->
                list.setItemChecked(index, appInfo.packageName in selectedPackages)
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        activity.fab.setOnClickListener(null)
        super.onDestroyView()
        _binding = null
    }
}