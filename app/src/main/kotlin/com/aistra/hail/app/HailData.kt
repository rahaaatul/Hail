package com.aistra.hail.app

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.aistra.hail.BuildConfig
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.utils.HFiles
import com.aistra.hail.utils.HLog
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

object HailData {
    const val URL_WHY_FREE_SOFTWARE = "https://www.gnu.org/philosophy/free-software-even-more-important.html"
    const val URL_GITHUB = "https://github.com/rahaaatul/Hail"
    const val URL_README = "$URL_GITHUB#readme"
    const val URL_RELEASES = "$URL_GITHUB/releases"
    const val URL_TELEGRAM = "https://t.me/+yvRXYTounDIxODFl"
    const val URL_QQ = "http://qm.qq.com/cgi-bin/qm/qr?k=I2g_Ymanc6bQMo4cVKTG0knARE0twtSG"
    const val URL_FDROID = "https://f-droid.org/packages/${BuildConfig.APPLICATION_ID}"
    const val URL_ALIPAY = "https://qr.alipay.com/tsx02922ajwj6xekqyd1rbf"
    const val URL_ALIPAY_API = "alipays://platformapi/startapp?saId=10000007&qrcode=$URL_ALIPAY"
    const val URL_KOFI = "https://ko-fi.com/aistra0528"
    const val URL_LIBERAPAY = "https://liberapay.com/aistra0528"
    const val URL_PAYPAL = "https://www.paypal.me/aistra0528"
    const val URL_TRANSLATE = "https://hosted.weblate.org/engage/hail/"
    const val VERSION = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    private const val KEY_ID = "id"
    const val KEY_TAG = "tag"
    private const val KEY_TAGS = "tags"
    private const val KEY_PINNED = "pinned"
    private const val KEY_WHITELISTED = "whitelisted"
    const val KEY_PACKAGE = "package"
    const val KEY_FROZEN = "frozen"
    private const val SORT_BY = "sort_by"
    const val SORT_NAME = "name"
    const val SORT_INSTALL = "install"
    const val SORT_UPDATE = "update"
    const val FILTER_USER_APPS = "filter_user_apps"
    const val FILTER_SYSTEM_APPS = "filter_system_apps"
    const val FILTER_ALL_APPS = "filter_all_apps"
    const val FILTER_FROZEN_APPS = "filter_frozen_apps"
    const val FILTER_UNFROZEN_APPS = "filter_unfrozen_apps"
    const val OWNER = "owner_"
    const val DHIZUKU = "dhizuku_"
    const val SU = "su_"
    const val SHIZUKU = "shizuku_"
    const val ISLAND = "island_"
    const val PRIVAPP = "privapp_"
    const val STOP = "stop"
    const val DISABLE = "disable"
    const val HIDE = "hide"
    const val SUSPEND = "suspend"
    const val WORKING_MODE = "working_mode"
    const val MODE_DEFAULT = "default"
    const val MODE_SHIZUKU_STOP = SHIZUKU + STOP
    const val MODE_SHIZUKU_DISABLE = SHIZUKU + DISABLE
    const val MODE_SHIZUKU_HIDE = SHIZUKU + HIDE
    const val MODE_SHIZUKU_SUSPEND = SHIZUKU + SUSPEND
    const val MODE_SU_STOP = SU + STOP
    const val MODE_SU_DISABLE = SU + DISABLE
    const val MODE_SU_HIDE = SU + HIDE
    const val MODE_SU_SUSPEND = SU + SUSPEND
    const val MODE_DHIZUKU_HIDE = DHIZUKU + HIDE
    const val MODE_DHIZUKU_SUSPEND = DHIZUKU + SUSPEND
    const val MODE_OWNER_HIDE = OWNER + HIDE
    const val MODE_OWNER_SUSPEND = OWNER + SUSPEND
    const val MODE_ISLAND_HIDE = ISLAND + HIDE
    const val MODE_ISLAND_SUSPEND = ISLAND + SUSPEND
    const val MODE_PRIVAPP_STOP = PRIVAPP + STOP
    const val MODE_PRIVAPP_DISABLE = PRIVAPP + DISABLE
    val WORKING_MODE_VALUES = listOf(
        MODE_DEFAULT,
        MODE_SHIZUKU_STOP,
        MODE_SHIZUKU_DISABLE,
        MODE_SHIZUKU_HIDE,
        MODE_SHIZUKU_SUSPEND,
        MODE_SU_STOP,
        MODE_SU_DISABLE,
        MODE_SU_HIDE,
        MODE_SU_SUSPEND,
        MODE_DHIZUKU_HIDE,
        MODE_DHIZUKU_SUSPEND,
        MODE_OWNER_HIDE,
        MODE_OWNER_SUSPEND,
        MODE_ISLAND_HIDE,
        MODE_ISLAND_SUSPEND,
        MODE_PRIVAPP_STOP,
        MODE_PRIVAPP_DISABLE
    )
    const val BIOMETRIC_LOGIN = "biometric_login"
    const val APP_THEME = "app_theme"
    const val FOLLOW_SYSTEM = "follow_system"
    const val THEME_LIGHT = "theme_light"
    const val THEME_DARK = "theme_dark"
    val APP_THEME_VALUES = listOf(FOLLOW_SYSTEM, THEME_LIGHT, THEME_DARK)
    const val ICON_PACK = "icon_pack"
    const val GRAYSCALE_ICON = "grayscale_icon"
    const val COMPACT_ICON = "compact_icon"
    const val SYNTHESIZE_ADAPTIVE_ICONS = "synthesize_adaptive_icons"
    const val HOME_FONT_SIZE = "home_font_size_f"
    const val FUZZY_SEARCH = "fuzzy_search"
    const val NINE_KEY_SEARCH = "nine_key"
    const val TILE_ACTION = "tile_action"
    const val ACTION_NONE = "none"
    const val ACTION_FREEZE_ALL = "freeze_all"
    const val ACTION_UNFREEZE_ALL = "unfreeze_all"
    const val ACTION_FREEZE_NON_WHITELISTED = "freeze_non_whitelisted"
    const val ACTION_LOCK = "lock"
    const val ACTION_LOCK_FREEZE = "lock_freeze"
    val TILE_ACTION_VALUES =
        listOf(
            AUTO_FREEZE_AFTER_LOCK,
            ACTION_FREEZE_ALL,
            ACTION_UNFREEZE_ALL,
            ACTION_FREEZE_NON_WHITELISTED,
            ACTION_LOCK,
            ACTION_LOCK_FREEZE
        )
    const val AUTO_FREEZE_AFTER_LOCK = "auto_freeze_after_lock"
    const val AUTO_FREEZE_DELAY = "auto_freeze_delay_f"
    const val SKIP_WHILE_CHARGING = "skip_while_charging"
    const val SKIP_FOREGROUND_APP = "skip_foreground_app"
    const val SKIP_NOTIFYING_APP = "skip_notifying_app"
    const val DYNAMIC_SHORTCUT_ACTION = "dynamic_shortcut_action"

