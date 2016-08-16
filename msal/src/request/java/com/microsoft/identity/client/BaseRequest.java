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

import org.json.JSONException;

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

    BaseRequest(final Context appContext, final AuthenticationRequestParameters authenticationRequestParameters) {
        mContext = appContext;
        mAuthRequestParameters = authenticationRequestParameters;

        if (authenticationRequestParameters.getScope() == null
                || authenticationRequestParameters.getScope().isEmpty()) {
            throw new IllegalArgumentException("scope is empty or null");
        }

        validateInputScopes(authenticationRequestParameters.getScope());

        mLoadFromCache = authenticationRequestParameters.getTokenCache() != null;
        mStoreIntoCache = mLoadFromCache;
    }


    abstract void preTokenRequest() throws MSALUserCancelException, AuthenticationException;

    abstract void setAdditionalRequestBody(final Oauth2Client oauth2Client);

    void getToken(final AuthenticationCallback callback) {
        final CallbackHandler callbackHandler = new CallbackHandler(getHandler(), callback);
        mRequestId = callback.hashCode();
        THREAD_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    preTokenRequest();
                    final AuthenticationResult authenticationResult = sentTokenRequest();
                    storeTokenIntoCache(authenticationResult);
                    callbackHandler.onSuccess(authenticationResult);
                } catch (final MSALUserCancelException userCancelException) {
                    callbackHandler.onCancel();
                } catch (final AuthenticationException authenticationException) {
                    callbackHandler.onError(authenticationException);
                }
            }
        });
        // getToken is doing:
        // 1. preTokenRequest. 1) for interactive request, preToken is launching the web ui. The end result should be
        // auth code or error result 2) for silent request, preToken is doing cache lookup. If there is a valid AT
        // returned, we should return the AT. If there is a RT returned, we should use it to token acquisition.
        // 2. performTokenRequest. Use either auth code or RT found in the preTokenRequest to get a new token,.
        // 3. Post token request, store the returned token into cache.
    }

    AuthenticationResult sentTokenRequest() throws AuthenticationException {
        final Oauth2Client oauth2Client = new Oauth2Client();

        oauth2Client.addHeader(OauthConstants.OauthHeader.CORRELATION_ID,
                mAuthRequestParameters.getCorrelationId().toString());

        // add body parameters
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.CLIENT_ID, mAuthRequestParameters.getClientId());
        final String scope = MSALUtils.convertSetToString(getDecoratedScope(mAuthRequestParameters.getScope()), " ");
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.SCOPE, scope);
        setAdditionalRequestBody(oauth2Client);

        final TokenResponse tokenResponse;
        try {
            tokenResponse = oauth2Client.getToken(mAuthRequestParameters.getAuthority().getAuthorityUrl());
        } catch (final RetryableException retryableException) {
            if (mLoadFromCache) {
                // TODO: Resilency feature for silent flow. we need to check if
                return null;
            } else {
                throw new AuthenticationException(MSALError.SERVER_ERROR, retryableException.getMessage(),
                        retryableException.getCause());
            }
        } catch (final IOException | JSONException e) {
            throw new AuthenticationException(MSALError.AUTH_FAILED, "Auth failed with the error " + e.getMessage(), e);
        }

        if (MSALUtils.isEmpty(tokenResponse.getAccessToken())) {
            throw new AuthenticationException(MSALError.OAUTH_ERROR, "ErrorCode: " + tokenResponse.getError()
                    + "; ErrorDescription: " + tokenResponse.getErrorDescription());
        }

        return new AuthenticationResult(tokenResponse);
    }

    void storeTokenIntoCache(final AuthenticationResult authenticationResult) {
        // TODO: do something.
    }

    private synchronized Handler getHandler() {
        if (mHandler == null) {
            return new Handler(mContext.getMainLooper());
        }

        return mHandler;
    }

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

    private Set<String> getReservedScopesAsSet() {
        return new HashSet<>(Arrays.asList(OauthConstants.Oauth2Value.RESERVED_SCOPES));
    }

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

    private static class CallbackHandler {
        private final Handler mReferenceHandler;
        private final AuthenticationCallback mCallback;

        public CallbackHandler(final Handler referenceHandler,
                               final AuthenticationCallback callback) {
            if (callback == null) {
                throw new IllegalArgumentException("callback is null");
            }

            mReferenceHandler = referenceHandler;
            mCallback = callback;
        }

        public void onSuccess(final AuthenticationResult result) {
            if (mReferenceHandler != null) {
                mReferenceHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onSuccess(result);
                    }
                });
            } else {
                mCallback.onSuccess(result);
            }
        }

        public void onError(final AuthenticationException exception) {
            if (mReferenceHandler != null) {
                mReferenceHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onError(exception);
                    }
                });
            } else {
                mCallback.onError(exception);
            }
        }

        public void onCancel() {
            if (mReferenceHandler != null) {
                mReferenceHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onCancel();
                    }
                });
            } else {
                mCallback.onCancel();
            }
        }
    }
}
