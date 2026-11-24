package com.aistra.hail.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import com.aistra.hail.app.SettingsData

/**
 * Exposes one setting of [settings] as a [MutableState] backed by HailSettings' optimistic setters.
 *
 * Reads go through the collected [SettingsData] snapshot, so every preference recomposes when the
 * underlying setting changes (including changes made elsewhere, e.g. the Quick Settings tile).
 * Writes go through [set], which persists the value and runs whatever side effect the change needs.
 *
 * A [set] that decides not to accept the change simply does not write to [HailSettings]; the preference
 * then keeps showing the previous value instead of the value the user just picked.
 */
@Composable
fun <T> rememberSettingState(
    settings: State<SettingsData>,
    get: (SettingsData) -> T,
    set: (T) -> Unit,
): MutableState<T> {
    val write = set
    return remember(settings, get, write) {
        object : MutableState<T> {
            override var value: T
                get() = get(settings.value)
                set(newValue) {
                    write(newValue)
                }

            override fun component1(): T = value

            override fun component2(): (T) -> Unit = { write(it) }
        }
    }
}
