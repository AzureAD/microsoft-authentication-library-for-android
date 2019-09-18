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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.client.internal.configuration.LogLevelDeserializer;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AuthorityDeserializer;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudienceDeserializer;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.msal.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static com.microsoft.identity.client.internal.MsalUtils.validateNonNullArgument;

public class PublicClientApplicationConfigurationFactory {
    private static final String TAG = PublicClientApplicationConfigurationFactory.class.getSimpleName();

    /**
     * Initalizes a default PublicClientApplicationConfiguration object.
     **/
    public static PublicClientApplicationConfiguration initializeConfiguration(@NonNull final Context context) {
        return initializeConfigurationInternal(context, null);
    }

    /**
     * Initalizes a PublicClientApplicationConfiguration from the given configResourceId, if there is any,
     * and merge it to the default config object.
     **/
    public static PublicClientApplicationConfiguration initializeConfiguration(@NonNull final Context context,
                                                                               final int configResourceId) {
        return initializeConfigurationInternal(context, loadConfiguration(context, configResourceId));
    }

    /**
     * Initalizes a PublicClientApplicationConfiguration from the given file, if there is any,
     * and merge it to the default config object.
     **/
    public static PublicClientApplicationConfiguration initializeConfiguration(@NonNull final Context context,
                                                                               @NonNull final File configFile) {
        validateNonNullArgument(configFile, "configFile");
        return initializeConfigurationInternal(context, loadConfiguration(configFile));
    }

    private static PublicClientApplicationConfiguration initializeConfigurationInternal(@NonNull final Context context,
                                                                                        @Nullable final PublicClientApplicationConfiguration developerConfig) {
        validateNonNullArgument(context, "context");

        final PublicClientApplicationConfiguration config = loadDefaultConfiguration(context);
        if (developerConfig != null) {
            config.mergeConfiguration(developerConfig);
            config.validateConfiguration();
        }

        config.setOAuth2TokenCache(MsalOAuth2TokenCache.create(context));
        return config;
    }

    private static PublicClientApplicationConfiguration loadDefaultConfiguration(@NonNull final Context context) {
        final String methodName = ":loadDefaultConfiguration";
        com.microsoft.identity.common.internal.logging.Logger.verbose(
                TAG + methodName,
                "Loading default configuration"
        );
        final PublicClientApplicationConfiguration config = loadConfiguration(context, R.raw.msal_default_config);
        config.setAppContext(context);

        return config;
    }

    @VisibleForTesting
    static PublicClientApplicationConfiguration loadConfiguration(@NonNull final Context context,
                                                                  final int configResourceId) {
        final InputStream configStream = context.getResources().openRawResource(configResourceId);
        boolean useDefaultConfigResourceId = configResourceId == R.raw.msal_default_config;
        return loadConfiguration(configStream, useDefaultConfigResourceId);
    }

    @VisibleForTesting
    static PublicClientApplicationConfiguration loadConfiguration(@NonNull final File configFile) {
        try {
            return loadConfiguration(new FileInputStream(configFile), false);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Provided configuration file path=" + configFile.getPath() + " not found.");
        }
    }

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
                    com.microsoft.identity.common.internal.logging.Logger.warn(
                            TAG + "loadConfiguration",
                            "Unable to close default configuration file. This can cause memory leak."
                    );
                } else {
                    com.microsoft.identity.common.internal.logging.Logger.warn(
                            TAG + "loadConfiguration",
                            "Unable to close provided configuration file. This can cause memory leak."
                    );
                }
            }
        }

        final String config = new String(buffer);
        final Gson gson = getGsonForLoadingConfiguration();

        return gson.fromJson(config, PublicClientApplicationConfiguration.class);
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
                        Logger.LogLevel.class,
                        new LogLevelDeserializer()
                )
                .create();
    }
}
