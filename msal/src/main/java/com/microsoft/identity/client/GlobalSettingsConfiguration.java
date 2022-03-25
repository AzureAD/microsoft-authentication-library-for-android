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
import com.microsoft.identity.client.configuration.AccountMode;
import com.microsoft.identity.client.configuration.HttpConfiguration;
import com.microsoft.identity.client.configuration.LoggerConfiguration;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.internal.authorities.UnknownAudience;
import com.microsoft.identity.common.internal.broker.PackageHelper;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.telemetry.TelemetryConfiguration;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.java.authorities.Environment;
import com.microsoft.identity.common.java.authorities.UnknownAuthority;
import com.microsoft.identity.common.java.configuration.LibraryConfiguration;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;
import com.microsoft.identity.common.java.ui.BrowserDescriptor;

import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.ACCOUNT_MODE;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.AUTHORITIES;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.AUTHORIZATION_IN_CURRENT_TASK;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.AUTHORIZATION_USER_AGENT;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.BROWSER_SAFE_LIST;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.CLIENT_CAPABILITIES;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.CLIENT_ID;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.ENVIRONMENT;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.HANDLE_TASKS_WITH_NULL_TASKAFFINITY;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.HTTP;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.LOGGING;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.MULTIPLE_CLOUDS_SUPPORTED;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.POWER_OPT_CHECK_FOR_NETWORK_REQUEST_ENABLED;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.REDIRECT_URI;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.REQUIRED_BROKER_PROTOCOL_VERSION;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.TELEMETRY;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.USE_BROKER;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.WEB_VIEW_ZOOM_CONTROLS_ENABLED;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.WEB_VIEW_ZOOM_ENABLED;
import static com.microsoft.identity.client.exception.MsalClientException.APP_MANIFEST_VALIDATION_ERROR;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.SecretKey;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class GlobalSettingsConfiguration {
    @SuppressWarnings("PMD")
    private static final String TAG = GlobalSettingsConfiguration.class.getSimpleName();
    private static final String BROKER_REDIRECT_URI_SCHEME_AND_SEPARATOR = "msauth://";
    public static final String INVALID_REDIRECT_MSG = "Invalid, null, or malformed redirect_uri supplied";

    /**
     * The currently configured client id for use with the {@link PublicClientApplication}.
     */
    @Getter
    @Setter
    @Accessors(prefix = "m")
    @SerializedName(CLIENT_ID)
    private String mClientId;

    /**
     * The currently configured redirect uri for use with the {@link PublicClientApplication}.
     */
    @Getter
    @Setter
    @Accessors(prefix = "m")
    @SerializedName(REDIRECT_URI)
    private String mRedirectUri;

    /**
     * The list of authorities configured by the developer for use with the {@link PublicClientApplication}.
     */
    @Getter
    @Accessors(prefix = "m")
    @SerializedName(AUTHORITIES)
    private List<Authority> mAuthorities;

    /**
     * The currently configured {@link AuthorizationAgent} for use with the {@link PublicClientApplication}.
     */
    @Getter
    @Accessors(prefix = "m")
    @SerializedName(AUTHORIZATION_USER_AGENT)
    private AuthorizationAgent mAuthorizationAgent;

    /**
     * The currently configured {@link HttpConfiguration} for use with the {@link PublicClientApplication}.
     */
    @Getter
    @Accessors(prefix = "m")
    @SerializedName(HTTP)
    private HttpConfiguration mHttpConfiguration;

    /**
     * The currently configured {@link LoggerConfiguration} for use with the {@link PublicClientApplication}.
     */
    @Getter
    @Accessors(prefix = "m")
    @SerializedName(LOGGING)
    private LoggerConfiguration mLoggerConfiguration;

    /**
     * Indicates whether the {@link PublicClientApplication} supports multiple clouds.  Automatic redirection to the cloud
     * associated with the authenticated user.
     */
    @Getter
    @Accessors(prefix = "m")
    @SerializedName(MULTIPLE_CLOUDS_SUPPORTED)
    private Boolean mMultipleCloudsSupported;

    /**
     * Indicates whether the {@link PublicClientApplication} would like to leverage the broker if available.
     * <p>
     * The client must have registered the device.
     */
    @Getter
    @Accessors(prefix = "m")
    @SerializedName(USE_BROKER)
    private Boolean mUseBroker;

    /**
     * The {@link Environment} (Production, PPE) that the {@link PublicClientApplication} is registered in.
     */
    @Getter
    @Accessors(prefix = "m")
    @SerializedName(ENVIRONMENT)
    private Environment mEnvironment;

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

    /**
     * The currently configured {@link AccountMode} for the {@link PublicClientApplication}.
     */
    @Getter
    @Accessors(prefix = "m")
    @SerializedName(ACCOUNT_MODE)
    private AccountMode mAccountMode;

    /**
     * The currently configured capabilities for the {@link PublicClientApplication}.
     */
    @Getter
    @Accessors(prefix = "m")
    @SerializedName(CLIENT_CAPABILITIES)
    private String mClientCapabilities;

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
    private Boolean isAuthorizationInCurrentTask;

    @Getter
    @Setter
    @Accessors(prefix = "m")
    transient private Context mAppContext;

    @Getter
    @Setter
    @Accessors(prefix = "m")
    transient private Boolean mIsSharedDevice = false;

    /**
     * Sets the secret key bytes to use when encrypting/decrypting cache entries.
     * {@link java.security.spec.KeySpec} algorithm is AES.
     *
     * @param rawKey The SecretKey to use in its primary encoding format.
     * @see SecretKey#getEncoded()
     */
    public void setTokenCacheSecretKeys(@NonNull final byte[] rawKey) {
        AuthenticationSettings.INSTANCE.setSecretKey(rawKey);
    }

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

    public Boolean authorizationInCurrentTask() {
        return isAuthorizationInCurrentTask;
    }

    public Authority getDefaultAuthority() {
        if (mAuthorities != null) {
            if (mAuthorities.size() > 1) {
                for (Authority authority : mAuthorities) {
                    if (authority.getDefault()) {
                        return authority;
                    }
                }
                return null; //This shouldn't happen since authority configuration is validated to ensure that one authority is marked as default/only one
            } else {
                return mAuthorities.get(0);
            }
        } else {
            return null;
        }
    }

    @VisibleForTesting
    public static boolean isBrokerRedirectUri(final @NonNull String redirectUri, final @NonNull String packageName) {
        final String potentialPrefix = BROKER_REDIRECT_URI_SCHEME_AND_SEPARATOR + packageName + "/";
        return redirectUri != null && redirectUri.startsWith(potentialPrefix);
    }
}
