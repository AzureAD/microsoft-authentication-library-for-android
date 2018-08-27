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
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Base64;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsPromptBehavior;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.PkceChallenge;
import com.microsoft.identity.common.internal.ui.AuthorizationStrategyFactory;
import com.microsoft.identity.common.internal.ui.webview.AzureActiveDirectoryWebViewClient;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Request handling the interactive flow. Interactive flow skips the cache look, will launch the web UI(either custom
 * tab or fall back to webview if custom tab is not available).
 */
final class InteractiveRequest extends BaseRequest {
    private static final String TAG = InteractiveRequest.class.getSimpleName();
    private final Set<String> mExtraScopesToConsent = new HashSet<>();

    static final int BROWSER_FLOW = 1001;
    private static AuthorizationResult sAuthorizationResult;
    private static MicrosoftStsAuthorizationRequest sAuthorizationRequest;
    private static AuthorizationStrategy sAuthorizationStrategy;
    private static CountDownLatch sResultLock = new CountDownLatch(1);
    private WeakReference<Activity> mActivityRef;

    /**
     * Constructor for {@link InteractiveRequest}.
     *
     * @param activity              {@link Activity} used to launch the {@link AuthenticationActivity}.
     * @param authRequestParameters {@link AuthenticationRequestParameters} that is holding all the parameters for oauth request.
     * @param extraScopesToConsent  An array of extra scopes.
     */
    InteractiveRequest(final Activity activity, final AuthenticationRequestParameters authRequestParameters,
                       final String[] extraScopesToConsent) {
        super(activity.getApplicationContext(), authRequestParameters);
        mActivityRef = new WeakReference<>(activity);

        // validate redirect
        if (MsalUtils.isEmpty(authRequestParameters.getRedirectUri())) {
            throw new IllegalArgumentException("redirect is empty");
        } // TODO: We need to validate redirect is as expected to make custom tab work.

        // validate extra scope
        if (extraScopesToConsent != null && extraScopesToConsent.length > 0) {
            final Set<String> extraScopesToConsentSet = new HashSet<>(Arrays.asList(extraScopesToConsent));
            validateInputScopes(extraScopesToConsentSet);

            mExtraScopesToConsent.addAll(extraScopesToConsentSet);
        }
    }

    /**
     * Pre token request. Launch either chrome custom tab or chrome to get the auth code back.
     */
    @Override
    synchronized void preTokenRequest() throws MsalUserCancelException, MsalClientException, MsalServiceException,
            MsalUiRequiredException {
        super.preTokenRequest();
        throwIfNetworkNotAvailable();
        Logger.verbose(TAG, mRequestContext, "Create the authorization request from request parameters.");
        sAuthorizationRequest = createAuthRequest();
        AuthorizationConfiguration.getInstance().setRedirectUrl(sAuthorizationRequest.getRedirectUri());
        sAuthorizationStrategy = AuthorizationStrategyFactory.getInstance().getAuthorizationStrategy(mActivityRef.get(), AuthorizationConfiguration.getInstance());
        try {
            sAuthorizationStrategy.requestAuthorization(Uri.parse(sAuthorizationRequest.getAuthorizationStartUrl()));
        } catch (final ClientException | UnsupportedEncodingException exc) {
            throw new MsalClientException("requestAuthorization cancelled.", exc.getMessage(), exc);
        }

        // lock the thread until onActivityResult release the lock.
        try {
            if (sResultLock.getCount() == 0) {
                sResultLock = new CountDownLatch(1);
            }

            sResultLock.await();
        } catch (final InterruptedException e) {
            Logger.error(TAG, mAuthRequestParameters.getRequestContext(), "Fail to lock the thread for waiting for authorize"
                    + " request to return.", e);
        }

        processAuthorizationResult(sAuthorizationResult);
    }

