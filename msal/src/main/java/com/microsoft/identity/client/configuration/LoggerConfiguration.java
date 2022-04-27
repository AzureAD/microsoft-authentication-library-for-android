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
package com.microsoft.identity.client.configuration;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.common.globalsettings.GlobalSettings;

import static com.microsoft.identity.client.configuration.LoggerConfiguration.SerializedNames.LOGCAT_ENABLED;
import static com.microsoft.identity.client.configuration.LoggerConfiguration.SerializedNames.LOG_LEVEL;
import static com.microsoft.identity.client.configuration.LoggerConfiguration.SerializedNames.PII_ENABLED;

public class LoggerConfiguration {

    /**
     * Field names used for serialization by Gson.
     */
    public static final class SerializedNames {
        public static final String PII_ENABLED = "pii_enabled";
        public static final String LOG_LEVEL = "log_level";
        public static final String LOGCAT_ENABLED = "logcat_enabled";
    }

    @SerializedName(PII_ENABLED)
    private boolean mPiiEnabled;

    @SerializedName(LOG_LEVEL)
    private Logger.LogLevel mLogLevel;

    @SerializedName(LOGCAT_ENABLED)
    private boolean mLogcatEnabled;

    /**
     * Method to load configuration from global LoggerConfiguration in Common.
     * Will be removed when global fields are fully removed from PCAConfiguration.
     */
    public void loadFromGlobalLoggerConfiguration() {
        final com.microsoft.identity.common.internal.logging.LoggerConfiguration globalLoggerConfig = GlobalSettings.getInstance().getGlobalSettingsConfiguration().getLoggerConfiguration();

        mPiiEnabled = globalLoggerConfig.isPiiEnabled();
        mLogcatEnabled = globalLoggerConfig.isLogcatEnabled();
        switch (globalLoggerConfig.getLogLevel()) {
            case ERROR:
                mLogLevel = Logger.LogLevel.ERROR;
            case WARN:
                mLogLevel = Logger.LogLevel.WARNING;
            case VERBOSE:
                mLogLevel = Logger.LogLevel.VERBOSE;
            case INFO:
                mLogLevel = Logger.LogLevel.INFO;
        }
    }

    /**
     * Gets the Pii Enabled state.
     *
     * @return True if Pii logging is allowed. False otherwise.
     */
    public boolean isPiiEnabled() {
        return mPiiEnabled;
    }

    /**
     * Gets the {@link Logger.LogLevel} to use.
     *
     * @return The LogLevel.
     */
    public Logger.LogLevel getLogLevel() {
        return mLogLevel;
    }

    /**
     * Gets the Logcat enabled state.
     *
     * @return True if Logcat is enabled, false otherwise.
     */
    public boolean isLogcatEnabled() {
        return mLogcatEnabled;
    }
}