    /** A preference the app stores as a [Float], with everything the settings screen offers. */
    data class FloatPreference(
        /** The bounds a slider for this key allows, inclusive at both ends. */
        val range: ClosedFloatingPointRange<Float>,
        /** What the app falls back to while the key has never been stored. */
        val default: Float,
    )

    /**
     * Every preference the app stores as a Float, with the range its slider allows and
     * the default its getter falls back to.
     *
     * This is the single declaration of that surface, and it is declared in one place
     * because three call sites used to state the same three facts separately: the
     * settings screen asked for a slider's range, the getters carried their own default,
     * and the backup reader carried a hand-maintained list of which keys are Floats. Any
     * of them could drift and nothing would notice, and a key that drifted out of the
     * reader's list is the #91 mis-typing all over again - the reader restores it with
     * the type it finds rather than the type the app means, and a bare whole number then
     * goes in as an Int and stays there on every later restore.
     *
     * Both sides read this map rather than restating it, and both are checked rather
     * than assumed. A lookup for a key the map does not list throws instead of inventing
     * a default, so the screen and the getters cannot quietly disagree with the reader
     * about a key. What no map can do is make the compiler object to a future literal,
     * so the read side is pinned structurally: [declaredFloat] is the only call to
     * `sp.getFloat` the app is allowed, and a test fails if a second one appears. That
     * is what lets HBackup treat a key in here as a Float and refuse anything outside its
     * range.
     */
    val FLOAT_PREFERENCES: Map<String, FloatPreference> = mapOf(
        HOME_FONT_SIZE to FloatPreference(range = 11f..16f, default = 14f),
        AUTO_FREEZE_DELAY to FloatPreference(range = 0f..30f, default = 0f),
    )

