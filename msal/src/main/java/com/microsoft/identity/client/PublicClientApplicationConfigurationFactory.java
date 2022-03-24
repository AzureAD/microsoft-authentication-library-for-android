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
package com.microsoft.identity.client;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.client.internal.configuration.LogLevelDeserializer;
import com.microsoft.identity.common.AndroidPlatformComponents;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.AuthorityDeserializer;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudienceDeserializer;
import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.java.configuration.LibraryConfiguration;
import com.microsoft.identity.msal.R;
import com.microsoft.identity.common.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static com.microsoft.identity.client.internal.MsalUtils.validateNonNullArgument;

public class PublicClientApplicationConfigurationFactory {
    private static final String TAG = PublicClientApplicationConfigurationFactory.class.getSimpleName();
    private static final GlobalSettings globalSettings = GlobalSettings.getInstance();

    /**
     * Initializes a default PublicClientApplicationConfiguration object.
     **/
    @WorkerThread
    public static PublicClientApplicationConfiguration initializeConfiguration(@NonNull final Context context) {
        synchronized (globalSettings.getGlobalSettingsLock()) {
            return initializeConfigurationInternal(context, null);
        }
    }

    /**
     * Initializes a PublicClientApplicationConfiguration from the given configResourceId, if there is any,
     * and merge it to the default config object.
     **/
    @WorkerThread
    public static PublicClientApplicationConfiguration initializeConfiguration(@NonNull final Context context,
                                                                               final int configResourceId) {
        synchronized (globalSettings.getGlobalSettingsLock()) {
            return initializeConfigurationInternal(context, loadConfiguration(context, configResourceId));
        }
    }

    /**
     * Initializes a PublicClientApplicationConfiguration from the given file, if there is any,
     * and merge it to the default config object.
     **/
    @WorkerThread
    public static PublicClientApplicationConfiguration initializeConfiguration(@NonNull final Context context,
                                                                               @NonNull final File configFile) {
        synchronized (globalSettings.getGlobalSettingsLock()) {
            validateNonNullArgument(configFile, "configFile");
            return initializeConfigurationInternal(context, loadConfiguration(configFile));
        }
    }

    @WorkerThread
    private static PublicClientApplicationConfiguration initializeConfigurationInternal(@NonNull final Context context,
                                                                                        @Nullable final PublicClientApplicationConfiguration developerConfig) {
        validateNonNullArgument(context, "context");

        final PublicClientApplicationConfiguration config = loadDefaultConfiguration(context);
        if (developerConfig != null) {
            config.mergeConfiguration(developerConfig);
            config.validateConfiguration();
        }

        final PublicClientApplicationConfiguration configWithGlobal = mergeConfigurationWithGlobal(config);

        //Initialize internal library configuration
        final LibraryConfiguration libraryConfiguration = LibraryConfiguration.builder().authorizationInCurrentTask((configWithGlobal.authorizationInCurrentTask())).build();
        LibraryConfiguration.intializeLibraryConfiguration(libraryConfiguration);

        configWithGlobal.setOAuth2TokenCache(MsalOAuth2TokenCache.create(AndroidPlatformComponents.createFromContext(context)));
        return configWithGlobal;
    }

    @WorkerThread
    private static PublicClientApplicationConfiguration mergeConfigurationWithGlobal(final @NonNull PublicClientApplicationConfiguration developerConfig) {
        if (!globalSettings.isGlobalSettingsInitialized()) {
            Logger.warn(TAG + "mergeConfigurationWithGlobal",
                    GlobalSettings.NO_GLOBAL_SETTINGS_WARNING);
            return developerConfig;
        }

        developerConfig.mergeGlobalConfiguration(globalSettings.getGlobalSettingsConfiguration());

        return developerConfig;
    }

    @WorkerThread
    private static PublicClientApplicationConfiguration loadDefaultConfiguration(@NonNull final Context context) {
        final String methodName = ":loadDefaultConfiguration";
        Logger.verbose(TAG + methodName, "Loading default configuration");
        final PublicClientApplicationConfiguration config = loadConfiguration(context, R.raw.msal_default_config);
        config.setAppContext(context);

        return config;
    }

    @VisibleForTesting
    @WorkerThread
    static PublicClientApplicationConfiguration loadConfiguration(@NonNull final Context context,
                                                                  final int configResourceId) {
        final InputStream configStream = context.getResources().openRawResource(configResourceId);
        boolean useDefaultConfigResourceId = configResourceId == R.raw.msal_default_config;
        return loadConfiguration(configStream, useDefaultConfigResourceId);
    }

    @VisibleForTesting
    @WorkerThread
    static PublicClientApplicationConfiguration loadConfiguration(@NonNull final File configFile) {
        try {
            return loadConfiguration(new FileInputStream(configFile), false);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Provided configuration file path=" + configFile.getPath() + " not found.");
        }
    }

    @WorkerThread
    private static PublicClientApplicationConfiguration loadConfiguration(final @NonNull InputStream configStream,
                                                                          final boolean isDefaultConfiguration) {
        byte[] buffer;

        try {
            buffer = new byte[configStream.available()];
            configStream.read(buffer);
        } catch (IOException e) {
            if (isDefaultConfiguration) {
                throw new IllegalStateException("Unable to open default configuration file.", e);
            } else {
                throw new IllegalArgumentException("Unable to open provided configuration file.", e);
            }
        } finally {
            try {
                configStream.close();
            } catch (IOException e) {
                if (isDefaultConfiguration) {
                    Logger.warn(TAG + "loadConfiguration",
                            "Unable to close default configuration file. " +
                                    "This can cause memory leak."
                    );
                } else {
                    Logger.warn(TAG + "loadConfiguration",
                            "Unable to close provided configuration file. " +
                                    "This can cause memory leak."
                    );
                }
            }
        }

        final String config = new String(buffer);
        final Gson gson = getGsonForLoadingConfiguration();

        try {
            final PublicClientApplicationConfiguration configuration = gson.fromJson(config, PublicClientApplicationConfiguration.class);
            globalSettings.pcaHasBeenInitiated();
            return configuration;
        } catch (final Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalArgumentException("Error while processing configuration", e);
        }
    }

    private static Gson getGsonForLoadingConfiguration() {
        return new GsonBuilder()
                .registerTypeAdapter(
                        Authority.class,
                        new AuthorityDeserializer()
                )
                .registerTypeAdapter(
                        AzureActiveDirectoryAudience.class,
                        new AzureActiveDirectoryAudienceDeserializer()
                )
                .registerTypeAdapter(
                        com.microsoft.identity.client.Logger.LogLevel.class,
                        new LogLevelDeserializer()
                )
                .create();
    }
}
