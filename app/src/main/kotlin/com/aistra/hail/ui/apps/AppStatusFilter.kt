@file:Suppress("MatchingDeclarationName")

package com.aistra.hail.ui.apps

import com.aistra.hail.app.AppInfo

internal data class StatusFilter(
    val enabled: Boolean,
    val disabled: Boolean,
    val suspended: Boolean,
    val hidden: Boolean,
    val stopped: Boolean,
    val uninstalled: Boolean,
)

internal fun matchesStatusFilter(
    status: AppInfo.State?,
    enabled: Boolean?,
    filter: StatusFilter,
): Boolean {
    if (status == null || status == AppInfo.State.UNKNOWN) return true
    return matchesKnownState(status, enabled, filter)
}

private fun matchesKnownState(
    status: AppInfo.State,
    enabled: Boolean?,
    filter: StatusFilter,
): Boolean =
    when (status) {
        AppInfo.State.UNINSTALLED -> {
            filter.uninstalled
        }

        AppInfo.State.NOT_FOUND -> {
            false
        }

        else -> {
            (filter.enabled && enabled == true) ||
                (filter.disabled && enabled == false) ||
                (filter.suspended && status == AppInfo.State.SUSPENDED) ||
                (filter.hidden && status == AppInfo.State.HIDDEN) ||
                (filter.stopped && status == AppInfo.State.STOPPED)
        }
    }
