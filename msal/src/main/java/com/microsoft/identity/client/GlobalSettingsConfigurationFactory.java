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
    public static GlobalSettingsConfiguration initializeGlobalConfiguration(@NonNull final Context context,
                                                                            final int configResourceId) {
        return loadConfiguration(context, configResourceId);
    }

    /**
     * Initializes a GlobalSettingsConfiguration from the given file, if there is any.
     **/
    @WorkerThread
    public static GlobalSettingsConfiguration initializeGlobalConfiguration(@NonNull final File configFile) {
        return loadConfiguration(configFile);
    }

    @VisibleForTesting
    @WorkerThread
    static GlobalSettingsConfiguration loadConfiguration(@NonNull final Context context,
                                                         final int configResourceId) {
        final InputStream configStream = context.getResources().openRawResource(configResourceId);
        return loadConfiguration(configStream);
    }

    @VisibleForTesting
    @WorkerThread
    static GlobalSettingsConfiguration loadConfiguration(@NonNull final File configFile) {
        try {
            return loadConfiguration(new FileInputStream(configFile));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Provided global configuration file path=" + configFile.getPath() + " not found.");
        }
    }

    @WorkerThread
    private static GlobalSettingsConfiguration loadConfiguration(final @NonNull InputStream configStream) {
        byte[] buffer;

        try {
            buffer = new byte[configStream.available()];
            configStream.read(buffer);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to open provided global configuration file.", e);
        } finally {
            try {
                configStream.close();
            } catch (IOException e) {
                Logger.warn(TAG + "loadConfiguration",
                        "Unable to close provided global configuration file. " +
                                "This can cause memory leak."
                );
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
