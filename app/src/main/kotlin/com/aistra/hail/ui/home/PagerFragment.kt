package com.aistra.hail.ui.home

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
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailApi.addTag
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.DialogInputBinding
import com.aistra.hail.databinding.FragmentPagerBinding
import com.aistra.hail.extensions.applyDefaultInsetter
import com.aistra.hail.extensions.isLandscape
import com.aistra.hail.extensions.isRtl
import com.aistra.hail.extensions.marginRelative
import com.aistra.hail.extensions.paddingRelative
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.AppActions
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.FuzzySearch
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HShizuku
import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.HUI
import com.aistra.hail.utils.NameComparator
import com.aistra.hail.utils.NineKeySearch
import com.aistra.hail.utils.PinyinSearch
import com.aistra.hail.work.HWork
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class PagerFragment :
    MainFragment(),
    PagerAdapter.OnItemClickListener,
    PagerAdapter.OnItemLongClickListener,
    MenuProvider {
    private var query: String = String()
    private var _binding: FragmentPagerBinding? = null
    private val binding get() = _binding!!
    private lateinit var pagerAdapter: PagerAdapter
    private var multiselect: Boolean
        set(value) {
            (parentFragment as HomeFragment).multiselect = value
        }
        get() = (parentFragment as HomeFragment).multiselect
    private val selectedList get() = (parentFragment as HomeFragment).selectedList
    private val tabs: TabLayout? get() = (parentFragment as? HomeFragment)?.binding?.tabs
    private val adapter: HomeAdapter? get() = (parentFragment as? HomeFragment)?.binding?.pager?.adapter as? HomeAdapter
    private val tag: Pair<String, Int>? get() = tabs?.let { HailData.tags.getOrNull(it.selectedTabPosition) }


    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (multiselect) {
                        deselect()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        _binding = FragmentPagerBinding.inflate(inflater, container, false)
        pagerAdapter = PagerAdapter(selectedList).apply {
            onItemClickListener = this@PagerFragment
            onItemLongClickListener = this@PagerFragment
        }
        binding.recyclerView.run {
            layoutManager = GridLayoutManager(
                activity,
                resources.getInteger(
                    if (HailData.compactIcon) R.integer.home_span_compact else R.integer.home_span,
                ),
            )
            adapter = pagerAdapter
            applyDefaultInsetter { paddingRelative(isRtl, bottom = isLandscape) }
        }

        binding.refresh.apply {
            setOnRefreshListener {
                updateCurrentList()
                binding.refresh.isRefreshing = false
            }
            applyDefaultInsetter { marginRelative(isRtl, start = !isLandscape, end = true) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                AppMetaCache.revision.collect { updateCurrentList() }
            }
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        AppMetaCache.invalidateState(HailData.checkedList.map { it.packageName })
        AppMetaCache.prefetchPackages(HailData.checkedList.map { it.packageName })
        updateCurrentList()
        updateBarTitle()
        activity.appbar.setLiftOnScrollTargetView(binding.recyclerView)
        tabs?.let { tabLayout ->
            tabLayout.getTabAt(tabLayout.selectedTabPosition)?.view?.setOnLongClickListener {
                if (isResumed) showTagDialog()
                true
            }
        }
        activity.fab.setOnLongClickListener(null)
    }

    internal fun updateCurrentList() {
        val binding = _binding ?: return
        HailData.checkedList.filter { it.isInstalled }.filter {
            if (query.isEmpty()) {
                tag?.second?.let { tagId -> tagId in it.tagIdList } ?: false
            } else {
                (
                    (
                        HailData.nineKeySearch && NineKeySearch.search(
                            query,
                            it.packageName,
                            it.name,
                        )
                        ) || FuzzySearch.search(it.packageName, query) || FuzzySearch.search(
                        it.name,
                        query,
                    ) || PinyinSearch.searchPinyinAll(it.name, query)
                    )
            }
        }.sortedWith(NameComparator).let {
            binding.empty.isVisible = it.isEmpty()
            pagerAdapter.submitList(it)
            app.setAutoFreezeService()
        }
    }


    override fun onItemClick(info: AppInfo) {
        if (isAdded) {
            if (multiselect) {
                if (info in selectedList) {
                    selectedList.remove(info)
                } else {
                    selectedList.add(info)
                }
                updateCurrentList()
                updateBarTitle()
            } else if (info.applicationInfo == null) {
                Snackbar.make(activity.fab, R.string.app_not_installed, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_remove_home) { removeCheckedApp(info.packageName) }.show()
            } else {
                launchApp(info.packageName)
            }
        }
    }

    override fun onItemLongClick(info: AppInfo): Boolean {
        if (info.applicationInfo == null && (!multiselect || info !in selectedList)) {
            exportToClipboard(listOf(info))
        } else if (info in selectedList) {
            onMultiSelect()
        } else {
        showAppActionDialog(info)
    }
    return true
}
