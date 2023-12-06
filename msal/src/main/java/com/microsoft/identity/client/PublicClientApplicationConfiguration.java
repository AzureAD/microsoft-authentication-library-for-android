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
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.PREFERRED_BROWSER;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.REDIRECT_URI;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.REQUIRED_BROKER_PROTOCOL_VERSION;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.TELEMETRY;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.USE_BROKER;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.WEBAUTHN_CAPABLE;
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
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;
import com.microsoft.identity.common.java.ui.BrowserDescriptor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.SecretKey;

public class PublicClientApplicationConfiguration {
    private static final String TAG = PublicClientApplicationConfiguration.class.getSimpleName();

    private static final String BROKER_REDIRECT_URI_SCHEME_AND_SEPARATOR = "msauth://";
    public static final String INVALID_REDIRECT_MSG = "Invalid, null, or malformed redirect_uri supplied";

    public static final class SerializedNames {
        static final String CLIENT_ID = "client_id";
        static final String REDIRECT_URI = "redirect_uri";
        static final String AUTHORITIES = "authorities";
        static final String AUTHORIZATION_USER_AGENT = "authorization_user_agent";
        static final String HTTP = "http";
        static final String LOGGING = "logging";
        static final String MULTIPLE_CLOUDS_SUPPORTED = "multiple_clouds_supported";
        static final String USE_BROKER = "broker_redirect_uri_registered";
        static final String ENVIRONMENT = "environment";
        static final String REQUIRED_BROKER_PROTOCOL_VERSION = "minimum_required_broker_protocol_version";
        static final String TELEMETRY = "telemetry";
        static final String PREFERRED_BROWSER = "preferred_browser";
        static final String BROWSER_SAFE_LIST = "browser_safelist";
        static final String ACCOUNT_MODE = "account_mode";
        static final String CLIENT_CAPABILITIES = "client_capabilities";
        static final String WEB_VIEW_ZOOM_CONTROLS_ENABLED = "web_view_zoom_controls_enabled";
        static final String WEB_VIEW_ZOOM_ENABLED = "web_view_zoom_enabled";
        static final String POWER_OPT_CHECK_FOR_NETWORK_REQUEST_ENABLED = "power_opt_check_for_network_req_enabled";
        static final String HANDLE_TASKS_WITH_NULL_TASKAFFINITY = "handle_null_taskaffinity";
        static final String AUTHORIZATION_IN_CURRENT_TASK = "authorization_in_current_task";
        static final String WEBAUTHN_CAPABLE = "webauthn_capable";
    }

    @SerializedName(CLIENT_ID)
    private String mClientId;

    @SerializedName(REDIRECT_URI)
    private String mRedirectUri;

    @SerializedName(AUTHORITIES)
    private List<Authority> mAuthorities;

    @SerializedName(AUTHORIZATION_USER_AGENT)
    private AuthorizationAgent mAuthorizationAgent;

    @SerializedName(HTTP)
    private HttpConfiguration mHttpConfiguration;

    @SerializedName(LOGGING)
    private LoggerConfiguration mLoggerConfiguration;

    @SerializedName(MULTIPLE_CLOUDS_SUPPORTED)
    private Boolean mMultipleCloudsSupported;

    @SerializedName(USE_BROKER)
    private Boolean mUseBroker;

    @SerializedName(ENVIRONMENT)
    private Environment mEnvironment;

    @SerializedName(REQUIRED_BROKER_PROTOCOL_VERSION)
    private String mRequiredBrokerProtocolVersion;

    @SerializedName(PREFERRED_BROWSER)
    private BrowserDescriptor mPreferredBrowser;

    @SerializedName(BROWSER_SAFE_LIST)
    private List<BrowserDescriptor> mBrowserSafeList;

    @SerializedName(TELEMETRY)
    private TelemetryConfiguration mTelemetryConfiguration;

    @SerializedName(ACCOUNT_MODE)
    private AccountMode mAccountMode;

