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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Base request class for handling either interactive request or silent request.
 */
abstract class BaseRequest {
    private static final String TAG = BaseRequest.class.getSimpleName();
    private static final ExecutorService THREAD_EXECUTOR = Executors.newSingleThreadExecutor();
    private Handler mHandler;
    private final RequestContext mRequestContext;

    protected final AuthenticationRequestParameters mAuthRequestParameters;
    protected final Context mContext;
    protected int mRequestId;
    protected TokenResponse mTokenResponse;

    /**
     * Abstract method, implemented by subclass for its own logic before the token request.
     * @throws MSALUserCancelException If pre token request fails as user cancels the flow.
     * @throws MsalException If error happens during the pre-process.
     */
    abstract void preTokenRequest() throws MsalUiRequiredException, MSALUserCancelException,
            MsalServiceException, MsalClientException;

    /**
     * Abstract method to set the additional body parameters for specific request.
     * @param oauth2Client
     */
    abstract void setAdditionalOauthParameters(final Oauth2Client oauth2Client);

    /**
     * Constructor for abstract {@link BaseRequest}.
     * @param appContext The app running context.
     * @param authenticationRequestParameters The {@link AuthenticationRequestParameters} used to create request.
     */
    BaseRequest(final Context appContext, final AuthenticationRequestParameters authenticationRequestParameters) {
        mContext = appContext;
        mAuthRequestParameters = authenticationRequestParameters;
        mRequestContext = authenticationRequestParameters.getRequestContext();

        if (authenticationRequestParameters.getScope() == null
                || authenticationRequestParameters.getScope().isEmpty()) {
            throw new IllegalArgumentException("scope is empty or null");
        }

        validateInputScopes(authenticationRequestParameters.getScope());
    }

