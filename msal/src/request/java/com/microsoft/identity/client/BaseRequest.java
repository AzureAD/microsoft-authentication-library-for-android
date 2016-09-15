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
    private static final ExecutorService THREAD_EXECUTOR = Executors.newSingleThreadExecutor();

    protected final AuthenticationRequestParameters mAuthRequestParameters;

    protected boolean mLoadFromCache;
    protected boolean mStoreIntoCache;

    private Handler mHandler;
    protected final Context mContext;
    protected int mRequestId;

    /**
     * Abstract method, implemented by subclass for its own logic before the token request.
     * @throws MSALUserCancelException If pre token request fails as user cancels the flow.
     * @throws AuthenticationException If error happens during the pre-process.
     */
    abstract void preTokenRequest() throws MSALUserCancelException, AuthenticationException;

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
                    mAuthRequestParameters.getAuthority().resolveEndpoints(mAuthRequestParameters.getCorrelationId());
                    preTokenRequest();
                    final AuthenticationResult authenticationResult = performTokenRequest();
                    storeTokenIntoCache(authenticationResult);
                    callbackOnSuccess(callback, authenticationResult);
                } catch (final MSALUserCancelException userCancelException) {
                    callbackOnCancel(callback);
                } catch (final AuthenticationException authenticationException) {
                    callbackOnError(callback, authenticationException);
                }
            }
        });
    }

    /**
     * Get the decorated scopes. Will combine the input scope and the reserved scope. If client id is provided as scope,
     * it will be removed from the combined scopes. If policy is provided, email and profile will be removed.
     * @param inputScopes The input scopes to decorate.
     * @return The combined scopes.
     */
    Set<String> getDecoratedScope(final Set<String> inputScopes) {
        final Set<String> scopes = new HashSet<>(inputScopes);
        final Set<String> reservedScopes = getReservedScopesAsSet();
        scopes.addAll(reservedScopes);
        scopes.remove(mAuthRequestParameters.getClientId());

        // For B2C scenario, policy will be provided. We don't send email and profile as scopes.
        if (!MSALUtils.isEmpty(mAuthRequestParameters.getPolicy())) {
            scopes.remove(OauthConstants.Oauth2Value.SCOPE_EMAIL);
            scopes.remove(OauthConstants.Oauth2Value.SCOPE_PROFILE);
        }

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

    private AuthenticationResult performTokenRequest() throws AuthenticationException {
        final Oauth2Client oauth2Client = new Oauth2Client();
        buildRequestParameters(oauth2Client);

        final TokenResponse tokenResponse;
        try {
            tokenResponse = oauth2Client.getToken(mAuthRequestParameters.getAuthority());
        } catch (final RetryableException retryableException) {
            if (mLoadFromCache) {
                // TODO: Resiliency feature for silent flow. we need to check if extended_expires_on
                // feature is turned on
                return null;
            } else {
                throw new AuthenticationException(MSALError.SERVER_ERROR, retryableException.getMessage(),
                        retryableException.getCause());
            }
        } catch (final IOException e) {
            throw new AuthenticationException(MSALError.AUTH_FAILED, "Auth failed with the error " + e.getMessage(), e);
        }

        // If client id is the only scope, id token will be returned instead of access token.
        if (MSALUtils.isEmpty(tokenResponse.getAccessToken())
                && MSALUtils.isEmpty(tokenResponse.getRawIdToken())) {
            throw new AuthenticationException(MSALError.OAUTH_ERROR, "ErrorCode: " + tokenResponse.getError()
                    + "; ErrorDescription: " + tokenResponse.getErrorDescription());
        }

        return new AuthenticationResult(tokenResponse);
    }

    private void storeTokenIntoCache(final AuthenticationResult authenticationResult) {
        // TODO: do something.
    }

    private synchronized Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(mContext.getMainLooper());
        }

        return mHandler;
    }

    private void buildRequestParameters(final Oauth2Client oauth2Client) {
        oauth2Client.addHeader(OauthConstants.OauthHeader.CORRELATION_ID,
                mAuthRequestParameters.getCorrelationId().toString());

        // add query parameter, policy is added as qp
        if (!MSALUtils.isEmpty(mAuthRequestParameters.getPolicy())) {
            oauth2Client.addQueryParameter(OauthConstants.Oauth2Parameters.POLICY, mAuthRequestParameters.getPolicy());
        }

        // add body parameters
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.CLIENT_ID, mAuthRequestParameters.getClientId());
        final String scope = MSALUtils.convertSetToString(getDecoratedScope(mAuthRequestParameters.getScope()), " ");
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.SCOPE, scope);
        setAdditionalOauthParameters(oauth2Client);
    }

    private Set<String> getReservedScopesAsSet() {
        return new HashSet<>(Arrays.asList(OauthConstants.Oauth2Value.RESERVED_SCOPES));
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
                                 final AuthenticationException authenticationException) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                callback.onError(authenticationException);
            }
        });
    }
}
