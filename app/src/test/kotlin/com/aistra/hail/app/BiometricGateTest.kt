package com.aistra.hail.app

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BiometricGateTest {
    @Test
    fun `should not authenticate when biometric login is disabled`() {
        assertFalse(
            BiometricGate.shouldAuthenticate(
                biometricLogin = false,
                strongResult = BiometricManager.BIOMETRIC_SUCCESS,
                deviceCredentialResult = BiometricManager.BIOMETRIC_SUCCESS,
            ),
        )
    }

    @Test
    fun `should authenticate when strong biometric authentication succeeds`() {
        assertTrue(
            BiometricGate.shouldAuthenticate(
                biometricLogin = true,
                strongResult = BiometricManager.BIOMETRIC_SUCCESS,
                deviceCredentialResult = BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            ),
        )
    }

    @Test
    fun `should authenticate when device credential authentication succeeds`() {
        assertTrue(
            BiometricGate.shouldAuthenticate(
                biometricLogin = true,
                strongResult = BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                deviceCredentialResult = BiometricManager.BIOMETRIC_SUCCESS,
            ),
        )
    }

    @Test
    fun `should authenticate when both authenticators succeed`() {
        assertTrue(
            BiometricGate.shouldAuthenticate(
                biometricLogin = true,
                strongResult = BiometricManager.BIOMETRIC_SUCCESS,
                deviceCredentialResult = BiometricManager.BIOMETRIC_SUCCESS,
            ),
        )
    }

    @Test
    fun `should not authenticate when neither authenticator succeeds`() {
        assertFalse(
            BiometricGate.shouldAuthenticate(
                biometricLogin = true,
                strongResult = BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                deviceCredentialResult = BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            ),
        )
    }

    @Test
    fun `allowed authenticators selects strong biometric only`() {
        assertEquals(
            BIOMETRIC_STRONG,
            BiometricGate.allowedAuthenticators(
                strongResult = BiometricManager.BIOMETRIC_SUCCESS,
                deviceCredentialResult = BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            ),
        )
    }

    @Test
    fun `allowed authenticators selects device credential only`() {
        assertEquals(
            DEVICE_CREDENTIAL,
            BiometricGate.allowedAuthenticators(
                strongResult = BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                deviceCredentialResult = BiometricManager.BIOMETRIC_SUCCESS,
            ),
        )
    }

    @Test
    fun `allowed authenticators selects both`() {
        assertEquals(
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
            BiometricGate.allowedAuthenticators(
                strongResult = BiometricManager.BIOMETRIC_SUCCESS,
                deviceCredentialResult = BiometricManager.BIOMETRIC_SUCCESS,
            ),
        )
    }

    @Test
    fun `allowed authenticators is zero when neither is available`() {
        assertEquals(
            0,
            BiometricGate.allowedAuthenticators(
                strongResult = BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                deviceCredentialResult = BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            ),
        )
    }

    @Test
    fun `build prompt info always sets core fields`() {
        val promptInfo =
            BiometricGate.buildPromptInfo(
                BiometricGate.PromptInfoSpec(
                    allowedAuthenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
                    title = "title",
                    subtitle = "subtitle",
                    description = null,
                    negativeButtonText = "cancel",
                    logoRes = null,
                    canUseAdvanced = false,
                ),
            )

        assertEquals(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, promptInfo.allowedAuthenticators)
        assertEquals("title", promptInfo.title)
        assertEquals("subtitle", promptInfo.subtitle)
        assertEquals("cancel", promptInfo.negativeButtonText)
    }

    @Test
    fun `build prompt info includes advanced fields when permitted`() {
        val promptInfo =
            BiometricGate.buildPromptInfo(
                BiometricGate.PromptInfoSpec(
                    allowedAuthenticators = BIOMETRIC_STRONG,
                    title = "title",
                    subtitle = "subtitle",
                    description = "description",
                    negativeButtonText = "cancel",
                    logoRes = android.R.drawable.ic_dialog_info,
                    canUseAdvanced = true,
                ),
            )

        assertEquals("description", promptInfo.description?.toString())
        assertEquals(android.R.drawable.ic_dialog_info, promptInfo.logoRes)
    }

    @Test
    fun `build prompt info omits advanced fields when not permitted`() {
        val promptInfo =
            BiometricGate.buildPromptInfo(
                BiometricGate.PromptInfoSpec(
                    allowedAuthenticators = BIOMETRIC_STRONG,
                    title = "title",
                    subtitle = "subtitle",
                    description = "description",
                    negativeButtonText = "cancel",
                    logoRes = android.R.drawable.ic_dialog_info,
                    canUseAdvanced = false,
                ),
            )

        assertNull(promptInfo.description)
        assertEquals(-1, promptInfo.logoRes)
    }
}
