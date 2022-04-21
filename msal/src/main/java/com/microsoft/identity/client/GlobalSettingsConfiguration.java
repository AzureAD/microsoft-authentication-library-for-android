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

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.client.configuration.LoggerConfiguration;
import com.microsoft.identity.common.internal.telemetry.TelemetryConfiguration;
import com.microsoft.identity.common.java.ui.BrowserDescriptor;

import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.AUTHORIZATION_IN_CURRENT_TASK;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.BROWSER_SAFE_LIST;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.HANDLE_TASKS_WITH_NULL_TASKAFFINITY;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.LOGGING;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.POWER_OPT_CHECK_FOR_NETWORK_REQUEST_ENABLED;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.REQUIRED_BROKER_PROTOCOL_VERSION;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.TELEMETRY;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.WEB_VIEW_ZOOM_CONTROLS_ENABLED;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.WEB_VIEW_ZOOM_ENABLED;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

public class GlobalSettingsConfiguration {
    @SuppressWarnings("PMD")
    private static final String TAG = GlobalSettingsConfiguration.class.getSimpleName();

    /**
     * The currently configured {@link LoggerConfiguration} for use with the {@link PublicClientApplication}.
     */
    @Getter
    @Accessors(prefix = "m")
    @SerializedName(LOGGING)
    private LoggerConfiguration mLoggerConfiguration;

    /**
     * The minimum required broker protocol version number.
     */
    @Getter
    @Accessors(prefix = "m")
    @SerializedName(REQUIRED_BROKER_PROTOCOL_VERSION)
    private String mRequiredBrokerProtocolVersion;

    /**
     * The list of safe browsers.
     */
    @SerializedName(BROWSER_SAFE_LIST)
    @Getter
    @Accessors(prefix = "m")
    private List<BrowserDescriptor> mBrowserSafeList;

    /**
     * The currently configured {@link TelemetryConfiguration} for the {@link PublicClientApplication}.
     */
    @Getter
    @Accessors(prefix = "m")
    @SerializedName(TELEMETRY)
    private TelemetryConfiguration mTelemetryConfiguration;

    @Setter
    @SerializedName(WEB_VIEW_ZOOM_CONTROLS_ENABLED)
    private Boolean webViewZoomControlsEnabled;

    @Setter
    @SerializedName(WEB_VIEW_ZOOM_ENABLED)
    private Boolean webViewZoomEnabled;

    @Setter
    @SerializedName(POWER_OPT_CHECK_FOR_NETWORK_REQUEST_ENABLED)
    private Boolean powerOptCheckEnabled;

    @SerializedName(HANDLE_TASKS_WITH_NULL_TASKAFFINITY)
    private Boolean handleNullTaskAffinity;

    /**
     * Controls whether authorization activites are opened/created in the current task.
     * Current default as of MSAL 2.0.12 is to use a new task
     */
    @SerializedName(AUTHORIZATION_IN_CURRENT_TASK)
    private Boolean authorizationInCurrentTask;

    public Boolean isWebViewZoomControlsEnabled() {
        return webViewZoomControlsEnabled;
    }

    public Boolean isWebViewZoomEnabled() {
        return webViewZoomEnabled;
    }

    public Boolean isPowerOptCheckEnabled() {
        return powerOptCheckEnabled;
    }

    public Boolean isHandleNullTaskAffinityEnabled() {
        return handleNullTaskAffinity;
    }

    public Boolean isAuthorizationInCurrentTask() {
        return authorizationInCurrentTask;
    }

    void mergeConfiguration(final @NonNull GlobalSettingsConfiguration globalConfig) {
        this.mTelemetryConfiguration = globalConfig.getTelemetryConfiguration() == null ? this.mTelemetryConfiguration : globalConfig.getTelemetryConfiguration();
        this.mRequiredBrokerProtocolVersion = globalConfig.getRequiredBrokerProtocolVersion() == null ? this.mRequiredBrokerProtocolVersion : globalConfig.getRequiredBrokerProtocolVersion();
        this.mBrowserSafeList = globalConfig.getBrowserSafeList() == null ? this.mBrowserSafeList : globalConfig.getBrowserSafeList();
        this.mLoggerConfiguration = globalConfig.getLoggerConfiguration() == null ? this.mLoggerConfiguration : globalConfig.getLoggerConfiguration();
        this.webViewZoomControlsEnabled = globalConfig.isWebViewZoomControlsEnabled() == null ? this.webViewZoomControlsEnabled : globalConfig.isWebViewZoomControlsEnabled();
        this.webViewZoomEnabled = globalConfig.isWebViewZoomEnabled() == null ? this.webViewZoomEnabled : globalConfig.isWebViewZoomEnabled();
        this.powerOptCheckEnabled = globalConfig.isPowerOptCheckEnabled() == null ? this.powerOptCheckEnabled : globalConfig.isPowerOptCheckEnabled();
        this.handleNullTaskAffinity = globalConfig.isHandleNullTaskAffinityEnabled() == null ? this.handleNullTaskAffinity : globalConfig.isHandleNullTaskAffinityEnabled();
        this.authorizationInCurrentTask = globalConfig.isAuthorizationInCurrentTask() == null ? this.authorizationInCurrentTask : globalConfig.isAuthorizationInCurrentTask();
    }
}