    @SerializedName(CLIENT_CAPABILITIES)
    private String mClientCapabilities;

    @SerializedName(WEB_VIEW_ZOOM_CONTROLS_ENABLED)
    private Boolean webViewZoomControlsEnabled;

    @SerializedName(WEB_VIEW_ZOOM_ENABLED)
    private Boolean webViewZoomEnabled;

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

    /**
     * When set to true, a passkey option will be visible in WebView.
     * Host apps should set flag as true after ensuring they have provided the correct info for passkey support.
     */
    @SerializedName(WEBAUTHN_CAPABLE)
    private Boolean webauthnCapable;

    transient private OAuth2TokenCache mOAuth2TokenCache;

    transient private Context mAppContext;

    transient private boolean mIsSharedDevice = false;

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

    /**
     * Gets the preferred browser.
     *
     * @return The preferred browser to be used for auth flow.
     */
    public BrowserDescriptor getPreferredBrowser() {
        return mPreferredBrowser;
    }

    /**
     * Gets the list of browser safe list.
     *
     * @return The list of browser which are allowed to use for auth flow.
     */
    public List<BrowserDescriptor> getBrowserSafeList() {
        return mBrowserSafeList;
    }

    /**
     * Gets the currently configured client id for the PublicClientApplication.
     *
     * @return The configured clientId.
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * Sets the configured client id for the PublicClientApplication.
     *
     * @@param The configured clientId.
     */
    public void setClientId(final String clientId) {
        mClientId = clientId;
    }

    /**
     * Gets the list of authorities configured by the developer for use with the
     * PublicClientApplication.
     *
     * @return The List of current Authorities.
     */
    public List<Authority> getAuthorities() {
        return mAuthorities;
    }

    /**
     * Gets the environment (Production, PPE) that the public client application is registered in
     *
     * @return The environment
     */
    public Environment getEnvironment() {
        return mEnvironment;
    }

    /**
     * Gets the currently configured {@link HttpConfiguration} for the PublicClientApplication.
     *
     * @return The HttpConfiguration to use.
     */
    public HttpConfiguration getHttpConfiguration() {
        return this.mHttpConfiguration;
    }

    /**
     * Gets the currently configured {@link LoggerConfiguration} for the PublicClientApplication.
     *
     * @return The LoggerConfiguration to use.
     */
    public LoggerConfiguration getLoggerConfiguration() {
        return mLoggerConfiguration;
    }

    /**
     * Gets the currently configured {@link TelemetryConfiguration} for the PublicClientApplication.
     *
     * @return The TelemetryConfiguration to use.
     */
    public TelemetryConfiguration getTelemetryConfiguration() {
        return mTelemetryConfiguration;
    }

    /**
     * Gets the currently configured redirect uri for the PublicClientApplication.
     *
     * @return The redirectUri to use.
     */
    public String getRedirectUri() {
        return this.mRedirectUri;
    }

    /**
     * Sets the configured redirect uri for the PublicClientApplication.
     *
     * @param redirectUri The redirectUri to use.
     */
    public void setRedirectUri(@NonNull final String redirectUri) {
        this.mRedirectUri = redirectUri;
    }

    /**
     * Gets the currently configured {@link AuthorizationAgent} for the PublicClientApplication.
     *
     * @return The AuthorizationAgent to use.
     */
    public AuthorizationAgent getAuthorizationAgent() {
        return this.mAuthorizationAgent;
    }

    /**
     * Indicates whether the PublicClientApplication supports multiple clouds.  Automatic redirection to the cloud
     * associated with the authenticated user
     *
     * @return The boolean indicator of whether multiple clouds are supported by this application.
     */
    public Boolean getMultipleCloudsSupported() {
        return mMultipleCloudsSupported;
    }

    /**
     * Indicates whether the PublicClientApplication would like to leverage the broker if available.
     * <p>
     * The client must have registered
     *
     * @return The boolean indicator of whether or not to use the broker.
     */
    public Boolean getUseBroker() {
        return mUseBroker;
    }

