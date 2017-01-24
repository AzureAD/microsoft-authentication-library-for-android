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
    private static final String TAG = PublicClientApplication.class.getSimpleName(); //NOPMD

    private static final String CLIENT_ID_META_DATA = "com.microsoft.identity.client.ClientId";
    private static final String AUTHORITY_META_DATA = "com.microsoft.identity.client.Authority";
    private static final String INTERNET_PERMISSION = "android.permission.INTERNET";
    private static final String ACCESS_NETWORK_STATE_PERMISSION = "android.permission.ACCESS_NETWORK_STATE";
    private static final String DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common/";

    private final Context mAppContext;
    private final Activity mActivity;

    private Authority mAuthority;
    private String mClientId;
    private final TokenCache mTokenCache;
    private String mRedirectUri;

    private boolean mValidateAuthority = true;
    private boolean mRestrictToSingleUser = false;

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
        mRedirectUri = createRedirectUri(mClientId);

        validateInputParameters();

        // Since network request is sent from the sdk, if calling app doesn't declare the internet permission in the
        // manifest, we cannot make the network call.
        checkInternetPermission();

        mTokenCache = new TokenCache(mAppContext);
    }

    /**
     * @return The current version for the sdk.
     */
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
     * @param restrictToSingleUser True if the application is on single user mode, false otherwise.
     */
    public void setRestrictToSingleUser(final boolean restrictToSingleUser) {
        mRestrictToSingleUser = restrictToSingleUser;
    }

    /**
     * Returns the list of signed in users for the application.
     * @return Immutable List of all the signed in users.
     * @throws AuthenticationException If failed to retrieve users from the cache.
     */
    public List<User> getUsers() throws AuthenticationException {
        return mTokenCache.getUsers(mClientId);
    }

    /**
     * Return the user object with specified user identifier. The user identifier could be either displayable id or
     * unique id.
     * @param userIdentifier The user identifier, could be either displayable id or unique id.
     * @return The {@link User} matching the user identifier.
     * @throws AuthenticationException if error happens when retrieving users from the cache.
     */
    public User getUser(final String userIdentifier) throws AuthenticationException {
        if (MSALUtils.isEmpty(userIdentifier)) {
            throw new IllegalArgumentException("invalid userIdentifier");
        }

        final List<User> allUsers = getUsers();
        for (final User user : allUsers) {
            if (userIdentifier.equals(user.getDisplayableId()) || userIdentifier.equals(user.getUniqueId())) {
                return user;
            }
        }

        // TODO: log no matching user exist.
        return null;
    }

    /**
     * The sdk requires calling app to pass in the {@link Activity} which <b> MUST </b> call this method to get the auth
     * code handled back correctly.
     * @param requestCode The request code for interactive request.
     * @param resultCode The result code for the request to get auth code.
     * @param data {@link Intent} either contains the url with auth code as query string or the errors.
     */
    public void handleInteractiveRequestRedirect(int requestCode, int resultCode, final Intent data) {
        InteractiveRequest.onActivityResult(requestCode, resultCode, data);
    }

    // Interactive APIs. Will launch the web UI.
    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     * Default value for {@link UIOptions} is {@link UIOptions#SELECT_ACCOUNT}.
     * @param scopes An array of scopes to acquire token for.
     * @param callback The {@link AuthenticationCallback} to receive the result back.
     *                 1) If user cancels the flow by pressing the device back button, the result will be sent
     *                 back via {@link AuthenticationCallback#onCancel()}.
     *                 2) If the sdk successfully receives the token back, result will be sent back via
     *                 {@link AuthenticationCallback#onSuccess(AuthenticationResult)}
     *                 3) All the other errors will be sent back via
     *                 {@link AuthenticationCallback#onError(AuthenticationException)}.
     */
    public void acquireToken(final String[] scopes, final AuthenticationCallback callback) {
        acquireTokenInteractive(scopes, "", UIOptions.SELECT_ACCOUNT, "", null, "", "", callback);
    }

    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     * Default value for {@link UIOptions} is {@link UIOptions#SELECT_ACCOUNT}.
     * @param scopes An array of scopes to acquire the token for.
     * @param loginHint Optional. If provided, will be used as the query parameter sent for authenticating the user,
     *                  which will have the UPN pre-populated.
     * @param callback The {@link AuthenticationCallback} to receive the result back.
     *                 1) If user cancels the flow by pressing the device back button, the result will be sent
     *                 back via {@link AuthenticationCallback#onCancel()}.
     *                 2) If the sdk successfully receives the token back, result will be sent back via
     *                 {@link AuthenticationCallback#onSuccess(AuthenticationResult)}
     *                 3) All the other errors will be sent back via
     *                 {@link AuthenticationCallback#onError(AuthenticationException)}.
     */
    public void acquireToken(final String[] scopes, final String loginHint,
                             final AuthenticationCallback callback) {
        acquireTokenInteractive(scopes, loginHint, UIOptions.SELECT_ACCOUNT, "", null, "", "", callback);
    }

    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     * Default value for {@link UIOptions} is {@link UIOptions#SELECT_ACCOUNT}.
     * @param scopes An array of scopes to acquire the token for.
     * @param loginHint Optional. If provided, will be used as the query parameter sent for authenticating the user,
     *                  which will have the UPN pre-populated.
     * @param uiOptions The {@link UIOptions} for prompting behavior. By default, the sdk use {@link UIOptions#SELECT_ACCOUNT}.
     * @param extraQueryParams Optional. The extra query parameter sent to authorize endpoint.
     * @param callback The {@link AuthenticationCallback} to receive the result back.
     *                 1) If user cancels the flow by pressing the device back button, the result will be sent
     *                 back via {@link AuthenticationCallback#onCancel()}.
     *                 2) If the sdk successfully receives the token back, result will be sent back via
     *                 {@link AuthenticationCallback#onSuccess(AuthenticationResult)}
     *                 3) All the other errors will be sent back via
     *                 {@link AuthenticationCallback#onError(AuthenticationException)}.
     */
    public void acquireToken(final String[] scopes, final String loginHint, final UIOptions uiOptions,
                             final String extraQueryParams, final AuthenticationCallback callback) {
        acquireTokenInteractive(scopes, loginHint, uiOptions == null ? UIOptions.SELECT_ACCOUNT : uiOptions,
                extraQueryParams, null, "", "", callback);
    }

    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     * Default value for {@link UIOptions} is {@link UIOptions#SELECT_ACCOUNT}.
     * @param scopes An array of scopes to acquire the token for.
     * @param loginHint Optional. If provided, will be used as the query parameter sent for authenticating the user,
     *                  which will have the UPN pre-populated.
     * @param uiOptions The {@link UIOptions} for prompting behavior. By default, the sdk use {@link UIOptions#SELECT_ACCOUNT}.
     * @param extraQueryParams Optional. The extra query parameter sent to authorize endpoint.
     * @param additionalScope Optional. The additional scope to consent for.
     * @param authority Should be set if developer wants to get token for a different authority url.
     * @param policy Optional. The policy to set for auth request.
     * @param callback The {@link AuthenticationCallback} to receive the result back.
     *                 1) If user cancels the flow by pressing the device back button, the result will be sent
     *                 back via {@link AuthenticationCallback#onCancel()}.
     *                 2) If the sdk successfully receives the token back, result will be sent back via
     *                 {@link AuthenticationCallback#onSuccess(AuthenticationResult)}
     *                 3) All the other errors will be sent back via
     *                 {@link AuthenticationCallback#onError(AuthenticationException)}.
     */
    public void acquireToken(final String[] scopes, final String loginHint, final UIOptions uiOptions,
                             final String extraQueryParams, final String[] additionalScope, final String authority,
                             final String policy, final AuthenticationCallback callback) {
        acquireTokenInteractive(scopes, loginHint, uiOptions == null ? UIOptions.SELECT_ACCOUNT : uiOptions,
                extraQueryParams, additionalScope, authority, policy, callback);
    }

    // Silent call APIs.
    /**
     * Perform acquire token silent call. If there is a valid AT in the cache, the sdk will return the silent AT; If
     * no valid AT exists, the sdk will try to find a RT and use the RT to get a new access token. If RT does not exist
     * or it fails to use RT for a new AT, exception will be sent back via callback.
     * @param scopes The array of scopes to silently get the token for.
     * @param callback {@link AuthenticationCallback} that is used to send the result back. The success result will be
     *                                               sent back via {@link AuthenticationCallback#onSuccess(AuthenticationResult)}.
     *                                               Failure case will be sent back via {
     *                                               @link AuthenticationCallback#onError(AuthenticationException)}.
     */
    public void acquireTokenSilentAsync(final String[] scopes, final AuthenticationCallback callback) {
        acquireTokenSilent(scopes, null, "", "", false, callback);
    }

    /**
     * Perform acquire token silent call. If there is a valid AT in the cache, the sdk will return the silent AT; If
     * no valid AT exists, the sdk will try to find a RT and use the RT to get a new access token. If RT does not exist
     * or it fails to use RT for a new AT, exception will be sent back via callback.
     * @param scopes The array of scopes to silently get the token for.
     * @param user {@link User} represents the user to silently be signed in.
     * @param callback {@link AuthenticationCallback} that is used to send the result back. The success result will be
     *                                               sent back via {@link AuthenticationCallback#onSuccess(AuthenticationResult)}.
     *                                               Failure case will be sent back via {
     *                                               @link AuthenticationCallback#onError(AuthenticationException)}.
     */
    public void acquireTokenSilentAsync(final String[] scopes, final User user,
                                        final AuthenticationCallback callback) {
        acquireTokenSilent(scopes, user, "", "", false, callback);
    }

    /**
     * Perform acquire token silent call. If there is a valid AT in the cache, the sdk will return the silent AT; If
     * no valid AT exists, the sdk will try to find a RT and use the RT to get a new access token. If RT does not exist
     * or it fails to use RT for a new AT, exception will be sent back via callback.
     * @param scopes The array of scopes to silently get the token for.
     * @param user {@link User} represents the user to silently be signed in.
     * @param authority (Optional) The alternate authority to get the token for. If not set, will use the default authority.
     * @param policy (Optional) The policy to set for auth request. The sdk will talk to b2c service if policy is set.
     * @param forceRefresh True if the request is forced to refresh, false otherwise.
     * @param callback {@link AuthenticationCallback} that is used to send the result back. The success result will be
     *                                               sent back via {@link AuthenticationCallback#onSuccess(AuthenticationResult)}.
     *                                               Failure case will be sent back via {
     *                                               @link AuthenticationCallback#onError(AuthenticationException)}.
     */
    public void acquireTokenSilentAsync(final String[] scopes, final User user, final String authority,
                                        final String policy, final boolean forceRefresh,
                                        final AuthenticationCallback callback) {
        acquireTokenSilent(scopes, user, authority, policy, forceRefresh, callback);
    }

    /**
     * Keep this method internal only to make it easy for MS apps to do serialize/deserialize on the family tokens.
     * @return The {@link TokenCache} that is used to persist token items for the running app.
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
            throw new IllegalArgumentException("No meta-data exists");
        }

        // read authority from manifest.
        final String authority = applicationInfo.metaData.getString(AUTHORITY_META_DATA);
        if (!MSALUtils.isEmpty(authority)) {
            mAuthority = Authority.createAuthority(authority, mValidateAuthority);
        } else {
            mAuthority = Authority.createAuthority(DEFAULT_AUTHORITY, mValidateAuthority);
        }

        // read client id from manifest
        final String clientId = applicationInfo.metaData.getString(CLIENT_ID_META_DATA);
        if (MSALUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("client id missing from manifest");
        }
        mClientId = clientId;

        // TODO: Comment out for now. As discussed, redirect should be computed during runtime, developer needs to put
//        final String redirectUri = applicationInfo.metaData.getString(REDIRECT_META_DATA);
//        if (!MSALUtils.isEmpty(redirectUri)) {
//            mRedirectUri = redirectUri;
//        }
    }

    // TODO: if no more input validation is needed, this could be moved back to the constructor.
    private void validateInputParameters() {
        if (!MSALUtils.hasCustomTabRedirectActivity(mAppContext, mRedirectUri)) {
            throw new IllegalStateException("App doesn't have the correct configuration for "
                    + BrowserTabActivity.class.getSimpleName() + ".");
        }
    }

    private void checkInternetPermission() {
        final PackageManager packageManager = mAppContext.getPackageManager();
        if (packageManager.checkPermission(INTERNET_PERMISSION, mAppContext.getPackageName())
                != PackageManager.PERMISSION_GRANTED
                || packageManager.checkPermission(ACCESS_NETWORK_STATE_PERMISSION, mAppContext.getPackageName())
                != PackageManager.PERMISSION_GRANTED) {
            throw new IllegalStateException("android.permission.Internet or android.permission.ACCESS_NETWORK_STATE is missing");
        }
    }

    /**
     * Redirect uri will the in the format of msauth-clientid://appPackageName.
     * The sdk will comupte the redirect when the PublicClientApplication is initialized.
     */
    private String createRedirectUri(final String clientId) {
        return "msauth-" + clientId + "://" + mAppContext.getPackageName();
    }


    private void acquireTokenInteractive(final String[] scopes, final String loginHint, final UIOptions uiOptions,
                                         final String extraQueryParams, final String[] additionalScope,
                                         final String authority, final String policy,
                                         final AuthenticationCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback is null");
        }

        final AuthenticationRequestParameters requestParameters = getRequestParameters(authority, scopes, loginHint,
                extraQueryParams, policy, uiOptions);

        final BaseRequest request = new InteractiveRequest(mActivity, requestParameters, additionalScope);
        request.getToken(null, callback);
    }

    private void acquireTokenSilent(final String[] scopes, final User user, final String authority,
                                    final String policy, final boolean forceRefresh,
                                    final AuthenticationCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback is null");
        }

        final Authority authorityForRequest = MSALUtils.isEmpty(authority) ? mAuthority
                : Authority.createAuthority(authority, mValidateAuthority);
        // set correlation if not developer didn't set it.
        final UUID correlationId = UUID.randomUUID();
        final Set<String> scopesAsSet = new HashSet<>(Arrays.asList(scopes));
        final AuthenticationRequestParameters requestParameters = AuthenticationRequestParameters.create(authorityForRequest, mTokenCache,
                scopesAsSet, mClientId, policy, mRestrictToSingleUser, correlationId);

        final BaseRequest request = new SilentRequest(mAppContext, requestParameters, forceRefresh, user);
        request.getToken(null, callback);
    }

    private AuthenticationRequestParameters getRequestParameters(final String authority, final String[] scopes,
                                                                 final String loginHint, final String extraQueryParam,
                                                                 final String policy, final UIOptions uiOption) {
        final Authority authorityForRequest = MSALUtils.isEmpty(authority) ? mAuthority
                : Authority.createAuthority(authority, mValidateAuthority);
        // set correlation if not developer didn't set it.
        final UUID correlationId = UUID.randomUUID();
        final Set<String> scopesAsSet = new HashSet<>(Arrays.asList(scopes));

        return AuthenticationRequestParameters.create(authorityForRequest, mTokenCache, scopesAsSet, mClientId,
                mRedirectUri, policy, mRestrictToSingleUser, loginHint, extraQueryParam, uiOption, correlationId);
    }
}