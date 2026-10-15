package com.aistra.hail.ui.home

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.aistra.hail.R
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages.myUserId
import com.aistra.hail.ui.home.ItemHome
import com.aistra.hail.ui.home.ItemHomeState
import com.aistra.hail.app.HailData
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Job

class PagerAdapter(
    private val selectedList: List<com.aistra.hail.app.AppInfo>,
    private val flags: MutableMap<String, Int> = mutableMapOf()
) : ListAdapter<com.aistra.hail.app.AppInfo, PagerAdapter.ViewHolder>(HomeDiff(selectedList, flags)) {
    private var loadIconJob: Job? = null
    lateinit var onItemClickListener: OnItemClickListener
    lateinit var onItemLongClickListener: OnItemLongClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val composeView = ComposeView(parent.context)
        return ViewHolder(composeView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int): Unit {
        val info = currentList[position]
        flags[info.packageName] = info.getFlag(selectedList)
        holder.bind(info)
    }

    fun onDestroy() {
        if (loadIconJob?.isActive == true) loadIconJob?.cancel()
    }

    fun refreshVisualState() {
        notifyItemRangeChanged(0, itemCount)
    }

    private class HomeDiff(
        private val selectedList: List<com.aistra.hail.app.AppInfo>, private val flags: Map<String, Int>
    ) : DiffUtil.ItemCallback<com.aistra.hail.app.AppInfo>() {
        override fun areItemsTheSame(oldItem: com.aistra.hail.app.AppInfo, newItem: com.aistra.hail.app.AppInfo): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: com.aistra.hail.app.AppInfo, newItem: com.aistra.hail.app.AppInfo): Boolean =
            flags[oldItem.packageName] == newItem.getFlag(selectedList)
    }

    inner class ViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        private val state = mutableStateOf(ItemHomeState(
            icon = null,
            appName = "",
            textColor = Color.Unspecified,
            textSize = TextUnit.Unspecified,
            enabled = true
        ))
        private var currentAppInfo: com.aistra.hail.app.AppInfo? = null

        fun bind(info: com.aistra.hail.app.AppInfo) {
            currentAppInfo = info
            state.value = state.value.copy(
                icon = null, // We'll load the icon asynchronously
                appName = info.name,
                textColor = Color.Unspecified,
                textSize = TextUnit.Unspecified,
                enabled = true
            )
            // Update textColor based on info state
            val context = itemView.context
            when {
                info in selectedList -> state.value = state.value.copy(textColor = Color(MaterialColors.getColor(itemView, androidx.appcompat.R.attr.colorPrimary)))
                info.state == com.aistra.hail.app.AppInfo.State.NOT_FOUND -> state.value = state.value.copy(textColor = Color(MaterialColors.getColor(itemView, androidx.appcompat.R.attr.colorError)))
                else -> state.value = state.value.copy(textColor = Color.Unspecified)
            }
            state.value = state.value.copy(textSize = HailData.homeFontSize.sp)
            // TODO: Load icon bitmap asynchronously and update state.icon
            // For now, we'll leave it as null and show the default icon

            // Set the content
            composeView.setContent {
                ItemHome(
                    state = state.value,
                    onClick = { currentAppInfo?.let { onItemClickListener.onItemClick(it) } },
                    onLongClick = { currentAppInfo?.let { onItemLongClickListener.onItemLongClick(it) } }
                )
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(info: com.aistra.hail.app.AppInfo)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(info: com.aistra.hail.app.AppInfo): Boolean
    }
}

private fun com.aistra.hail.app.AppInfo.getFlag(selectedList: List<com.aistra.hail.app.AppInfo>) =
    (1 shl state.ordinal) or (this in selectedList).shl(3) or whitelisted.shl(4)

private fun Boolean.shl(bitCount: Int) = if (this) 1 shl bitCount else 0