    /**
     * Gets the currently configured {@link AccountMode} for the PublicClientApplication.
     *
     * @return The AccountMode supported by this application.
     */
    public AccountMode getAccountMode() {
        return this.mAccountMode;
    }

    /**
     * Sets the account mode for the PublicClientApplication.
     *
     * @param accountMode the account mode.
     */
    public void setAccountMode(final AccountMode accountMode) {
        this.mAccountMode = accountMode;
    }

    /**
     * Gets the currently configured capabilities for the PublicClientApplication.
     *
     * @return The capabilities supported by this application.
     */
    public String getClientCapabilities() {
        return this.mClientCapabilities;
    }

    /**
     * Indicates the minimum required broker protocol version number.
     *
     * @return String of broker protocol version
     */
    public String getRequiredBrokerProtocolVersion() {
        return mRequiredBrokerProtocolVersion;
    }

    public Context getAppContext() {
        return mAppContext;
    }

    void setAppContext(Context applicationContext) {
        mAppContext = applicationContext;
    }

    public OAuth2TokenCache getOAuth2TokenCache() {
        return mOAuth2TokenCache;
    }

    void setOAuth2TokenCache(OAuth2TokenCache tokenCache) {
        mOAuth2TokenCache = tokenCache;
    }

    public boolean getIsSharedDevice() {
        return mIsSharedDevice;
    }

    void setIsSharedDevice(boolean isSharedDevice) {
        mIsSharedDevice = isSharedDevice;
    }

    public boolean isWebViewZoomControlsEnabled() {
        return webViewZoomControlsEnabled;
    }

    public boolean isWebViewZoomEnabled() {
        return webViewZoomEnabled;
    }

    public void setWebViewZoomControlsEnabled(boolean webViewZoomControlsEnabled) {
        this.webViewZoomControlsEnabled = webViewZoomControlsEnabled;
    }

    public void setWebViewZoomEnabled(boolean webViewZoomEnabled) {
        this.webViewZoomEnabled = webViewZoomEnabled;
    }

    public Boolean isPowerOptCheckForEnabled() {
        return powerOptCheckEnabled;
    }

    public void setPowerOptCheckEnabled(Boolean powerOptCheckEnabled) {
        this.powerOptCheckEnabled = powerOptCheckEnabled;
    }

    public Boolean isHandleNullTaskAffinityEnabled() {
        return handleNullTaskAffinity;
    }

    public Boolean authorizationInCurrentTask() {
        return isAuthorizationInCurrentTask;
    }