    /**
     * Perform the whole acquire token request.
     * 1. preTokenRequest.
     * 1) for interactive request, preToken is launching the web ui. The end result should be auth code or error result
     * 2) for silent request, preToken is doing cache lookup. If there is a valid AT returned, we should return the AT.
     * If there is a RT returned, we should use it to token acquisition.
     * 2. performTokenRequest. Use either auth code or RT found in the preTokenRequest to get a new token.
     * 3. Post token request, store the returned token into cache.
     * @param callback The {@link AuthenticationCallback} to deliver the result back.
     */
    void getToken(final AuthenticationCallback callback) {
        mRequestId = callback.hashCode();
        THREAD_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // perform authority validation before doing any token request
                    mAuthRequestParameters.getAuthority().resolveEndpoints(
                            mAuthRequestParameters.getRequestContext(),
                            mAuthRequestParameters.getLoginHint()
                    );
                    preTokenRequest();
                    performTokenRequest();

                    final AuthenticationResult result = postTokenRequest();

                    Logger.info(TAG, mAuthRequestParameters.getRequestContext(), "Token request succeeds.");
                    callbackOnSuccess(callback, result);
                } catch (final MSALUserCancelException userCancelException) {
                    Logger.error(TAG, mAuthRequestParameters.getRequestContext(), "User cancelled the flow.",
                            userCancelException);
                    callbackOnCancel(callback);
                } catch (final MsalException msalException) {
                    Logger.error(TAG, mAuthRequestParameters.getRequestContext(), "Error occurred during authentication.",
                            msalException);
                    callbackOnError(callback, msalException);
                }
            }
        });
    }

    /**
     * Get the decorated scopes. Will combine the input scope and the reserved scope. If client id is provided as scope,
     * it will be removed from the combined scopes.
     * @param inputScopes The input scopes to decorate.
     * @return The combined scopes.
     */
    Set<String> getDecoratedScope(final Set<String> inputScopes) {
        final Set<String> scopes = new HashSet<>(inputScopes);
        final Set<String> reservedScopes = getReservedScopesAsSet();
        scopes.addAll(reservedScopes);
        scopes.remove(mAuthRequestParameters.getClientId());

        return scopes;
    }

    /**
     * Validate the input scopes. The input scope cannot have reserved scopes, if client id is provided as the scope it
     * should be a single scope.
     * @param inputScopes The input set of scope to validate.
     */
    void validateInputScopes(final Set<String> inputScopes) {
        final Set<String> scopes = new HashSet<>(inputScopes);
        final Set<String> reservedScopes = getReservedScopesAsSet();
        // fail if the input scopes contains the reserved scopes.
        // retainAll removes all the objects that are not contained in the reserved scopes.
        if (!scopes.retainAll(reservedScopes)) {
            throw new IllegalArgumentException("Reserved scopes "
                    + OauthConstants.Oauth2Value.RESERVED_SCOPES.toString() + " cannot be provided as scopes.");
        }

        // client id can only be provided as a single scope.
        if (inputScopes.contains(mAuthRequestParameters.getClientId()) && inputScopes.size() != 1) {
            throw new IllegalArgumentException("Client id can only be provided as single scope.");
        }
    }

    /**
     * Perform the token request sent to token endpoint.
     * @throws MsalException If there is error happened in the request.
     */
    void performTokenRequest() throws MsalClientException, MsalServiceException {
        throwIfNetworkNotAvailable();

        final Oauth2Client oauth2Client = new Oauth2Client();
        buildRequestParameters(oauth2Client);

        final TokenResponse tokenResponse;
        try {
            tokenResponse = oauth2Client.getToken(mAuthRequestParameters.getAuthority());
        } catch (final IOException e) {
            Logger.error(TAG, mRequestContext, "Token request failed with error: "
                    + e.getMessage(), e);
            throw new MsalClientException(MSALError.IO_ERROR, "Auth failed with the error " + e.getMessage(), e);
        }

        mTokenResponse = tokenResponse;
    }

    /**
     * Silent flow will check if there is already an access token returned. If
     * so, return the stored token. Otherwise read the token response, and send Interaction_required back to calling app.
     * Silent flow will also remove token if receiving invalid_grant from token endpoint.
     * Interactive request will read the response, and send error back with code as oauth_error.
     * @throws MsalException
     */
    AuthenticationResult postTokenRequest() throws MsalUiRequiredException, MsalServiceException, MsalClientException {
        checkUserMismatch();

        final TokenCache tokenCache = mAuthRequestParameters.getTokenCache();
        final Authority authority = mAuthRequestParameters.getAuthority();
        authority.updateTenantLessAuthority(new IdToken(mTokenResponse.getRawIdToken()).getTenantId());
        final AccessTokenCacheItem accessTokenCacheItem = tokenCache.saveAccessToken(authority.getAuthority(),
                mAuthRequestParameters.getClientId(), mTokenResponse);
        tokenCache.saveRefreshToken(authority.getAuthorityHost(), mAuthRequestParameters.getClientId(),
                mTokenResponse);

        return new AuthenticationResult(accessTokenCacheItem);
    }

    /**
     * @return True if either access token or id token is returned, false otherwise.
     */
    boolean isAccessTokenReturned() {
        return !MSALUtils.isEmpty(mTokenResponse.getAccessToken()) || !MSALUtils.isEmpty(mTokenResponse.getRawIdToken());
    }

    /**
     * Throw DEVICE_CONNECTION_NOT_AVAILABLE if device network connection is not available.
     */
    void throwIfNetworkNotAvailable() throws MsalClientException {
        final ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            Logger.error(TAG, mRequestContext, "No active network is available on the device.", null);
            throw new MsalClientException(MSALError.DEVICE_NETWORK_NOT_AVAILABLE, "Device network connection is not available.");
        }
    }

    void throwExceptionFromTokenResponse(final TokenResponse tokenResponse) throws MsalUiRequiredException, MsalServiceException {
        if (MSALUtils.isEmpty(tokenResponse.getError())) {
            throw new MsalServiceException(MSALError.UNKNOWN_ERROR, "Request failed, but no error returned back from service.", tokenResponse.getHttpStatusCode(),
                    tokenResponse.getClaims(), null);
        }

        if (MSALError.INVALID_GRANT.equals(tokenResponse.getError())) {
            throw new MsalUiRequiredException(MSALError.INVALID_GRANT, tokenResponse.getErrorDescription(), tokenResponse.getClaims(), null);
        }

        throw new MsalServiceException(tokenResponse.getError(), tokenResponse.getErrorDescription(), tokenResponse.getHttpStatusCode(), tokenResponse.getClaims(), null);
    }

    private synchronized Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(mContext.getMainLooper());
        }

        return mHandler;
    }

    /**
     * Build request parameters, containing header, query parameters and request body.
     * @param oauth2Client
     */
    private void buildRequestParameters(final Oauth2Client oauth2Client) {
        oauth2Client.addHeader(OauthConstants.OauthHeader.CORRELATION_ID,
                mRequestContext.getCorrelationId().toString());

        // add body parameters
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.CLIENT_ID, mAuthRequestParameters.getClientId());
        final String scope = MSALUtils.convertSetToString(getDecoratedScope(mAuthRequestParameters.getScope()), " ");
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.SCOPE, scope);
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.CLIENT_INFO, "1");
        setAdditionalOauthParameters(oauth2Client);
    }

    private Set<String> getReservedScopesAsSet() {
        return new HashSet<>(Arrays.asList(OauthConstants.Oauth2Value.RESERVED_SCOPES));
    }

    private void checkUserMismatch() throws MsalClientException {
        final ClientInfo returnedClientInfo = new ClientInfo(mTokenResponse.getRawClientInfo());
        final String uniqueUserIdentifer = MSALUtils.getUniqueUserIdentifier(returnedClientInfo.getUniqueIdentifier(), returnedClientInfo.getUniqueTenantIdentifier());
        if (mAuthRequestParameters.getUser() != null && !mAuthRequestParameters.getUser().getUserIdentifier().equals(uniqueUserIdentifer)) {
            Logger.errorPII(TAG, mAuthRequestParameters.getRequestContext(), "User unique identifier provided in the request is: " + mAuthRequestParameters.getUser().getUserIdentifier()
                    + ". The user unique identifier returned from token endpoint is: " + uniqueUserIdentifer, null);
            throw new MsalClientException(MSALError.USER_MISMATCH, "User unique identifier provided in the request doesn't match the one returned in the token response");
        }
    }

    private void callbackOnSuccess(final AuthenticationCallback callback,
                                   final AuthenticationResult result) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(result);
            }
        });
    }

    private void callbackOnCancel(final AuthenticationCallback callback) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                callback.onCancel();
            }
        });
    }

    private void callbackOnError(final AuthenticationCallback callback,
                                 final MsalException msalException) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                callback.onError(msalException);
            }
        });
    }
}
