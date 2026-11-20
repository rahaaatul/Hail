package com.aistra.hail.app

import android.content.pm.ApplicationInfo
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.HPackages

class AppInfo(
    val packageName: String,
    var pinned: Boolean = false,
    var whitelisted: Boolean = false,
    val tagIdList: MutableList<Int> = mutableListOf(0),
) {
    enum class State {
        NOT_FOUND,
        ENABLED,
        DISABLED,
        SUSPENDED,
        HIDDEN,
        STOPPED,
        UNINSTALLED,
        ;

        /** True when the app is present but not runnable — disabled or otherwise held by the system. */
        val isFrozen: Boolean
            get() =
                when (this) {
                    DISABLED, SUSPENDED, HIDDEN, STOPPED -> true
                    NOT_FOUND, ENABLED, UNINSTALLED -> false
                }
    }

    val applicationInfo: ApplicationInfo? get() = HPackages.getApplicationInfoOrNull(packageName)
    val name get() = AppMetaCache.get(packageName)?.name ?: packageName
    val isInstalled get() = AppMetaCache.get(packageName)?.installed ?: (applicationInfo != null)
    val state get() =
        AppMetaCache.get(packageName)?.state ?: when {
            applicationInfo == null -> State.NOT_FOUND
            else -> State.ENABLED
        }

    override fun equals(other: Any?): Boolean = other is AppInfo && other.packageName == packageName

    override fun hashCode(): Int = packageName.hashCode()
}
