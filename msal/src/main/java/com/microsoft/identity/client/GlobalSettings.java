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

import static com.microsoft.identity.client.GlobalSettingsConfigurationFactory.initializeGlobalConfiguration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.logging.Logger;

import java.io.File;

/**
 * Singleton class used to initialize global configurations for the library.
 */
public class GlobalSettings {
    private static final String TAG = GlobalSettings.class.getSimpleName();
    public static final String NO_GLOBAL_SETTINGS_WARNING = "Global settings have not been initialized before the creation of PCA Configuration. Initializing global setting using default MSAL global configuration file.";
    public static final String GLOBAL_INIT_AFTER_PCA_ERROR_CODE = "pca_created_before_global";
    public static final String GLOBAL_INIT_AFTER_PCA_ERROR_MESSAGE = "Global initialization was attempted after a PublicClientApplicationConfiguration instance was already created. Please initialize global settings before any PublicClientApplicationConfiguration instance is created.";
    public static final String GLOBAL_ALREADY_INITIALIZED_ERROR_CODE = "global_already_initialized";
    public static final String GLOBAL_ALREADY_INITIALIZED_ERROR_MESSAGE = "Attempting to load global settings again after it has already been initialized.";

    /**
     * Singleton instance for this class, already initialized.
     */
    private final static GlobalSettings mGlobalSettingsSingleton = new GlobalSettings();

    /**
     * Holds configuration fields from a global config file.
     */
    private GlobalSettingsConfiguration mGlobalSettingsConfiguration;

    /**
     * Boolean showing if the global settings have been initialized.
     */
    private boolean mGlobalSettingsInitialized = false;

    /**
     * Shows if the global settings were initialized with the default configuration file.
     */
    private boolean mIsDefaulted = false;

    /**
     * Lock object for synchronizing pca creation and global settings initialization.
     */
    private final Object mGlobalSettingsLock = new Object();

    /**
     * Private Constructor for Singleton.
     */
    private GlobalSettings() {
        // Do nothing
    }

    /**
     * Load the global configuration file using the context, resource id of the configuration file, and a listener.
     *
     * @param context Context of the app.
     * @param configFileResourceId Resource Id for the configuration file.
     * @param listener Handles success and error messages.
     */
    @WorkerThread
    public static void loadGlobalConfigurationFile(@NonNull final Context context,
                                                   final int configFileResourceId,
                                                   @NonNull final GlobalSettingsListener listener) {
        synchronized (mGlobalSettingsSingleton.mGlobalSettingsLock) {
            if (mGlobalSettingsSingleton.mGlobalSettingsInitialized && !mGlobalSettingsSingleton.mIsDefaulted) {
                listener.onError(new MsalClientException(GLOBAL_ALREADY_INITIALIZED_ERROR_CODE,
                        GLOBAL_ALREADY_INITIALIZED_ERROR_MESSAGE));
                return;
            }

            if (mGlobalSettingsSingleton.mIsDefaulted) {
                listener.onError(new MsalClientException(GLOBAL_INIT_AFTER_PCA_ERROR_CODE,
                        GLOBAL_INIT_AFTER_PCA_ERROR_MESSAGE));
                return;
            }

            setGlobalConfiguration(
                    initializeGlobalConfiguration(context, configFileResourceId),
                    listener
            );
        }
    }

    /**
     * Load the global configuration file using the configuration file and a listener.
     *
     * @param configFile Configuration file.
     * @param listener Handles success and error messages.
     */
    @WorkerThread
    public static void loadGlobalConfigurationFile(@NonNull final Context context,
                                                   @NonNull final File configFile,
                                                   @NonNull final GlobalSettingsListener listener) {
        synchronized (mGlobalSettingsSingleton.mGlobalSettingsLock) {
            if (mGlobalSettingsSingleton.mGlobalSettingsInitialized && !mGlobalSettingsSingleton.mIsDefaulted) {
                listener.onError(new MsalClientException(GLOBAL_ALREADY_INITIALIZED_ERROR_CODE,
                        GLOBAL_ALREADY_INITIALIZED_ERROR_MESSAGE));
                return;
            }

            if (mGlobalSettingsSingleton.mIsDefaulted) {
                listener.onError(new MsalClientException(GLOBAL_INIT_AFTER_PCA_ERROR_CODE,
                        GLOBAL_INIT_AFTER_PCA_ERROR_MESSAGE));
                return;
            }

            setGlobalConfiguration(
                    initializeGlobalConfiguration(context, configFile),
                    listener
            );
        }
    }

    /**
     * Load global configuration file using the default configuration file.
     *
     * @param context Context of the App.
     */
    @WorkerThread
    private static void loadDefaultGlobalConfiguration(@NonNull final Context context) {
        synchronized (mGlobalSettingsSingleton.mGlobalSettingsLock) {
            setDefaultGlobalConfiguration(
                    initializeGlobalConfiguration(context)
            );
        }
    }

    @WorkerThread
    private static void setGlobalConfiguration(@NonNull final GlobalSettingsConfiguration globalConfiguration,
                                               @NonNull final GlobalSettingsListener listener) {
        mGlobalSettingsSingleton.mGlobalSettingsConfiguration = globalConfiguration;
        mGlobalSettingsSingleton.mGlobalSettingsInitialized = true;
        listener.onSuccess("Global configuration initialized.");
    }

    @WorkerThread
    private static void setDefaultGlobalConfiguration(@NonNull final GlobalSettingsConfiguration globalConfiguration) {
        mGlobalSettingsSingleton.mGlobalSettingsConfiguration = globalConfiguration;
        mGlobalSettingsSingleton.mGlobalSettingsInitialized = true;
        mGlobalSettingsSingleton.mIsDefaulted = true;
    }

    public GlobalSettingsConfiguration getGlobalSettingsConfiguration() {
        return mGlobalSettingsConfiguration;
    }

    @WorkerThread
    PublicClientApplicationConfiguration mergeConfigurationWithGlobal(final @NonNull PublicClientApplicationConfiguration developerConfig) {
        synchronized (mGlobalSettingsLock) {
            // If global has not been initialized, log warning and init with default configuration file
            if (!mGlobalSettingsInitialized) {
                Logger.warn(TAG + "mergeConfigurationWithGlobal",
                        GlobalSettings.NO_GLOBAL_SETTINGS_WARNING);

                loadDefaultGlobalConfiguration(developerConfig.getAppContext());
            }

            if (mIsDefaulted) {
                developerConfig.mergeDefaultGlobalConfiguration(mGlobalSettingsConfiguration);
            }
            else {
                developerConfig.mergeGlobalConfiguration(mGlobalSettingsConfiguration);
            }

            return developerConfig;
        }
    }

    static GlobalSettings getInstance() {
        return mGlobalSettingsSingleton;
    }

    /**
     * Used for resetting the static singleton instance when testing.
     */
    static void resetInstance() {
        mGlobalSettingsSingleton.mGlobalSettingsConfiguration = null;
        mGlobalSettingsSingleton.mGlobalSettingsInitialized = false;
        mGlobalSettingsSingleton.mIsDefaulted = false;
    }

    public interface GlobalSettingsListener {
        /**
         * Invoked if the global settings are initialized successfully.
         *
         * @param message A message showing successful initialization.
         */
        void onSuccess(@NonNull final String message);

        /**
         * Invoked if an error is encountered during the creation of the global configuration.
         *
         * @param exception Error exception.
         */
        void onError(@NonNull final MsalException exception);
    }
}
