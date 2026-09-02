package com.aistra.hail.ui.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute : NavKey
@Serializable data object AppsRoute : NavKey
@Serializable data object ActionsRoute : NavKey
@Serializable data object SettingsRoute : NavKey
@Serializable data object AboutRoute : NavKey