    public Boolean isWebauthnCapable() {
        return Boolean.TRUE.equals(webauthnCapable);
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

    private void checkManifestPermissions() {
        if (this.handleNullTaskAffinity != null && this.handleNullTaskAffinity) {
            final PackageManager packageManager = mAppContext.getPackageManager();
            final int reorderTasksGranted = packageManager.checkPermission(Manifest.permission.REORDER_TASKS, mAppContext.getPackageName());
            if (reorderTasksGranted != PackageManager.PERMISSION_GRANTED) {
                throw new IllegalStateException("You requested that we handle null taskAffinity but your manifest does not include the REORDER_TASKS permission");
            }
        }
    }

    private void checkDefaultAuthoritySpecified() {
        if (mAuthorities != null && mAuthorities.size() > 1) {
            int defaultCount = 0;

            for (Authority authority : mAuthorities) {
                if (authority.getDefault()) {
                    defaultCount++;
                }
            }

            if (defaultCount == 0) {
                throw new IllegalArgumentException("One authority in your configuration must be marked as default.");
            }

            if (defaultCount > 1) {
                throw new IllegalArgumentException("More than one authority in your configuration is marked as default.  Only one authority may be default.");
            }
        }
    }

    public boolean isDefaultAuthorityConfigured() {
        Authority authority = getDefaultAuthority();

        if (authority != null) {
            return true;
        } else {
            return false;
        }
    }

    void mergeConfiguration(PublicClientApplicationConfiguration config) {
        this.mClientId = config.mClientId == null ? this.mClientId : config.mClientId;
        this.mRedirectUri = config.mRedirectUri == null ? this.mRedirectUri : config.mRedirectUri;
        this.mAuthorities = config.mAuthorities == null ? this.mAuthorities : config.mAuthorities;
        this.mAuthorizationAgent = config.mAuthorizationAgent == null ? this.mAuthorizationAgent : config.mAuthorizationAgent;
        this.mEnvironment = config.mEnvironment == null ? this.mEnvironment : config.mEnvironment;
        this.mHttpConfiguration = config.mHttpConfiguration == null ? this.mHttpConfiguration : config.mHttpConfiguration;
        this.mMultipleCloudsSupported = config.mMultipleCloudsSupported == null ? this.mMultipleCloudsSupported : config.mMultipleCloudsSupported;
        this.mUseBroker = config.mUseBroker == null ? this.mUseBroker : config.mUseBroker;
        this.mTelemetryConfiguration = config.mTelemetryConfiguration == null ? this.mTelemetryConfiguration : config.mTelemetryConfiguration;
        this.mRequiredBrokerProtocolVersion = config.mRequiredBrokerProtocolVersion == null ? this.mRequiredBrokerProtocolVersion : config.mRequiredBrokerProtocolVersion;
        this.mPreferredBrowser = config.mPreferredBrowser == null ? this.mPreferredBrowser : config.mPreferredBrowser;
        if (this.mBrowserSafeList == null) {
            this.mBrowserSafeList = config.mBrowserSafeList;
        } else if (config.mBrowserSafeList != null) {
            this.mBrowserSafeList.addAll(config.mBrowserSafeList);
        }
        // Multiple is the default mode.
        this.mAccountMode = config.mAccountMode != AccountMode.MULTIPLE && config.mAccountMode != null ? config.mAccountMode : this.mAccountMode;
        this.mClientCapabilities = config.mClientCapabilities == null ? this.mClientCapabilities : config.mClientCapabilities;
        this.mIsSharedDevice = config.mIsSharedDevice == true ? this.mIsSharedDevice : config.mIsSharedDevice;
        this.mLoggerConfiguration = config.mLoggerConfiguration == null ? this.mLoggerConfiguration : config.mLoggerConfiguration;
        this.webViewZoomControlsEnabled = config.webViewZoomControlsEnabled == null ? this.webViewZoomControlsEnabled : config.webViewZoomControlsEnabled;
        this.webViewZoomEnabled = config.webViewZoomEnabled == null ? this.webViewZoomEnabled : config.webViewZoomEnabled;
        this.powerOptCheckEnabled = config.powerOptCheckEnabled == null ? this.powerOptCheckEnabled : config.powerOptCheckEnabled;
        this.handleNullTaskAffinity = config.handleNullTaskAffinity == null ? this.handleNullTaskAffinity : config.handleNullTaskAffinity;
        this.isAuthorizationInCurrentTask = config.isAuthorizationInCurrentTask == null ? this.isAuthorizationInCurrentTask : config.isAuthorizationInCurrentTask;
        this.webauthnCapable = config.webauthnCapable == null ? this.webauthnCapable : config.webauthnCapable;
    }

    void validateConfiguration() {
        validateRedirectUri(mRedirectUri);
        nullConfigurationCheck(CLIENT_ID, mClientId);
        checkDefaultAuthoritySpecified();
        checkManifestPermissions();

        // Only validate the browser safe list configuration
        // when the authorization agent is set either DEFAULT or BROWSER.
        if (!mAuthorizationAgent.equals(AuthorizationAgent.WEBVIEW)
                && (mBrowserSafeList == null || mBrowserSafeList.isEmpty())) {
            throw new IllegalArgumentException(
                    "Null browser safe list configured."
            );
        }

        for (final Authority authority : mAuthorities) {
            if (authority instanceof UnknownAuthority) {
                throw new IllegalArgumentException(
                        "Unrecognized authority type -- null, invalid or unknown type specified."
                );
            }

            // validate the audience type of this Authority
            if (authority instanceof AzureActiveDirectoryAuthority) {
                validateAzureActiveDirectoryAuthority((AzureActiveDirectoryAuthority) authority);
            }
        }
    }

    private void validateRedirectUri(@NonNull final String redirectUri) {
        boolean isInvalid = false;
        if (mAppContext!= null && AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME.equalsIgnoreCase(mAppContext.getPackageName())) {
            isInvalid = !isValidAuthenticatorRedirectUri();
        } else {
            isInvalid = TextUtils.isEmpty(redirectUri) || !hasSchemeAndAuthority(redirectUri);
        }
        if (isInvalid) {
            throw new IllegalArgumentException(INVALID_REDIRECT_MSG);
        }
    }

    private boolean hasSchemeAndAuthority(@NonNull final String redirectUri) {
        final String methodTag = TAG + ":hasSchemeAndAuthority";
        try {
            final Uri parsedRedirectUri = Uri.parse(redirectUri);
            final boolean hasScheme = !TextUtils.isEmpty(parsedRedirectUri.getScheme());
            final boolean hasAuthority = !TextUtils.isEmpty(parsedRedirectUri.getAuthority());
            return hasScheme && hasAuthority;
        } catch (final NullPointerException e) {
            Logger.errorPII(methodTag, INVALID_REDIRECT_MSG, e);
        }

        return false;
    }

    private void validateAzureActiveDirectoryAuthority(@NonNull final AzureActiveDirectoryAuthority azureActiveDirectoryAuthority) {
        if (null != azureActiveDirectoryAuthority.mAudience
                && azureActiveDirectoryAuthority.mAudience instanceof UnknownAudience) {
            throw new IllegalArgumentException(
                    "Unrecognized audience type for AzureActiveDirectoryAuthority -- null, invalid, or unknown type specified"
            );
        }
    }

    private static void nullConfigurationCheck(String configKey, String configValue) {
        if (TextUtils.isEmpty(configValue)) {
            throw new IllegalArgumentException(configKey + " cannot be null.  Invalid configuration.");
        }
    }

    @VisibleForTesting
    public static boolean isBrokerRedirectUri(final @NonNull String redirectUri, final @NonNull String packageName) {
        final String potentialPrefix = BROKER_REDIRECT_URI_SCHEME_AND_SEPARATOR + packageName + "/";
        return redirectUri != null && redirectUri.startsWith(potentialPrefix);
    }

    // Verifies broker redirect URI against the app's signature, to make sure that this is legit.
    private void verifyRedirectUriWithAppSignature() throws MsalClientException {
        final String methodTag = TAG + ":verifyRedirectUriWithAppSignature";
        final String packageName = mAppContext.getPackageName();
        try {
            final PackageInfo info = PackageHelper.getPackageInfo(mAppContext.getPackageManager(), packageName);
            Signature[] signatures = PackageHelper.getSignatures(info);
            for (final Signature signature : signatures) {
                final MessageDigest messageDigest = MessageDigest.getInstance("SHA");
                messageDigest.update(signature.toByteArray());
                final String signatureHash = Base64.encodeToString(messageDigest.digest(), Base64.NO_WRAP);
                final Uri.Builder builder = new Uri.Builder();
                final Uri uri = builder.scheme("msauth")
                        .authority(packageName)
                        .appendPath(signatureHash)
                        .build();

                if (mRedirectUri.equalsIgnoreCase(uri.toString())) {
                    // Life is good.
                    return;
                } else {
                    throw new MsalClientException(
                            MsalClientException.REDIRECT_URI_VALIDATION_ERROR,
                            "The redirect URI in the configuration file doesn't match with the one " +
                                    "generated with package name and signature hash. Please verify " +
                                    "the uri in the config file and your app registration in Azure portal." +
                                    "We expected '" + uri.toString() + "' and we received '" + mRedirectUri + "'.");
                }
            }
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            Logger.error(methodTag, "Unexpected error in verifyRedirectUriWithAppSignature()", e);
            throw new MsalClientException(MsalClientException.UNKNOWN_ERROR, "Unexpected error in verifyRedirectUriWithAppSignature()", e);
        }
    }

    /**
     * Ensures that the developer has properly configured their
     * AndroidManifest to expose the BrowserTabActivity.
     *
     * @param context the context of the application
     * @param url     the redirect uri of the app
     * @return a boolean indicating if BrowserTabActivity is configured or not
     */
    private static boolean validateCustomTabRedirectActivity(@NonNull final Context context,
                                                             @NonNull final String url) throws MsalClientException {
        final String methodTag = TAG + ":validateCustomTabRedirectActivity";
        final PackageManager packageManager = context.getPackageManager();

        if (packageManager == null) {
            return false;
        }

        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setDataAndNormalize(Uri.parse(url));

        final List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(
                intent,
                PackageManager.GET_RESOLVED_FILTER
        );

        // resolve info list will never be null, if no matching activities are found, empty list will be returned.
        boolean hasActivity = false;

        for (final ResolveInfo info : resolveInfoList) {
            final ActivityInfo activityInfo = info.activityInfo;
            String activityClassName = BrowserTabActivity.class.getName();

            //If we're using authorization in current task... then we need to look for that activity
            if(LibraryConfiguration.getInstance().isAuthorizationInCurrentTask()){
                activityClassName = CurrentTaskBrowserTabActivity.class.getName();
            }

            if (activityInfo.name.equals(activityClassName) &&
                    activityInfo.packageName.equals(context.getPackageName())) {
                hasActivity = true;
            } else {
                // another application is listening for this url scheme, don't open
                // Custom Tab for security reasons
                com.microsoft.identity.common.logging.Logger.warn(
                        methodTag,
                        String.format("Another application %s is listening for the URL scheme %s", activityInfo.packageName, url)
                );
                throw new MsalClientException(
                        MsalClientException.MULTIPLE_APPS_LISTENING_CUSTOM_URL_SCHEME,
                        "More than one app is listening for the URL scheme defined for BrowserTabActivity in the AndroidManifest." +
                                " The package name of this other app is: " + activityInfo.packageName
                );
            }
        }

        return hasActivity;
    }

    @SuppressWarnings("PMD")
    public void checkIntentFilterAddedToAppManifestForBrokerFlow() throws MsalClientException {
        final String methodTag = TAG + ":checkIntentFilterAddedToAppManifestForBrokerFlow";
        if ((getAuthorizationAgent() == AuthorizationAgent.DEFAULT
                || getAuthorizationAgent() == AuthorizationAgent.BROWSER)) {

            final boolean hasCustomTabRedirectActivity = validateCustomTabRedirectActivity(
                    mAppContext,
                    mRedirectUri
            );

            if (!hasCustomTabRedirectActivity) {
                String activityClassName = BrowserTabActivity.class.getSimpleName();

                if (LibraryConfiguration.getInstance().isAuthorizationInCurrentTask()){
                    activityClassName = CurrentTaskBrowserTabActivity.class.getSimpleName();
                }

                final Uri redirectUri = Uri.parse(mRedirectUri);

                throw new MsalClientException(
                        APP_MANIFEST_VALIDATION_ERROR,
                        "Intent filter for: " +
                                activityClassName +
                                " is missing. " +
                                " Please make sure you have the following activity in your AndroidManifest.xml \n\n" +
                                "<activity android:name=\"com.microsoft.identity.client." + activityClassName + "\">" + "\n" +
                                "\t" + "<intent-filter>" + "\n" +
                                "\t\t" + "<action android:name=\"android.intent.action.VIEW\" />" + "\n" +
                                "\t\t" + "<category android:name=\"android.intent.category.DEFAULT\" />" + "\n" +
                                "\t\t" + "<category android:name=\"android.intent.category.BROWSABLE\" />" + "\n" +
                                "\t\t" + "<data" + "\n" +
                                "\t\t\t" + "android:host=\"" + redirectUri.getHost() + "\"" + "\n" +
                                "\t\t\t" + "android:path=\"" + redirectUri.getPath() + "\"" + "\n" +
                                "\t\t\t" + "android:scheme=\"" + redirectUri.getScheme() + "\" />" + "\n" +
                                "\t" + "</intent-filter>" + "\n" +
                                "</activity>" + "\n");
            }
        }

        if (!mUseBroker) {
            return;
        }

        if (mAppContext!=null && AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME.equalsIgnoreCase(mAppContext.getPackageName())) {
            if (isValidAuthenticatorRedirectUri()) {
                return;
            }
        }

        if (mAppContext != null && !isBrokerRedirectUri(mRedirectUri, mAppContext.getPackageName())) {
            // This means that the app is still using the legacy local-only MSAL Redirect uri (already removed from the new portal).
            // If this is the case, we can assume that the user doesn't need Broker support.
            Logger.warn(methodTag, "The app is still using legacy MSAL redirect uri. Switch to MSAL local auth."
                    + "  For brokered auth, the redirect URI is expected to conform to 'msauth://<authority>/.*' where the authority in "
                    + "that uri is the package name of the app. This package name is listed as 'applicationId' in the build.gradle file.");
            mUseBroker = false;
            return;
        }

        verifyRedirectUriWithAppSignature();
    }

    private boolean isValidAuthenticatorRedirectUri() {
        final String methodTag = TAG + ":isValidAuthenticatorRedirectUri";
        // This is a temporary fix to allow authenticator to migrate to MSAL
        // For Legacy reason Authenticator still needs to pass in the old redirect uri to be able to
        // have backward compatibility with older versions of BrokerHost (Company Portal)
        // We should remove this check after the new Broker Host apps are released to >90% of production
        // customer.
        // ADO workitem for tracking: https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1576096
        try {
            final PackageInfo info = mAppContext.getPackageManager().getPackageInfo(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME, PackageManager.GET_SIGNATURES);
            if (info != null && info.signatures != null && info.signatures.length > 0) {
                final Signature signature = info.signatures[0];

                final MessageDigest md_sha512 = MessageDigest.getInstance("SHA-512");
                md_sha512.update(signature.toByteArray());
                final String sha512_signingCertThumbprint = Base64.encodeToString(md_sha512.digest(), Base64.NO_WRAP);

                if (AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_RELEASE_SIGNATURE_SHA512.equalsIgnoreCase(sha512_signingCertThumbprint)
                        || AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_DEBUG_SIGNATURE_SHA512.equalsIgnoreCase(sha512_signingCertThumbprint)) {

                    // MSAL still uses SHA-1 format in redirect url.
                    final MessageDigest md_sha1 = MessageDigest.getInstance("SHA");
                    md_sha1.update(signature.toByteArray());
                    final String sha1_signingCertThumbprint = Base64.encodeToString(md_sha1.digest(), Base64.NO_WRAP);

                    final Uri.Builder builder = new Uri.Builder();
                    final Uri uri = builder.scheme("msauth")
                            .authority(mAppContext.getPackageName())
                            .appendPath(sha1_signingCertThumbprint)
                            .build();

                    if (mRedirectUri.equalsIgnoreCase(uri.toString()) ||
                            mRedirectUri.equalsIgnoreCase(AuthenticationConstants.Broker.BROKER_REDIRECT_URI) ||
                            mRedirectUri.equalsIgnoreCase(AuthenticationConstants.Broker.NEW_BROKER_REDIRECT_URI)) {
                        return true;
                    }
                }
            }
        } catch (final PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            Logger.error(methodTag, "Unexpected error in getting package info/signature for Authenticator", e);
        }

        return false;
    }
}
