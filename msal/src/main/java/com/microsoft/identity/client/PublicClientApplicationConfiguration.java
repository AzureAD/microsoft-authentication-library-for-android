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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.client.configuration.AccountMode;
import com.microsoft.identity.client.configuration.HttpConfiguration;
import com.microsoft.identity.client.configuration.LoggerConfiguration;
import com.microsoft.identity.client.internal.MsalUtils;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.authorities.Environment;
import com.microsoft.identity.common.internal.authorities.UnknownAudience;
import com.microsoft.identity.common.internal.authorities.UnknownAuthority;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.telemetry.TelemetryConfiguration;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.ui.browser.BrowserDescriptor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;

import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.ACCOUNT_MODE;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.AUTHORITIES;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.AUTHORIZATION_USER_AGENT;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.BROWSER_SAFE_LIST;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.CLIENT_CAPABILITIES;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.CLIENT_ID;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.ENVIRONMENT;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.HTTP;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.LOGGING;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.MULTIPLE_CLOUDS_SUPPORTED;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.REDIRECT_URI;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.REQUIRED_BROKER_PROTOCOL_VERSION;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.TELEMETRY;
import static com.microsoft.identity.client.PublicClientApplicationConfiguration.SerializedNames.USE_BROKER;

public class PublicClientApplicationConfiguration {
    private static final String TAG = PublicClientApplicationConfiguration.class.getSimpleName();

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
        static final String BROWSER_SAFE_LIST = "browser_safelist";
        static final String ACCOUNT_MODE = "account_mode";
        static final String CLIENT_CAPABILITIES = "client_capabilities";
    }

    @SerializedName(CLIENT_ID)
    String mClientId;

    @SerializedName(REDIRECT_URI)
    String mRedirectUri;

    @SerializedName(AUTHORITIES)
    List<Authority> mAuthorities;

    @SerializedName(AUTHORIZATION_USER_AGENT)
    AuthorizationAgent mAuthorizationAgent;

    @SerializedName(HTTP)
    HttpConfiguration mHttpConfiguration;

    @SerializedName(LOGGING)
    LoggerConfiguration mLoggerConfiguration;

    @SerializedName(MULTIPLE_CLOUDS_SUPPORTED)
    Boolean mMultipleCloudsSupported;

    @SerializedName(USE_BROKER)
    Boolean mUseBroker;

    @SerializedName(ENVIRONMENT)
    Environment mEnvironment;

    @SerializedName(REQUIRED_BROKER_PROTOCOL_VERSION)
    String mRequiredBrokerProtocolVersion;

    @SerializedName(BROWSER_SAFE_LIST)
    List<BrowserDescriptor> mBrowserSafeList;

    @SerializedName(TELEMETRY)
    TelemetryConfiguration mTelemetryConfiguration;

    @SerializedName(ACCOUNT_MODE)
    AccountMode mAccountMode;

    @SerializedName(CLIENT_CAPABILITIES)
    String mClientCapabilities;

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

    OAuth2TokenCache getOAuth2TokenCache() {
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
        if (this.mBrowserSafeList == null) {
            this.mBrowserSafeList = config.mBrowserSafeList;
        } else if (config.mBrowserSafeList != null) {
            this.mBrowserSafeList.addAll(config.mBrowserSafeList);
        }

        // Multiple is the default mode.
        this.mAccountMode = config.mAccountMode != AccountMode.MULTIPLE ? config.mAccountMode : this.mAccountMode;
        this.mClientCapabilities = config.mClientCapabilities == null ? this.mClientCapabilities : config.mClientCapabilities;
        this.mIsSharedDevice = config.mIsSharedDevice == true ? this.mIsSharedDevice : config.mIsSharedDevice;
        this.mLoggerConfiguration = config.mLoggerConfiguration == null ? this.mLoggerConfiguration : config.mLoggerConfiguration;
    }

    void validateConfiguration() {
        nullConfigurationCheck(REDIRECT_URI, mRedirectUri);
        nullConfigurationCheck(CLIENT_ID, mClientId);
        checkDefaultAuthoritySpecified();

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

    private void validateAzureActiveDirectoryAuthority(@NonNull final AzureActiveDirectoryAuthority azureActiveDirectoryAuthority) {
        if (null != azureActiveDirectoryAuthority.mAudience
                && azureActiveDirectoryAuthority.mAudience instanceof UnknownAudience) {
            throw new IllegalArgumentException(
                    "Unrecognized audience type for AzureActiveDirectoryAuthority -- null, invalid, or unknown type specified"
            );
        }
    }

    private void nullConfigurationCheck(String configKey, String configValue) {
        if (configValue == null) {
            throw new IllegalArgumentException(configKey + " cannot be null.  Invalid configuration.");
        }
    }

    private boolean isBrokerRedirectUri() {
        final String BROKER_REDIRECT_URI_REGEX = "msauth://" + mAppContext.getPackageName() + "/.*";
        final Pattern pairRegex = Pattern.compile(BROKER_REDIRECT_URI_REGEX);
        final Matcher matcher = pairRegex.matcher(mRedirectUri);
        return matcher.matches();
    }

    // Verifies broker redirect URI against the app's signature, to make sure that this is legit.
    private void verifyRedirectUriWithAppSignature() {
        final String packageName = mAppContext.getPackageName();
        try {
            final PackageInfo info = mAppContext.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            for (final Signature signature : info.signatures) {
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
                }
            }
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            Logger.error(TAG, "Unexpected error in verifyRedirectUriWithAppSignature()", e);
        }

        throw new IllegalStateException("The redirect URI in the configuration file doesn't match with the one " +
                "generated with package name and signature hash. Please verify the uri in the config file and your app registration in Azure portal.");
    }

    @SuppressWarnings("PMD")
    public void checkIntentFilterAddedToAppManifestForBrokerFlow() {
        if (!mUseBroker) {
            return;
        }

        if (!isBrokerRedirectUri()) {
            // This means that the app is still using the legacy local-only MSAL Redirect uri (already removed from the new portal).
            // If this is the case, we can assume that the user doesn't need Broker support.
            Logger.info(TAG, "The app is still using legacy MSAL redirect uri. Switch to MSAL local auth.");
            mUseBroker = false;
            return;
        }

        verifyRedirectUriWithAppSignature();

        final boolean hasCustomTabRedirectActivity = MsalUtils.hasCustomTabRedirectActivity(
                mAppContext,
                mRedirectUri
        );

        if (!hasCustomTabRedirectActivity) {
            final Uri redirectUri = Uri.parse(mRedirectUri);
            throw new IllegalStateException(
                    "Intent filter for: " +
                            BrowserTabActivity.class.getSimpleName() +
                            " is missing. " +
                            " Please make sure you have the following activity in your AndroidManifest.xml \n\n" +
                            "<activity android:name=\"com.microsoft.identity.client.BrowserTabActivity\">" + "\n" +
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

}