    /**
     * Create the {@Link MicrosoftStsAuthorizationRequest} from authentication parameters.
     *
     * @return MicrosoftStsAuthorizationRequest
     * @throws MsalClientException
     */
    private MicrosoftStsAuthorizationRequest createAuthRequest() throws MsalClientException {
        MicrosoftStsPromptBehavior promptBehavior;

        //Map the UIBehavior from MSAL to Common
        switch (getAuthRequestParameters().getUiBehavior()) {
            case CONSENT:
                promptBehavior = MicrosoftStsPromptBehavior.CONSENT;
                break;
            case FORCE_LOGIN:
                promptBehavior = MicrosoftStsPromptBehavior.FORCE_LOGIN;
                break;
            default:
                promptBehavior = MicrosoftStsPromptBehavior.SELECT_ACCOUNT;
                break;
        }

        /*

        (final String responseType,
                                            @NonNull final String clientId,
                                            @NonNull final String redirectUri,
                                            final String state,
                                            @NonNull final String scope,
                                            @NonNull final URL authority,
                                            final String loginHint,
                                            final UUID correlationId,
                                            final PkceChallenge pkceChallenge,
                                            final String extraQueryParam,
                                            final String libraryVersion,
                                            @NonNull final MicrosoftStsPromptBehavior promptBehavior,
                                            final String uid,
                                            final String utid,
                                            final String displayableId,
                                            final String sliceParameters
                                            )
         */

        final MicrosoftStsAuthorizationRequest authorizationRequest = new MicrosoftStsAuthorizationRequest(
                OauthConstants.Oauth2Parameters.CODE,
                getAuthRequestParameters().getClientId(),
                getAuthRequestParameters().getRedirectUri(),
                null,
                StringUtil.join(' ', new ArrayList<String>(getAuthRequestParameters().getScope())),
                getAuthRequestParameters().getAuthority().getAuthorityUrl(),
                getAuthRequestParameters().getLoginHint(),
                getAuthRequestParameters().getRequestContext().getCorrelationId(),
                null,
                getAuthRequestParameters().getExtraQueryParam(),
                PublicClientApplication.getSdkVersion(),
                promptBehavior,
                null,
                null,
                null,
                getAuthRequestParameters().getSliceParameters());

        if (null != getAuthRequestParameters().getUser()) {
            authorizationRequest.setUid(getAuthRequestParameters().getUser().getUid());
            authorizationRequest.setUtid(getAuthRequestParameters().getUser().getUtid());
            authorizationRequest.setDisplayableId(getAuthRequestParameters().getUser().getDisplayableId());
        }

        //Set encoded state for the request
        try {
            authorizationRequest.setState(authorizationRequest.generateEncodedState());
        } catch (final UnsupportedEncodingException exception) {
            Logger.errorPII(TAG, mRequestContext, exception.getMessage(), exception);
            throw new MsalClientException(ErrorStrings.UNSUPPORTED_ENCODING, exception.getMessage(), exception);
        }

        //Set pcke challenge
        try {
            authorizationRequest.setPkceChallenge(PkceChallenge.newPkceChallenge());
        } catch (final ClientException exception) {
            Logger.errorPII(TAG, mRequestContext, exception.getMessage(), exception);
            throw new MsalClientException(exception.getErrorCode(), exception.getMessage(), exception);
        }

        return authorizationRequest;
    }

    @Override
    void setAdditionalOauthParameters(final Oauth2Client oauth2Client) {
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.GRANT_TYPE,
                OauthConstants.Oauth2GrantType.AUTHORIZATION_CODE);
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.CODE, sAuthorizationResult.getAuthCode());
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.REDIRECT_URI,
                mAuthRequestParameters.getRedirectUri());
        // Adding code verifier per PKCE spec. See https://tools.ietf.org/html/rfc7636
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.CODE_VERIFIER, sAuthorizationRequest.getPkceChallenge().getCodeVerifier());
    }

    @Override
    AuthenticationResult postTokenRequest() throws MsalUiRequiredException, MsalServiceException, MsalClientException {
        if (!isAccessTokenReturned()) {
            throwExceptionFromTokenResponse(mTokenResponse);
        }

        return super.postTokenRequest();
    }

    static synchronized void onActivityResult(int requestCode, int resultCode, final Intent data) {
        Logger.info(TAG, null, "Received request code is: " + requestCode + "; result code is: " + resultCode);
        try {
            if (requestCode != AuthorizationStrategy.BROWSER_FLOW) {
                throw new IllegalStateException("Unknown request code");
            }

            // check it is the same request.
            sAuthorizationStrategy.completeAuthorization(requestCode, resultCode, data);
            sAuthorizationResult = AuthorizationResult.create(resultCode, data);
        } finally {
            sResultLock.countDown();
        }
    }

    private void processAuthorizationResult(final AuthorizationResult authorizationResult) throws MsalUserCancelException,
            MsalServiceException, MsalClientException {
        if (authorizationResult == null) {
            Logger.error(TAG, mAuthRequestParameters.getRequestContext(), "Authorization result is null", null);
            throw new MsalClientException(MsalServiceException.UNKNOWN_ERROR, "Receives empty result for authorize request");
        }

        final AuthorizationResult.AuthorizationStatus status = authorizationResult.getAuthorizationStatus();
        Logger.info(TAG, mAuthRequestParameters.getRequestContext(), "Authorize request status is: " + status.toString());
        switch (status) {
            case USER_CANCEL:
                throw new MsalUserCancelException();
            case FAIL:
                // TODO: if clicking on the cancel button in the signin page, we get sub_error with the returned url,
                // however we cannot take dependency on the sub_error. Then how do we know user click on the cancel button
                // and that's actually a cancel request? Is server going to return some error code that we can use?
                throw new MsalServiceException(authorizationResult.getError(), authorizationResult.getError() + ";"
                        + authorizationResult.getErrorDescription(), MsalServiceException.DEFAULT_STATUS_CODE, null);
            case SUCCESS:
                // verify if the state is the same as the one we send
                verifyStateInResponse(authorizationResult.getState());
                // Happy path, continue the process to use code for new access token.
                return;
            default:
                throw new IllegalStateException("Unknown status code");
        }
    }

    private void verifyStateInResponse(final String stateInResponse) throws MsalClientException {
        final String decodeState = sAuthorizationRequest.decodeState(stateInResponse);

        if (decodeState == null || decodeState.equals(sAuthorizationRequest.getState())) {
            throw new MsalClientException(MsalClientException.STATE_MISMATCH, Constants.MsalErrorMessage.STATE_NOT_THE_SAME);
        }
    }
}
