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
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudienceDeserializer;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.AuthorityDeserializer;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.logging.Logger;
import com.microsoft.identity.msal.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class GlobalSettingsConfigurationFactory {
    private static final String TAG = GlobalSettingsConfigurationFactory.class.getSimpleName();

    /**
     * Initializes a GlobalSettingsConfiguration from the given configResourceId, if there is any.
     **/
    @WorkerThread
    public static GlobalSettingsConfiguration initializeGlobalConfiguration(@NonNull final Context context) {
        return initializeGlobalConfigurationInternal(context, null);
    }

    /**
     * Initializes a GlobalSettingsConfiguration from the given configResourceId, if there is any.
     **/
    @WorkerThread
    public static GlobalSettingsConfiguration initializeGlobalConfiguration(@NonNull final Context context,
                                                                            final int configResourceId) {
        return initializeGlobalConfigurationInternal(context, loadConfiguration(context, configResourceId));
    }

    /**
     * Initializes a GlobalSettingsConfiguration from the given file, if there is any.
     **/
    @WorkerThread
    public static GlobalSettingsConfiguration initializeGlobalConfiguration(@NonNull final Context context,
                                                                            @NonNull final File configFile) {
        return initializeGlobalConfigurationInternal(context, loadConfiguration(configFile));
    }

    @WorkerThread
    private static GlobalSettingsConfiguration initializeGlobalConfigurationInternal(@NonNull final Context context,
                                                                                     @Nullable final GlobalSettingsConfiguration developerConfig) {
        final GlobalSettingsConfiguration config = loadDefaultGlobalConfiguration(context);

        if (developerConfig != null){
            config.mergeConfiguration(developerConfig);
        }

        return config;
    }

    @WorkerThread
    private static GlobalSettingsConfiguration loadDefaultGlobalConfiguration(@NonNull final Context context) {
        final String methodTag = TAG + ":loadDefaultGlobalConfiguration";
        Logger.verbose(methodTag, "Loading default global configuration");
        return loadConfiguration(context, R.raw.msal_default_global_config);
    }

    @VisibleForTesting
    @WorkerThread
    static GlobalSettingsConfiguration loadConfiguration(@NonNull final Context context,
                                                         final int configResourceId) {
        final InputStream configStream = context.getResources().openRawResource(configResourceId);
        boolean useDefaultConfigResourceId = configResourceId == R.raw.msal_default_global_config;
        return loadConfiguration(configStream, useDefaultConfigResourceId);
    }

    @VisibleForTesting
    @WorkerThread
    static GlobalSettingsConfiguration loadConfiguration(@NonNull final File configFile) {
        try {
            return loadConfiguration(new FileInputStream(configFile), false);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Provided global configuration file path=" + configFile.getPath() + " not found.");
        }
    }

    @WorkerThread
    private static GlobalSettingsConfiguration loadConfiguration(final @NonNull InputStream configStream,
                                                                 final boolean isDefaultConfiguration) {
        final String methodTag = TAG + ":loadConfiguration";
        byte[] buffer;

        try {
            buffer = new byte[configStream.available()];
            configStream.read(buffer);
        } catch (IOException e) {
            if (isDefaultConfiguration) {
                throw new IllegalStateException("Unable to open default global configuration file.", e);
            } else {
                throw new IllegalArgumentException("Unable to open provided global configuration file.", e);
            }
        } finally {
            try {
                configStream.close();
            } catch (IOException e) {
                if (isDefaultConfiguration) {
                    Logger.warn(methodTag,
                            "Unable to close default global configuration file. " +
                                    "This can cause memory leak."
                    );
                } else {
                    Logger.warn(methodTag,
                            "Unable to close provided global configuration file. " +
                                    "This can cause memory leak."
                    );
                }
            }
        }

        final String config = StringUtil.fromByteArray(buffer);
        final Gson gson = getGsonForLoadingConfiguration();

        try {
            return gson.fromJson(config, GlobalSettingsConfiguration.class);
        } catch (final Exception e) {
            throw new IllegalArgumentException("Error while processing global configuration", e);
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
