package com.aistra.hail.app

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt

object BiometricGate {
    data class PromptInfoSpec(
        val allowedAuthenticators: Int,
        val title: String,
        val subtitle: String,
        val description: String?,
        val negativeButtonText: String?,
        val logoRes: Int?,
        val canUseAdvanced: Boolean,
    )

    fun shouldAuthenticate(
        biometricLogin: Boolean,
        strongResult: Int,
        deviceCredentialResult: Int,
    ): Boolean {
        if (!biometricLogin) return false
        return strongResult == BiometricManager.BIOMETRIC_SUCCESS ||
            deviceCredentialResult == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun allowedAuthenticators(
        strongResult: Int,
        deviceCredentialResult: Int,
    ): Int {
        val strongAvailable = strongResult == BiometricManager.BIOMETRIC_SUCCESS
        val deviceCredentialAvailable = deviceCredentialResult == BiometricManager.BIOMETRIC_SUCCESS
        return when {
            strongAvailable && deviceCredentialAvailable -> BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            strongAvailable -> BIOMETRIC_STRONG
            deviceCredentialAvailable -> DEVICE_CREDENTIAL
            else -> 0
        }
    }

    fun buildPromptInfo(spec: PromptInfoSpec): BiometricPrompt.PromptInfo {
        val builder =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(spec.title)
                .setSubtitle(spec.subtitle)
                .setAllowedAuthenticators(spec.allowedAuthenticators)
        // PromptInfo.Builder.setNegativeButtonText is incompatible with DEVICE_CREDENTIAL: the
        // system supplies its own button when the device credential is allowed. Without it a
        // strong-only prompt on API 24-29 has no button and the framework prompt fails to build.
        if (!spec.allowedAuthenticators.hasDeviceCredential() && !spec.negativeButtonText.isNullOrEmpty()) {
            builder.setNegativeButtonText(spec.negativeButtonText)
        }
        if (spec.canUseAdvanced) {
            spec.description?.let { builder.setDescription(it) }
            spec.logoRes?.let { builder.setLogoRes(it) }
        }
        return builder.build()
    }

    private fun Int.hasDeviceCredential(): Boolean = this and DEVICE_CREDENTIAL != 0
}
