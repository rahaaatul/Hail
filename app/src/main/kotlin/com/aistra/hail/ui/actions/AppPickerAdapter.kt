package com.aistra.hail.ui.actions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.app.AppInfo
import com.aistra.hail.databinding.ItemActionPickerBinding
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages

class AppPickerAdapter(
    private val multi: Boolean,
    private val selected: MutableSet<String>,
    private val onSelected: (String) -> Unit
) : RecyclerView.Adapter<AppPickerAdapter.ViewHolder>() {
    private var applications: List<android.content.pm.ApplicationInfo> = emptyList()

    fun submitList(value: List<android.content.pm.ApplicationInfo>) {
        applications = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemActionPickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(applications[position])

    override fun getItemCount() = applications.size

    inner class ViewHolder(private val binding: ItemActionPickerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(info: android.content.pm.ApplicationInfo) {
            val packageName = info.packageName
            binding.appName.text = info.loadLabel(binding.root.context.packageManager)
            AppIconCache.loadIconBitmapAsync(binding.root.context, info, HPackages.myUserId, binding.appIcon, false)
            binding.selectedIcon.isVisible = packageName in selected
            binding.root.setOnClickListener {
                if (multi) {
                    if (!selected.add(packageName)) selected.remove(packageName)
                    notifyItemChanged(bindingAdapterPosition)
                }
                onSelected(packageName)
            }
        }
    }
}