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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Entry point for developer to create the public native application, and make API call to get token.
 */
public final class PublicClientApplication {
    private static final String TAG = PublicClientApplication.class.getSimpleName();

    private static final String DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common/";
    private static final String CLIENT_ID_META_DATA = "com.microsoft.identity.client.ClientId";
    private static final String AUTHORITY_META_DATA = "com.microsoft.identity.client.Authority";
    private static final String REDIRECT_META_DATA = "com.microsoft.identity.client.RedirectUri";

    private final Context mAppContext;
    private final Activity mActivity;

    private Authority mAuthority;
    private String mClientId;
    private final TokenCache mTokenCache;
    private final Settings mSettings;
    private String mRedirectUri;

    private boolean mValidateAuthority = true;
    private boolean mRestrictToSingleUser = false;
    private UUID mCorrelationId = null;

    /**
     * Constructor for {@link PublicClientApplication}.
     * Client id has be to set in the manifest as the meta data, name for client id in the metadata is:
     * "com.microsoft.identity.client.ClientId"
     * Redirect uri has to be set in the manifest as the meta data, name for redirect uri in metadata is:
     * "com.microsoft.identity.client.RedirectUri"
     * Authority can be set in the meta data, if not provided, the sdk will use the default authority.
     * @param activity The sdk requires the activity to be passed in when creating the {@link PublicClientApplication}.
     *                 For interactive request, the result has will be delivered back via the
     *                 {@link Activity#onActivityResult(int, int, Intent)}. Cannot be null.
     */
    public PublicClientApplication(@NonNull final Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("activity is null.");
        }

        mActivity = activity;
        mAppContext = activity.getApplicationContext();

        loadMetaDataFromManifest();

        validateInputParameters();