    /** The Float preference [key] names, or a loud failure when it is not a declared one. */
    fun floatPreference(key: String): FloatPreference =
        FLOAT_PREFERENCES[key]
            ?: error("'$key' is not a declared Float preference; add it to FLOAT_PREFERENCES")

    /** The slider range for a Float preference declared in [FLOAT_PREFERENCES]. */
    fun floatRange(key: String): ClosedFloatingPointRange<Float> = floatPreference(key).range

    /**
     * The value a Float preference falls back to before it has ever been stored. The
     * getters and each slider's own `defaultValue` both read it from here, so the two
     * cannot state different defaults for one key.
     */
    fun floatDefault(key: String): Float = floatPreference(key).default

    /**
     * The stored value of a declared Float preference, or the default it declares.
     *
     * The only call to `sp.getFloat` the app makes, and deliberately so: a getter that
     * reached for the preference directly could state a default this map does not hold,
     * and the backup reader would then treat its key as undeclared - which is #91 again
     * under a different key. [floatDefault] throws for a key the map does not list, so a
     * new read is a deliberate edit to this file, and
     * `every float read in the app goes through the declared default` fails if a second
     * `sp.getFloat` call turns up anywhere under `src/main/kotlin`.
     */
    private fun declaredFloat(key: String): Float = sp.getFloat(key, floatDefault(key))
    val DYNAMIC_SHORTCUT_ACTIONS = listOf(
        ACTION_NONE,
        ACTION_FREEZE_ALL,
        ACTION_UNFREEZE_ALL,
        ACTION_FREEZE_NON_WHITELISTED,
        ACTION_LOCK,
        ACTION_LOCK_FREEZE
    )

    private val sp by lazy { PreferenceManager.getDefaultSharedPreferences(app) }
    val sortBy get() = sp.getString(SORT_BY, SORT_NAME)
    val filterUserApps get() = sp.getBoolean(FILTER_USER_APPS, true)
    val filterSystemApps get() = sp.getBoolean(FILTER_SYSTEM_APPS, false)
    val filterAllApps get() = sp.getBoolean(FILTER_ALL_APPS, true)
    val filterFrozenApps get() = sp.getBoolean(FILTER_FROZEN_APPS, true)
    val filterUnfrozenApps get() = sp.getBoolean(FILTER_UNFROZEN_APPS, true)
    val workingMode get() = sp.getString(WORKING_MODE, MODE_DEFAULT)!!
    val biometricLogin get() = sp.getBoolean(BIOMETRIC_LOGIN, false)
    val appTheme get() = sp.getString(APP_THEME, FOLLOW_SYSTEM)!!
    val iconPack get() = sp.getString(ICON_PACK, ACTION_NONE)!!
    val grayscaleIcon get() = sp.getBoolean(GRAYSCALE_ICON, true)
    val compactIcon get() = sp.getBoolean(COMPACT_ICON, false)
    val synthesizeAdaptiveIcons get() = sp.getBoolean(SYNTHESIZE_ADAPTIVE_ICONS, false)
    val homeFontSize get() = declaredFloat(HOME_FONT_SIZE)
    val fuzzySearch get() = sp.getBoolean(FUZZY_SEARCH, false)
    val nineKeySearch get() = sp.getBoolean(NINE_KEY_SEARCH, false)
    val tileAction get() = sp.getString(TILE_ACTION, AUTO_FREEZE_AFTER_LOCK)!!
    var autoFreezeAfterLock
        get() = sp.getBoolean(AUTO_FREEZE_AFTER_LOCK, false)
        set(value) = sp.edit { putBoolean(AUTO_FREEZE_AFTER_LOCK, value) }
    val autoFreezeDelay get() = declaredFloat(AUTO_FREEZE_DELAY).toLong()
    val skipWhileCharging get() = sp.getBoolean(SKIP_WHILE_CHARGING, false)
    val skipForegroundApp get() = sp.getBoolean(SKIP_FOREGROUND_APP, false)
    val skipNotifyingApp get() = sp.getBoolean(SKIP_NOTIFYING_APP, false)
    val dynamicShortcutAction get() = sp.getString(DYNAMIC_SHORTCUT_ACTION, ACTION_NONE)!!

