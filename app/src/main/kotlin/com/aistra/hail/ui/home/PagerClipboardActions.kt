package com.aistra.hail.ui.home

import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HUI
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PagerClipboardActions(private val fragment: PagerFragment) {
    fun export(list: List<AppInfo>) {
        if (list.isEmpty()) return
        HUI.copyText(if (list.size > 1) JSONArray().run {
            list.forEach { put(it.packageName) }
            toString()
        } else list[0].packageName)
        HUI.showToast(
            R.string.msg_exported, if (list.size > 1) list.size.toString() else list[0].name
        )
    }

    fun import() {
        val str = HUI.pasteText()
        if (str == null) {
            HUI.showToast(R.string.operation_failed, "Clipboard is empty", true)
            return
        }
        runCatching {
            val json = if (str.contains('[')) JSONArray(
                str.substring(
                    str.indexOf('[')..str.indexOf(']', str.indexOf('['))
                )
            ) else JSONArray().put(str)
            var importedCount = 0
            for (index in 0 until json.length()) {
                val packageName = json.getString(index)
                if (HPackages.getApplicationInfoOrNull(packageName) != null &&
                    !HailData.isChecked(packageName)
                ) {
                    HailData.addCheckedApp(packageName, fragment.tag?.second ?: 0, false)
                    importedCount++
                }
            }
            if (importedCount > 0) {
                HailData.saveApps()
                fragment.updateCurrentList()
            }
            HUI.showToast(fragment.getString(R.string.msg_imported, importedCount.toString()))
        }.onFailure {
            HUI.showToast(
                R.string.operation_failed, it.message ?: it.javaClass.simpleName, true
            )
        }
    }

    suspend fun importFrozenApps() = withContext(Dispatchers.IO) {
        HPackages.getInstalledApplications().map { it.packageName }
            .filter { AppManager.isAppFrozen(it) && !HailData.isChecked(it) }
            .onEach { HailData.addCheckedApp(it, fragment.tag?.second ?: 0, false) }.size
    }
}
