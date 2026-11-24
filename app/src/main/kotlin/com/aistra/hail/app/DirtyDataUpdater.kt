package com.aistra.hail.app

import android.content.Context

object DirtyDataUpdater {
    fun update(ignored: Context) {
        // Ensure Proto DataStore is initialized early in app startup.
        SettingsDataStore.ensureInitialized()
    }
}
