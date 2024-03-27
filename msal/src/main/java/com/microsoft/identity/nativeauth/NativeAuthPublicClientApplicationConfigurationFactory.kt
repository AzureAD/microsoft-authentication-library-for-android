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
package com.microsoft.identity.nativeauth

import android.content.Context
import androidx.annotation.WorkerThread
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.microsoft.identity.client.PublicClientApplicationConfiguration
import com.microsoft.identity.client.PublicClientApplicationConfigurationFactory
import com.microsoft.identity.client.internal.configuration.LogLevelDeserializer
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudienceDeserializer
import com.microsoft.identity.common.java.nativeauth.BuildValues
import com.microsoft.identity.common.java.authorities.Authority
import com.microsoft.identity.common.java.authorities.AuthorityDeserializer
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAudience
import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache
import com.microsoft.identity.common.java.configuration.LibraryConfiguration
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAccount
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftRefreshToken
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse
import com.microsoft.identity.msal.R
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/**
 * NativeAuthPublicClientApplicationConfigurationFactory manages the initialization and loads the
 * configuration from a resource file for NativeAuthClient.
 */
class NativeAuthPublicClientApplicationConfigurationFactory :
    PublicClientApplicationConfigurationFactory() {

    companion object {
        private val TAG = NativeAuthPublicClientApplicationConfigurationFactory::class.java.simpleName

        /**
         * Initialize default [NativeAuthPublicClientApplicationConfiguration] object with default fields
         */
        @WorkerThread
        fun initializeNativeAuthConfiguration(context: Context): NativeAuthPublicClientApplicationConfiguration {
            return initializeNativeAuthConfigurationInternal(context, null)
        }

        /**
         * Initialize [NativeAuthPublicClientApplicationConfiguration] object from the provided configResourceId, if there is any,
         * and merge it with the default native auth config
         */
        @WorkerThread
        fun initializeNativeAuthConfiguration(context: Context, configResourceId: Int): NativeAuthPublicClientApplicationConfiguration {
            return initializeNativeAuthConfigurationInternal(context, loadConfiguration(context, configResourceId))
        }

        /**
         * Initialize [NativeAuthPublicClientApplicationConfiguration] object from the provided file, if there is any,
         * and merge it with the default native auth config
         */
        @WorkerThread
        fun initializeNativeAuthConfiguration(context: Context, configFile: File): NativeAuthPublicClientApplicationConfiguration {
            return initializeNativeAuthConfigurationInternal(context, loadConfiguration(configFile))
        }

        /**
         * Initialize the Native Auth configuration with base MSAL default configs and Native Auth default Configs
         */
        @WorkerThread
        private fun initializeNativeAuthConfigurationInternal(context: Context, developerConfig: NativeAuthPublicClientApplicationConfiguration?): NativeAuthPublicClientApplicationConfiguration {
            // This will create the default msal configuration
            val defaultMsalConfiguration: PublicClientApplicationConfiguration = PublicClientApplicationConfigurationFactory.loadConfiguration(context, R.raw.msal_default_config)

            // This will create the default native auth configuration
            val defaultNativeAuthConfiguration = loadDefaultNativeAuthConfiguration(context)

            // Empty configuration to use as basis for merging
            val config = NativeAuthPublicClientApplicationConfiguration()
            config.appContext = context

            // Merge base MSAL defaults
            config.mergeConfiguration(defaultMsalConfiguration)
            // Marge Native Auth defaults
            config.mergeConfiguration(defaultNativeAuthConfiguration)

            config.authorities.clear()

            // Check for developerConfig
            if (developerConfig != null) {
                // Merge developer Native Auth configuration
                config.mergeConfiguration(developerConfig)
                // Validate Native Auth configuration
                config.validateConfiguration()
            }

            // Initialize internal library configuration
            val libraryConfiguration = LibraryConfiguration.builder().authorizationInCurrentTask(config.authorizationInCurrentTask()).build()
            LibraryConfiguration.intializeLibraryConfiguration(libraryConfiguration)

            config.oAuth2TokenCache = createCache(
                AndroidPlatformComponentsFactory.createFromContext(context)
            )

            // Set build values if present in configuration
            initializeBuildValues(developerConfig)

            return config
        }

        private fun initializeBuildValues(developerConfig: NativeAuthPublicClientApplicationConfiguration?) {
            val dc = developerConfig?.dc
            val useMockAuthority = developerConfig?.useMockAuthority

            if (dc != null) {
                BuildValues.setDC(dc)
            }

            if (useMockAuthority != null) {
                BuildValues.setUseMockApiForNativeAuth(useMockAuthority)
            }
        }

        /**
         * This method loads the default Native Auth configuration
         */
        @WorkerThread
        private fun loadDefaultNativeAuthConfiguration(context: Context): NativeAuthPublicClientApplicationConfiguration {
            // Load and return default Native Auth config
            return loadConfiguration(context, R.raw.msal_native_auth_default_config)
        }

        /**
         * Tweaked loadConfiguration to return [NativeAuthPublicClientApplicationConfiguration]
         */
        @WorkerThread
        private fun loadConfiguration(
            context: Context,
            configResourceId: Int
        ): NativeAuthPublicClientApplicationConfiguration {
            val configStream = context.resources.openRawResource(configResourceId)
            val useDefaultConfigResourceId = (configResourceId == R.raw.msal_native_auth_default_config)
            return loadConfiguration(configStream, useDefaultConfigResourceId)
        }

        /**
         * Tweaked loadConfiguration to return [NativeAuthPublicClientApplicationConfiguration]
         */
        @WorkerThread
        private fun loadConfiguration(configFile: File): NativeAuthPublicClientApplicationConfiguration {
            return try {
                loadConfiguration(FileInputStream(configFile), false)
            } catch (e: FileNotFoundException) {
                throw java.lang.IllegalArgumentException("Provided configuration file path=" + configFile.getPath() + " not found.")
            }
        }

        /**
         * Tweaked loadConfiguration to serialize json into a [NativeAuthPublicClientApplicationConfiguration]
         * object.
         */
        @WorkerThread
        private fun loadConfiguration(
            configStream: InputStream,
            isDefaultConfiguration: Boolean
        ): NativeAuthPublicClientApplicationConfiguration {
            val buffer: ByteArray

            try {
                configStream.use {
                    buffer = ByteArray(configStream.available())
                    configStream.read(buffer)
                }
            } catch (e: IOException) {
                if (isDefaultConfiguration) {
                    throw IllegalStateException(
                        "Unable to open default native auth configuration file.", e)
                } else {
                    throw IllegalArgumentException(
                        "Unable to open provided native auth configuration file.", e)
                }
            }

            val config = String(buffer)
            val gson = getGsonForLoadingConfiguration()
            return try {
                gson.fromJson(config, NativeAuthPublicClientApplicationConfiguration::class.java)
            } catch (e: Exception) {
                if (e is InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                throw IllegalArgumentException("Error while processing configuration", e)
            }
        }

        // This method doesn't belong here, but in the MsalOAuth2Cache. However, we can't
        // access NativeOAuth2Strategy there, as it isn't in common
        private fun createCache(components: IPlatformComponents): MsalOAuth2TokenCache<
            MicrosoftStsOAuth2Strategy,
            MicrosoftStsAuthorizationRequest,
            MicrosoftStsTokenResponse,
            MicrosoftAccount,
            MicrosoftRefreshToken
            > {
            val methodName = ":createCache"
            Logger.verbose(
                TAG + methodName,
                "Creating MsalOAuth2TokenCache"
            )
            return MsalOAuth2TokenCache.create(components)
        }

        private fun getGsonForLoadingConfiguration(): Gson {
            return GsonBuilder()
                .registerTypeAdapter(
                    Authority::class.java,
                    AuthorityDeserializer()
                )
                .registerTypeAdapter(
                    AzureActiveDirectoryAudience::class.java,
                    AzureActiveDirectoryAudienceDeserializer()
                )
                .registerTypeAdapter(
                    com.microsoft.identity.client.Logger.LogLevel::class.java,
                    LogLevelDeserializer()
                )
                .create()
        }
    }
}