    // Resolved when first needed rather than while this object initializes. An initializer
    // that names the application captures whatever the application happened to be at the
    // moment something first touched HailData, and in the app that is onCreate setting `app`
    // (HailApp.kt:32) before any screen or service can reach here - so the eager version
    // worked, which is why nobody had a reason to look at it. On a JVM test it does not: a
    // relaxed mock is often installed first, and `filesDir.path` is whatever the mock
    // answers with, which freezes the path for every test that follows.
    private val dir by lazy { "${app.filesDir.path}/v1" }

    // Derived rather than lazy: `dir` already caches, and these are on the write path, where
    // every save reads them.
    private val appsPath get() = "$dir/apps.json"
    private val tagsPath get() = "$dir/tags.json"
    private val checkedListLock = Object()

    val checkedList: MutableList<AppInfo> by lazy {
        mutableListOf<AppInfo>().apply {
            runCatching {
                val json = JSONArray(HFiles.read(appsPath))
                for (i in 0 until json.length()) {
                    add(with(json.getJSONObject(i)) {
                        AppInfo(
                            packageName = getString(KEY_PACKAGE),
                            pinned = optBoolean(KEY_PINNED),
                            whitelisted = optBoolean(KEY_WHITELISTED),
                            tagIdList = optJSONArray(KEY_TAGS)?.let {
                                MutableList(it.length()) { index -> it.getInt(index) }
                            } ?: mutableListOf(optInt(KEY_TAG))
                        )
                    })
                }
            }
        }
    }

    fun isChecked(packageName: String): Boolean {
        if (packageName == BuildConfig.APPLICATION_ID) return false
        synchronized(checkedListLock) {
            return checkedList.any { it.packageName == packageName }
        }
    }

    fun addCheckedApp(packageName: String, tagId: Int = 0, shouldSave: Boolean = true) {
        if (packageName == BuildConfig.APPLICATION_ID) return
        synchronized(checkedListLock) {
            checkedList.add(AppInfo(packageName, tagIdList = mutableListOf(tagId)))
        }
        if (shouldSave) saveApps()
    }

    fun removeCheckedApp(packageName: String, shouldSave: Boolean = true) {
        synchronized(checkedListLock) {
            checkedList.removeAll { it.packageName == packageName }
        }
        if (shouldSave) saveApps()
    }

    fun saveApps(): Boolean {
        val snapshot: List<AppInfo>
        synchronized(checkedListLock) {
            snapshot = checkedList.toList()
        }
        return saveAppsLocked(snapshot)
    }

    private fun saveAppsLocked(apps: List<AppInfo>): Boolean {
        if (!HFiles.exists(dir)) HFiles.createDirectories(dir)
        val json = JSONArray().run {
            apps.forEach {
                put(
                    JSONObject()
                        .put(KEY_PACKAGE, it.packageName)
                        .put(KEY_PINNED, it.pinned)
                        .put(KEY_WHITELISTED, it.whitelisted)
                        .put(KEY_TAGS, JSONArray(it.tagIdList))
                )
            }
            toString()
        }
        val tmpFile = File("$appsPath.tmp")
        val appsFile = File(appsPath)
        if (!HFiles.write(tmpFile.absolutePath, json)) {
            HLog.e("Failed to write apps.json to ${tmpFile.absolutePath}")
            tmpFile.delete()
            return false
        }
        if (!tmpFile.renameTo(appsFile)) {
            HLog.e("Failed to rename ${tmpFile.absolutePath} to ${appsFile.absolutePath}")
            tmpFile.delete()
            return false
        }
        return true
    }

    val tags: MutableList<Pair<String, Int>> by lazy {
        mutableListOf<Pair<String, Int>>().apply {
            runCatching {
                val json = JSONArray(HFiles.read(tagsPath))
                for (i in 0 until json.length()) {
                    add(with(json.getJSONObject(i)) { getString(KEY_TAG) to getInt(KEY_ID) })
                }
            }.onFailure {
                add(app.getString(R.string.label_default) to 0)
            }
        }
    }

    fun saveTags() {
        if (!HFiles.exists(dir)) HFiles.createDirectories(dir)
        HFiles.write(tagsPath, JSONArray().run {
            tags.forEach {
                put(JSONObject().put(KEY_TAG, it.first).put(KEY_ID, it.second))
            }
            toString()
        })
    }

    fun changeAppsSort(sort: String) = sp.edit { putString(SORT_BY, sort) }

    fun changeAppsFilter(filter: String, enabled: Boolean) = sp.edit { putBoolean(filter, enabled) }
}
