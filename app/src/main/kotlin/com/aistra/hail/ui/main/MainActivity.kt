package com.aistra.hail.ui.main

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.MenuCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.aistra.hail.HailApp
import com.aistra.hail.R
import com.aistra.hail.app.BiometricGate
import com.aistra.hail.app.HailSettings
import com.aistra.hail.databinding.ActivityMainBinding
import com.aistra.hail.extensions.applyDefaultInsetter
import com.aistra.hail.extensions.isLandscape
import com.aistra.hail.extensions.isRtl
import com.aistra.hail.extensions.marginRelative
import com.aistra.hail.extensions.paddingRelative
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HUI
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MainActivity :
    AppCompatActivity(),
    NavController.OnDestinationChangedListener {
    lateinit var fab: ExtendedFloatingActionButton
    lateinit var appbar: AppBarLayout
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private var isAuthenticated = false
    private var isPromptShowing = false
    private var biometricUnavailableDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = initView()
        HailApp.app.syncRootShell()
        authenticateIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        authenticateIfNeeded()
    }

    // TODO: biometric-compose (rememberAuthenticationLauncher) is not applicable here; MainActivity is AppCompat/Fragments.
    // Re-entering the activity (for example, coming back from the biometric settings) re-runs the
    // check so the content never stays hidden behind a dismissed prompt or dialog.
    private fun authenticateIfNeeded() {
        if (isAuthenticated || isPromptShowing || !HailSettings.biometricLogin) return

        val biometricManager = BiometricManager.from(this)
        val strongResult = biometricManager.canAuthenticate(BIOMETRIC_STRONG)
        val deviceCredentialResult = biometricManager.canAuthenticate(DEVICE_CREDENTIAL)

        if (BiometricGate.shouldAuthenticate(HailSettings.biometricLogin, strongResult, deviceCredentialResult)) {
            isPromptShowing = true
            binding.root.isVisible = false
            startBiometricPrompt(BiometricGate.allowedAuthenticators(strongResult, deviceCredentialResult))
        } else {
            showBiometricUnavailableDialog()
        }
    }

    // TODO: setInvalidatedByBiometricEnrollment is a Keystore-only property; it requires a biometric-only CryptoObject path and is incompatible with the device-credential fallback.
    private fun startBiometricPrompt(allowedAuthenticators: Int) {
        val canUseAdvanced =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SET_BIOMETRIC_DIALOG_ADVANCED,
            ) == PackageManager.PERMISSION_GRANTED

        try {
            launchBiometricPrompt(allowedAuthenticators, canUseAdvanced)
        } catch (_: SecurityException) {
            if (!canUseAdvanced) {
                showBiometricUnavailableDialog()
                return
            }
            try {
                launchBiometricPrompt(allowedAuthenticators, canUseAdvanced = false)
            } catch (_: SecurityException) {
                showBiometricUnavailableDialog()
            }
        }
    }

    private fun launchBiometricPrompt(
        allowedAuthenticators: Int,
        canUseAdvanced: Boolean,
    ) {
        val biometricPrompt =
            BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        super.onAuthenticationError(errorCode, errString)
                        isPromptShowing = false
                        HUI.showToast(errString)
                        finishAndRemoveTask()
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isPromptShowing = false
                        isAuthenticated = true
                        binding.root.isVisible = true
                    }
                },
            )
        val promptInfo =
            BiometricGate.buildPromptInfo(
                BiometricGate.PromptInfoSpec(
                    allowedAuthenticators = allowedAuthenticators,
                    title = getString(R.string.action_biometric),
                    subtitle = getString(R.string.msg_biometric),
                    description = getString(R.string.msg_biometric_prompt),
                    negativeButtonText = getString(R.string.action_cancel),
                    logoRes = R.mipmap.ic_launcher,
                    canUseAdvanced = canUseAdvanced,
                ),
            )
        biometricPrompt.authenticate(promptInfo)
    }

    private fun showBiometricUnavailableDialog() {
        isPromptShowing = false
        binding.root.isVisible = false
        if (biometricUnavailableDialog?.isShowing == true) return
        biometricUnavailableDialog =
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title_biometric_unavailable)
                .setMessage(R.string.msg_biometric_unavailable)
                .setPositiveButton(R.string.action_biometric_settings) { _, _ -> openBiometricSettings() }
                .setNegativeButton(R.string.action_cancel) { _, _ -> finishAndRemoveTask() }
                .setOnCancelListener { finishAndRemoveTask() }
                .show()
    }

    private fun openBiometricSettings() {
        biometricUnavailableDialog?.dismiss()
        biometricUnavailableDialog = null
        val intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Intent(Settings.ACTION_BIOMETRIC_ENROLL).takeIf { it.resolveActivity(packageManager) != null }
            } else {
                null
            } ?: Intent(Settings.ACTION_SECURITY_SETTINGS)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            HUI.showToast(getString(R.string.msg_biometric_settings_not_found))
            finishAndRemoveTask()
        }
    }

    private fun initView() =
        ActivityMainBinding.inflate(layoutInflater).apply {
            binding = this
            setContentView(root)
            setSupportActionBar(appBarMain.toolbar)
            fab = appBarMain.fab
            appbar = appBarMain.appBarLayout

            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController
            navController.addOnDestinationChangedListener(this@MainActivity)
            appBarConfiguration =
                AppBarConfiguration
                    .Builder(
                        R.id.nav_home,
                        R.id.nav_actions,
                        R.id.nav_settings,
                    ).build()
            setupActionBarWithNavController(navController, appBarConfiguration)
            bottomNav?.setupWithNavController(navController)
            navRail?.setupWithNavController(navController)

            // Always navigate to the existing Settings page instead of pushing a
            // duplicate SettingsFragment on top of whatever is current.
            // popUpTo(R.id.nav_settings, inclusive = true) pops down to and
            // including Settings if it's already on the back stack (e.g. we're
            // on the Data page inside Settings), then re-pushes Settings.
            // If Settings isn't on the stack, there's nothing to pop and it just
            // pushes Settings on top — either way we end up on a single Settings
            // page, never a duplicate/loop.
            val navigateToSettings = {
                navController.navigate(
                    R.id.nav_settings,
                    null,
                    NavOptions
                        .Builder()
                        .setPopUpTo(R.id.nav_settings, true)
                        .build(),
                    null,
                )
            }
            bottomNav?.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_settings -> navigateToSettings()
                    else -> navController.navigate(item.itemId)
                }
                true
            }
            navRail?.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_settings -> navigateToSettings()
                    else -> navController.navigate(item.itemId)
                }
                true
            }

            val isRtl = isRtl
            val isLandscape = isLandscape
            appBarMain.appBarLayout.applyDefaultInsetter {
                paddingRelative(isRtl, start = !isLandscape, end = true, top = true)
            }
            bottomNav?.applyDefaultInsetter { paddingRelative(isRtl, start = true, end = true, bottom = true) }
            navRail?.applyDefaultInsetter { paddingRelative(isRtl, start = true, top = true, bottom = true) }
            fab.applyDefaultInsetter { marginRelative(isRtl, end = true, bottom = isLandscape) }
        }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.let { MenuCompat.setGroupDividerEnabled(it, true) }
        return super.onCreateOptionsMenu(menu)
    }

    fun ownerRemoveDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_remove_owner)
            .setMessage(R.string.msg_remove_owner)
            .setPositiveButton(R.string.action_continue) { _, _ ->
                HPolicy.setOrganizationName()
                HPolicy.removeDeviceOwner()
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /* override fun onStop() {
        super.onStop()
        if (HailSettings.biometricLogin) finishAndRemoveTask()
    } */

    override fun onSupportNavigateUp(): Boolean = navController.navigateUp() || super.onSupportNavigateUp()

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?,
    ) {
        fab.setOnClickListener(null)
        fab.tag = destination.id == R.id.nav_home || destination.id == R.id.nav_actions
        when (destination.id) {
            R.id.nav_home -> {
                fab.setIconResource(R.drawable.ic_round_add)
                fab.contentDescription = getString(R.string.title_apps)
                fab.setOnClickListener { controller.navigate(R.id.nav_apps) }
                fab.show()
            }

            R.id.nav_actions -> {
                fab.setIconResource(R.drawable.ic_round_add)
                fab.contentDescription = getString(R.string.action_create_action)
                fab.setOnClickListener {
                    (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment)
                        ?.childFragmentManager
                        ?.primaryNavigationFragment
                        ?.let { fragment ->
                            (fragment as? com.aistra.hail.ui.actions.ActionsFragment)?.showEditor(null)
                        }
                }
                fab.show()
            }

            else -> {
                fab.hide()
            }
        }
        binding.bottomNav?.isVisible = destination.id != R.id.nav_about
        binding.navRail?.isVisible = destination.id != R.id.nav_about
    }
}
