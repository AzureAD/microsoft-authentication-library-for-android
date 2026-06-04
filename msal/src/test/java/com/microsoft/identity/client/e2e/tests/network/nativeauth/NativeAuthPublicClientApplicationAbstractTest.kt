// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.e2e.tests.network.nativeauth

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager
import com.microsoft.identity.client.e2e.tests.IPublicClientApplicationTest
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.common.internal.controllers.CommandDispatcherHelper
import com.microsoft.identity.common.java.nativeauth.BuildValues
import com.microsoft.identity.internal.testutils.TestUtils
import com.microsoft.identity.internal.testutils.nativeauth.ConfigType
import com.microsoft.identity.internal.testutils.nativeauth.api.models.NativeAuthTestConfig
import com.microsoft.identity.labapi.utilities.BuildConfig
import com.microsoft.identity.labapi.utilities.authentication.LabApiAuthenticationClient
import com.microsoft.identity.labapi.utilities.client.LabClient
import com.microsoft.identity.labapi.utilities.constants.UserType
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

// TODO: move to "PAUSED". A work in RoboTestUtils will be needed though.
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowAndroidSdkStorageEncryptionManager::class])
abstract class NativeAuthPublicClientApplicationAbstractTest : IPublicClientApplicationTest {
    companion object{
        const val SHARED_PREFERENCES_NAME = "com.microsoft.identity.client.account_credential_cache"
        const val INVALID_EMAIL = "email"
        const val INVALID_PASSWORD = "password"
        const val INCORRECT_CODE = "00000000"

        private val labApiAuthenticationClient: LabApiAuthenticationClient =
            LabApiAuthenticationClient(BuildConfig.LAB_CLIENT_SECRET)
        val labClient: LabClient = LabClient(labApiAuthenticationClient)
    }

    private lateinit var context: Context
    private lateinit var activity: Activity

    // Remove default Coroutine test timeout of 10 seconds.
    private val testDispatcher = StandardTestDispatcher()

    override fun getConfigFilePath(): String {
        return "" // Not needed for native auth flows
    }

    @Before
    open fun setup() {
        context = ApplicationProvider.getApplicationContext()
        activity = Mockito.mock(Activity::class.java)
        Mockito.`when`(activity.applicationContext).thenReturn(context)
        CommandDispatcherHelper.clear()
        Dispatchers.setMain(testDispatcher)
    }

    @After
    open fun cleanup() {
        // remove everything from cache after test ends
        TestUtils.clearCache(SHARED_PREFERENCES_NAME)
    }

    fun getSafePassword(): String {
        return labClient.getAccountFromLabJsonStringInMobileBuildVault(UserType.CIAM).password
    }

    private fun getConfigsThroughBuildValue(): Map<String, NativeAuthTestConfig.Config>? {
        var buildConfigString = BuildValues.getNativeAuthConfigString()
        // If the buildConfigString is null or empty, we will try to read the config from the file path
        if (buildConfigString.isNullOrBlank()) {
            val buildConfigFilePath = BuildValues.getNativeAuthConfigFilePath()
            // If the build config file path is set, read the file and set buildConfigString to its content
            if (buildConfigFilePath != null && buildConfigFilePath.isNotEmpty()) {
                buildConfigString = readConfigFile(buildConfigFilePath)
            } else {
                throw IllegalStateException("Native auth config file pipeline variable or local file path not found")
            }
        }
        val type = TypeToken.getParameterized(
            Map::class.java,
            String::class.java,
            NativeAuthTestConfig.Config::class.java
        ).type

        return Gson().fromJson(buildConfigString, type)
    }

    fun getConfig(configType: ConfigType): NativeAuthTestConfig.Config {
        val secretValue = getConfigsThroughBuildValue()
        return secretValue?.get(configType.stringValue)
            ?: throw IllegalStateException("Config not $secretValue")
    }

    fun setupPCA(config: NativeAuthTestConfig.Config, challengeTypes: List<String>, capabilities: List<String>): INativeAuthPublicClientApplication {
        return try {
            val parameters = NativeAuthPublicClientApplicationParameters(
                config.clientId,
                config.authorityUrl,
                challengeTypes
            )
            parameters.capabilities = capabilities

            PublicClientApplication.createNativeAuthPublicClientApplication(
                context,
                parameters
            )
        } catch (e: MsalException) {
            Assert.fail(e.message)
            throw e
        }
    }

    fun <T> retryOperation(
        maxRetries: Int = 5,
        authFlow: () -> T
    ) {
        var retryCount = 0
        var shouldRetry = true

        while (shouldRetry) {
            try {
                authFlow()
                shouldRetry = false // authFlow() has succeeded, so we don't need to retry.
            } catch (e: Exception) {
                //1secmail occasionally has a delay for emails to arrive / return from the API, or throws an internal server error, which causes tests to fail
                //In this case, retry the test
                if (retryCount >= maxRetries) {
                    Assert.fail(e.message)
                    shouldRetry = false
                } else {
                    retryCount++
                }
            }
        }
    }

    private fun readConfigFile(filePath: String): String {
        val sb = StringBuilder()
        try {
            FileInputStream(filePath).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        sb.append(line)
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: IOException) {
            android.util.Log.e("NativeAuthTest", "Failed to read config file: $filePath", e)
            throw RuntimeException("Error reading config file: $filePath", e)
        }
        return sb.toString()
    }
}