//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.msal.automationapp.testpass.msalonly.nativeauth

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.identity.client.msal.automationapp.AbstractCrossAppUiTest
import com.microsoft.identity.client.ui.automation.annotations.DoNotRunOnPipeline
import com.microsoft.identity.client.ui.automation.app.NativeAuthSampleApp
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils
import com.microsoft.identity.common.java.nativeauth.BuildValues
import org.junit.Assert
import org.junit.Test

/**
 * UI automation test for NativeAuth sign-in with custom headers on the initiate endpoint.
 *
 * This test launches the NativeAuthSampleApp, navigates to the "Email & Password" fragment
 * (which sets custom headers via NativeAuthRequestInterceptor on the initiate call),
 * enters credentials from the lab vault, and attempts sign-in.
 */
@DoNotRunOnPipeline
class NativeAuthSignInTest : AbstractCrossAppUiTest() {

    companion object {
        private const val NATIVE_AUTH_SAMPLE_PACKAGE = NativeAuthSampleApp.NATIVE_AUTH_SAMPLE_PACKAGE_NAME
        private const val LAUNCH_TIMEOUT = 10_000L
        private const val SIGN_IN_TIMEOUT = 30_000L
    }

    /**
     * Test sign-in via NativeAuthSampleApp with custom headers set on initiate.
     *
     * Steps:
     * 1. Fetch CIAM tenant account credentials from the mobile build vault.
     * 2. Launch the NativeAuthSampleApp.
     * 3. Navigate to the "Email & Password" tab (which implements NativeAuthRequestInterceptor).
     * 4. Enter email and password.
     * 5. Tap "Sign In".
     * 6. Verify sign-in succeeds (access token displayed or sign-out button enabled).
     */
    @Test
    fun testSignInWithCustomHeadersOnInitiate() {
        // Step 1: Get NativeAuth config for client ID and authority
        val config = getNativeAuthConfig("SIGN_IN_PASSWORD")
        val clientId = config.clientId
        val authorityUrl = config.authorityUrl

        // Step 2: Get credentials - email from conf.json, password from Key Vault
        val username = config.email
        // Derive secret name from authority URL (e.g., "https://MSIDLABCIAM6.ciamlogin.com/..." -> "MSIDLABCIAM6")
        val secretName = authorityUrl.removePrefix("https://").substringBefore(".ciamlogin.com").uppercase()
        val password = mLabClient.getPasswordSecretFromLabsKeyVault(secretName)

        Assert.assertNotNull("Username should not be null", username)
        Assert.assertNotNull("Password should not be null", password)

        // Step 3: Launch the NativeAuthSampleApp with config
        launchNativeAuthSampleApp(clientId, authorityUrl)

        // Step 4: Navigate to "Email & Password" tab
        navigateToEmailPasswordTab()

        // Step 5: Enter credentials
        enterEmail(username)
        enterPassword(password)

        // Step 6: Tap Sign In
        tapSignIn()

        // Step 7: Verify sign-in result
        verifySignInSuccess()
    }

    private fun getNativeAuthConfig(configType: String): NativeAuthConfig {
        var configString = BuildValues.getNativeAuthConfigString()
        if (configString.isNullOrBlank()) {
            val filePath = BuildValues.getNativeAuthConfigFilePath()
            if (!filePath.isNullOrBlank()) {
                configString = java.io.File(filePath).readText()
            } else {
                throw IllegalStateException("Native auth config not found via build value or file path")
            }
        }
        configString = configString.replace("'", "\"")
        val type = TypeToken.getParameterized(
            Map::class.java,
            String::class.java,
            NativeAuthConfig::class.java
        ).type
        val configs: Map<String, NativeAuthConfig> = Gson().fromJson(configString, type)
        return configs[configType]
            ?: throw IllegalStateException("Config not found for $configType")
    }

    private data class NativeAuthConfig(
        val email: String = "",
        val client_id: String = "",
        val authority_url: String = ""
    ) {
        val clientId: String get() = client_id
        val authorityUrl: String get() = authority_url
    }

    private fun launchNativeAuthSampleApp(clientId: String, authorityUrl: String) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = context.packageManager.getLaunchIntentForPackage(NATIVE_AUTH_SAMPLE_PACKAGE)
            ?: throw AssertionError("Could not get launch intent for $NATIVE_AUTH_SAMPLE_PACKAGE. Is the app installed?")

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("native_auth_client_id", clientId)
        intent.putExtra("native_auth_authority_url", authorityUrl)

        context.startActivity(intent)

        mDevice.wait(Until.hasObject(By.pkg(NATIVE_AUTH_SAMPLE_PACKAGE as String).depth(0)), LAUNCH_TIMEOUT)
    }

    private fun navigateToEmailPasswordTab() {
        val emailPasswordTab = mDevice.wait(
            Until.findObject(By.desc("Email & Password")),
            LAUNCH_TIMEOUT
        ) ?: mDevice.wait(
            Until.findObject(By.text("Email & Password")),
            LAUNCH_TIMEOUT
        )

        Assert.assertNotNull("Could not find 'Email & Password' tab", emailPasswordTab)
        emailPasswordTab.click()

        mDevice.wait(
            Until.hasObject(By.res(NATIVE_AUTH_SAMPLE_PACKAGE, "email_text")),
            LAUNCH_TIMEOUT
        )
    }

    private fun enterEmail(email: String) {
        val emailField = UiAutomatorUtils.obtainUiObjectWithResourceId(
            "$NATIVE_AUTH_SAMPLE_PACKAGE:id/email_text"
        )
        emailField.clearTextField()
        emailField.setText(email)
    }

    private fun enterPassword(password: String) {
        val passwordField = UiAutomatorUtils.obtainUiObjectWithResourceId(
            "$NATIVE_AUTH_SAMPLE_PACKAGE:id/password_text"
        )
        passwordField.clearTextField()
        passwordField.setText(password)
    }

    private fun tapSignIn() {
        val signInButton = UiAutomatorUtils.obtainUiObjectWithResourceId(
            "$NATIVE_AUTH_SAMPLE_PACKAGE:id/sign_in"
        )
        signInButton.click()
    }

    private fun verifySignInSuccess() {
        val accessTokenResult = mDevice.wait(
            Until.findObject(By.res(NATIVE_AUTH_SAMPLE_PACKAGE, "result_access_token").textContains("Access token:")),
            SIGN_IN_TIMEOUT
        )

        if (accessTokenResult != null) {
            val text = accessTokenResult.text
            Assert.assertTrue(
                "Expected access token in result but got: $text",
                text.contains("Access token:") && text.length > "Access token: ".length
            )
        } else {
            val signOutButton = mDevice.wait(
                Until.findObject(By.res(NATIVE_AUTH_SAMPLE_PACKAGE, "sign_out").enabled(true)),
                SIGN_IN_TIMEOUT
            )
            Assert.assertNotNull(
                "Sign-in did not succeed: neither access token displayed nor sign-out button enabled",
                signOutButton
            )
        }
    }
}