        mTokenCache = new TokenCache();
        mSettings = new Settings();
    }

    public static String getSdkVersion() {
        return "1.0.0";
    }

    /**
     * Authority validation is turned on by default. If developer wants to turn off the authority validation, use the
     * {@link PublicClientApplication#setValidateAuthority(boolean)} to set it as false.
     * @param validateAuthority True if to turn on authority validation, false otherwise. (By default, authority
     *                          validation is turned on.)
     */
    public void setValidateAuthority(final boolean validateAuthority) {
        mValidateAuthority = validateAuthority;
    }

    /**
     * Set the restrictToSingleUser. Default value is false.
     * If set, it means the sdk will only allow single unique id in the cache, which also means single user for single
     * tenant. And the sdk will 1) fail when loading the cache and it finds more than one entry with distinct unique
     * id. 2) fail when adding entry to the cache and the entry has a different unique id than the one in the cache
     * 3) cache look up
     * @param restrictTosingleUser
     */
    public void setRestrictToSingleUser(final boolean restrictTosingleUser) {
        mRestrictToSingleUser = restrictTosingleUser;
    }

    /**
     * @return {@link Settings} for customizing the authentication per application base. Developer will get the Settings
     * object and can call individual setter to customize the authentication setting.
     */
    public Settings getMSALCustomizedSetting() {
        return mSettings;
    }

    /**
     * Returns the list of signed in users.
     * @return Immutable List of all the signed in users.
     */
    public List<User> getUsers() {
        return null;
    }

    /**
     * Return the user object with specified user identifier. The user identifier could be either displayable id or
     * unique id.
     * @param userIdentifier The user identifier, could be either displayable id or unique id.
     * @return The {@link User} matching the user identifier.
     */
    public User getUser(final String userIdentifier) {
        // TODO: add the implementation for returning specific user.
        return null;
    }

    public void handleInteractiveRequestRedirect(int requestCode, int resultCode, final Intent data) {
        InteractiveRequest.onActivityResult(requestCode, resultCode, data);
    }

    // Interactive APIs. Will launch the web UI.
    /**
     * Acquire token interactively. Will pop the web UI.
     * Default value for {@link UIOptions} is {@link UIOptions#SELECT_ACCOUNT}.
     * Default value for redirect URI will be the default redirect uri "urn:ietf:wg:oauth:2.0:oob".
     * @param scopes
     * @param callback
     */
    // TODO: activity in the constructor
    public void acquireToken(final String[] scopes, final AuthenticationCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        acquireTokenInteractiveyly(scopes, "", UIOptions.SELECT_ACCOUNT, "", null, "", "", callback);
    }

    /**
     * TODO: add javadoc
     * @param scopes
     * @param loginHint
     * @param callback
     */
    public void acquireToken(final String[] scopes, final String loginHint,
                             final AuthenticationCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        acquireTokenInteractiveyly(scopes, loginHint, UIOptions.SELECT_ACCOUNT, "", null, "", "", callback);
    }

    public void acquireToken(final String[] scopes, final String loginHint, final UIOptions uiOptions,
                             final String extraQueryParams, final AuthenticationCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        acquireTokenInteractiveyly(scopes, loginHint, uiOptions, "", null, "", "", callback);
    }

    public void acquireToken(final String[] scopes, final String loginHint, final UIOptions uiOptions,
                             final String extraQueryParams, final String[] additionalScope, final String authority,
                             final String policy, final AuthenticationCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        acquireTokenInteractiveyly(scopes, loginHint, uiOptions, extraQueryParams, additionalScope, authority, policy,
                callback);
    }

    // Silent call APIs.
    /**
     * TODO: add javadoc
     * @param scopes
     * @param callback
     */
    public void acquireTokenSilentAsync(final String[] scopes, final AuthenticationCallback callback) { }

    /**
     * TODO: add javadoc
     * @param scopes
     * @param user
     * @param callback
     */
    public void acquireTokenSilentAsync(final String[] scopes, final User user,
                                        final AuthenticationCallback callback) {}

    /**
     * TODO: add java doc
     * @param scopes
     * @param user
     * @param authority
     * @param policy
     * @param forceRefresh
     * @param callback
     */
    public void acquireTokenSilentAsync(final String[] scopes, final User user, final String authority,
                                        final String policy, final boolean forceRefresh,
                                        final AuthenticationCallback callback) { }

    private void acquireTokenInteractiveyly(final String[] scopes, final String loginHint, final UIOptions uiOptions,
                                            final String extraQueryParams, final String[] additionalScope,
                                            final String authority, final String policy,
                                            final AuthenticationCallback callback) {
        final AuthenticationRequestParameters requestParameters = getRequestParameters(authority, scopes, loginHint,
                extraQueryParams, policy, uiOptions);

        final BaseRequest request = new InteractiveRequest(mActivity, requestParameters, additionalScope);
        request.getToken(callback);
    }

    private AuthenticationRequestParameters getRequestParameters(final String authority, final String[] scopes,
                                                                 final String loginHint, final String extraQueryParam,
                                                                 final String policy, final UIOptions uiOption) {
        final Authority authorityForRequest = MSALUtils.isEmpty(authority) ? mAuthority
                : new Authority(authority, mValidateAuthority);
        // set correlation if not developer didn't set it.
        if (mCorrelationId == null) {
            mCorrelationId = UUID.randomUUID();
        }

        final Set<String> scopesAsSet = new HashSet<>(Arrays.asList(scopes));

        return new AuthenticationRequestParameters(authorityForRequest, mTokenCache, scopesAsSet, mClientId,
                mRedirectUri, policy, mRestrictToSingleUser, loginHint, extraQueryParam, uiOption, mCorrelationId);
    }

    /**
     * Keep this method internal only to make it easy for MS apps to do serialize/deserialize on the family tokens.
     * @return
     */
    TokenCache getTokenCache() {
        return mTokenCache;
    }

    private void loadMetaDataFromManifest() {
        final ApplicationInfo applicationInfo;
        try {
            applicationInfo = mAppContext.getPackageManager().getApplicationInfo(
                    mAppContext.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("Unable to find the package info, unable to proceed");
        }

        if (applicationInfo == null || applicationInfo.metaData == null) {
            throw new IllegalArgumentException("No meta-data existed");
        }

        // read authority from manifest.
        final String authority = applicationInfo.metaData.getString(AUTHORITY_META_DATA);
        if (!MSALUtils.isEmpty(authority)) {
            mAuthority = new Authority(authority, mValidateAuthority);
        } else {
            mAuthority = new Authority(DEFAULT_AUTHORITY, mValidateAuthority);
        }

        // read client id from manifest
        final String clientId = applicationInfo.metaData.getString(CLIENT_ID_META_DATA);
        if (!MSALUtils.isEmpty(clientId)) {
            mClientId = clientId;
        }

        // read the redirect from manifest.
        final String redirectUri = applicationInfo.metaData.getString(REDIRECT_META_DATA);
        if (!MSALUtils.isEmpty(redirectUri)) {
            mRedirectUri = redirectUri;
        }
    }

    private void validateInputParameters() {
        if (MSALUtils.isEmpty(mClientId)) {
            throw new IllegalArgumentException("empty, null or blank client id.");
        }

        if (MSALUtils.isEmpty(mRedirectUri)) {
            throw new IllegalArgumentException("empty, null or blank redirect uri.");
        }

        // TODO: once we decide on the redirect uri format, validate here.
    }
}