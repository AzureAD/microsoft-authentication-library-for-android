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
package com.microsoft.identity.client.internal.controllers;

import android.content.Intent;
import android.text.TextUtils;

import com.microsoft.identity.client.BrowserTabActivity;
import com.microsoft.identity.client.exception.MsalUiRequiredException;
import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.UiRequiredException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.LocalAuthenticationResult;
import com.microsoft.identity.common.internal.ui.AuthorizationStrategyFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.microsoft.identity.common.adal.internal.net.HttpWebRequest.throwIfNetworkNotAvailable;

public class LocalMSALController extends BaseController {

    private static final String TAG = LocalMSALController.class.getSimpleName();

    private AuthorizationStrategy mAuthorizationStrategy = null;
    private AuthorizationRequest mAuthorizationRequest = null;

    @Override
    public AcquireTokenResult acquireToken(final AcquireTokenOperationParameters parameters)
            throws ExecutionException, InterruptedException, ClientException, IOException, ArgumentException {
        final String methodName = ":acquireToken";
        Logger.verbose(
                TAG + methodName,
                "Acquiring token..."
        );
        final AcquireTokenResult acquireTokenResult = new AcquireTokenResult();

        //00) Validate MSAL Parameters
        parameters.validate();

        // Add default scopes
        addDefaultScopes(parameters);

        //0) Get known authority result
        throwIfNetworkNotAvailable(parameters.getAppContext());
        Authority.KnownAuthorityResult authorityResult = Authority.getKnownAuthorityResult(parameters.getAuthority());

        //0.1 If not known throw resulting exception
        if (!authorityResult.getKnown()) {
            throw authorityResult.getClientException();
        }

        //1) Get oAuth2Strategy for Authority Type
        final OAuth2Strategy oAuth2Strategy = parameters.getAuthority().createOAuth2Strategy();

        //2) Request authorization interactively
        final AuthorizationResult result = performAuthorizationRequest(oAuth2Strategy, parameters);
        acquireTokenResult.setAuthorizationResult(result);

        if (result.getAuthorizationStatus().equals(AuthorizationStatus.SUCCESS)) {
            //3) Exchange authorization code for token
            final TokenResult tokenResult = performTokenRequest(
                    oAuth2Strategy,
                    mAuthorizationRequest,
                    result.getAuthorizationResponse(),
                    parameters
            );

            acquireTokenResult.setTokenResult(tokenResult);

            if (tokenResult != null && tokenResult.getSuccess()) {
                //4) Save tokens in token cache
                final ICacheRecord cacheRecord = saveTokens(
                        oAuth2Strategy,
                        mAuthorizationRequest,
                        tokenResult.getTokenResponse(),
                        parameters.getTokenCache()
                );

                acquireTokenResult.setLocalAuthenticationResult(
                        new LocalAuthenticationResult(cacheRecord)
                );
            }
        }
        return acquireTokenResult;
    }

    private AuthorizationResult performAuthorizationRequest(final OAuth2Strategy strategy,
                                                            final AcquireTokenOperationParameters parameters)
            throws ExecutionException, InterruptedException, ClientException {
        throwIfNetworkNotAvailable(parameters.getAppContext());
        //Create pendingIntent to handle the authorization result intent back to the calling activity
        final Intent resultIntent = new Intent(parameters.getActivity(), BrowserTabActivity.class);
        mAuthorizationStrategy = AuthorizationStrategyFactory
                .getInstance()
                .getAuthorizationStrategy(
                        parameters.getActivity(),
                        parameters.getAuthorizationAgent(),
                        resultIntent
                );
        mAuthorizationRequest = getAuthorizationRequest(strategy, parameters);

        Future<AuthorizationResult> future = strategy.requestAuthorization(
                mAuthorizationRequest,
                mAuthorizationStrategy
        );

        //We could implement Timeout Here if we wish instead of blocking indefinitely
        //future.get(10, TimeUnit.MINUTES);  // Need to handle timeout exception in the scenario it doesn't return within a reasonable amount of time
        AuthorizationResult result = future.get();

        return result;
    }

    @Override
    public void completeAcquireToken(final int requestCode,
                                     final int resultCode,
                                     final Intent data) {
        final String methodName = ":completeAcquireToken";
        Logger.verbose(
                TAG + methodName,
                "Completing acquire token..."
        );
        mAuthorizationStrategy.completeAuthorization(requestCode, resultCode, data);
    }

    @Override
    public AcquireTokenResult acquireTokenSilent(
            final AcquireTokenSilentOperationParameters parameters)
            throws IOException, ClientException, ArgumentException {
        final String methodName = ":acquireTokenSilent";
        Logger.verbose(
                TAG + methodName,
                "Acquiring token silently..."
        );

        final AcquireTokenResult acquireTokenSilentResult = new AcquireTokenResult();

        //Validate MSAL Parameters
        parameters.validate();

        // Add default scopes
        addDefaultScopes(parameters);

        final OAuth2TokenCache tokenCache = parameters.getTokenCache();

        final AccountRecord targetAccount = getCachedAccountRecord(parameters);

        final OAuth2Strategy strategy = parameters.getAuthority().createOAuth2Strategy();
        final ICacheRecord cacheRecord = tokenCache.load(
                parameters.getClientId(),
                TextUtils.join(" ", parameters.getScopes()),
                targetAccount
        );

        if (accessTokenIsNull(cacheRecord)
                || refreshTokenIsNull(cacheRecord)
                || parameters.getForceRefresh()) {
            if (!refreshTokenIsNull(cacheRecord)) {
                // No AT found, but the RT checks out, so we'll use it
                Logger.verbose(
                        TAG + methodName,
                        "No access token found, but RT is available."
                );
                renewAccessToken(
                        parameters,
                        acquireTokenSilentResult,
                        tokenCache,
                        strategy,
                        cacheRecord
                );
            } else {
                throw new ClientException(MsalUiRequiredException.NO_TOKENS_FOUND, "No refresh token was found. ");
            }
        } else if (cacheRecord.getAccessToken().isExpired()) {
            Logger.warn(
                    TAG + methodName,
                    "Access token is expired. Removing from cache..."
            );
            // Remove the expired token
            tokenCache.removeCredential(cacheRecord.getAccessToken());

            Logger.verbose(
                    TAG + methodName,
                    "Renewing access token..."
            );
            // Request a new AT
            renewAccessToken(
                    parameters,
                    acquireTokenSilentResult,
                    tokenCache,
                    strategy,
                    cacheRecord
            );
        } else {
            Logger.verbose(
                    TAG + methodName,
                    "Returning silent result"
            );
            // the result checks out, return that....
            acquireTokenSilentResult.setLocalAuthenticationResult(
                    new LocalAuthenticationResult(cacheRecord)
            );
        }

        return acquireTokenSilentResult;
    }
}
