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
import android.util.Base64;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsPromptBehavior;
import com.microsoft.identity.common.internal.providers.oauth2.PkceChallenge;
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
    private static CountDownLatch sResultLock = new CountDownLatch(1);
    private final ActivityWrapper mActivityWrapper;

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
        mActivityWrapper = new ActivityWrapper(activity);

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
        Logger.verbose(TAG, mRequestContext, "Create the authorization request from request parameters.");
        sAuthorizationRequest = createAuthRequest();

        Logger.verbose(TAG, mRequestContext, "Create the intent to launch in AuthenticationActivity.");
        final Intent intentToLaunch = new Intent(mContext, AuthorizationActivity.class);
        try {
            intentToLaunch.putExtra(Constants.REQUEST_URL_KEY, sAuthorizationRequest.getAuthorizationStartUrl());
        } catch (final ClientException exception) {
            Logger.errorPII(TAG, mRequestContext, exception.getMessage(), exception);
            throw new MsalClientException(exception.getErrorCode(), exception.getMessage(), exception);
        } catch (final UnsupportedEncodingException exception) {
            Logger.errorPII(TAG, mRequestContext, exception.getMessage(), exception);
            throw new MsalClientException(ErrorStrings.UNSUPPORTED_ENCODING, exception.getMessage(), exception);
        }

        intentToLaunch.putExtra(Constants.REQUEST_ID, mRequestId);
        intentToLaunch.putExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE, sAuthorizationRequest);
        intentToLaunch.putExtra(Constants.WEBVIEW_SELECTION, getAuthRequestParameters().getWebViewSelection().getId());
        intentToLaunch.putExtra(
                Constants.TELEMETRY_REQUEST_ID,
                mAuthRequestParameters.getRequestContext().getTelemetryRequestId().toString()
        );

        if (!resolveIntent(intentToLaunch)) {
            throw new MsalClientException(MsalClientException.UNRESOLVABLE_INTENT, "The intent is not resolvable");
        }

        throwIfNetworkNotAvailable();

        mActivityWrapper.startActivityForResult(intentToLaunch, BROWSER_FLOW);
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
            if (requestCode != BROWSER_FLOW) {
                throw new IllegalStateException("Unknown request code");
            }

            // check it is the same request.
            sAuthorizationResult = AuthorizationResult.create(resultCode, data);
        } finally {
            sResultLock.countDown();
        }
    }

    private boolean resolveIntent(final Intent intent) {
        final ResolveInfo resolveInfo = mContext.getPackageManager().resolveActivity(intent, 0);
        return resolveInfo != null;
    }

    private Map<String, String> createAuthorizationRequestParameters() throws UnsupportedEncodingException, MsalClientException {
        final Map<String, String> requestParameters = new HashMap<>();

        final Set<String> scopes = new HashSet<>(mAuthRequestParameters.getScope());
        scopes.addAll(mExtraScopesToConsent);
        final Set<String> requestedScopes = getDecoratedScope(scopes);
        requestParameters.put(OauthConstants.Oauth2Parameters.SCOPE,
                MsalUtils.convertSetToString(requestedScopes, " "));
        requestParameters.put(OauthConstants.Oauth2Parameters.CLIENT_ID, mAuthRequestParameters.getClientId());
        requestParameters.put(OauthConstants.Oauth2Parameters.REDIRECT_URI, mAuthRequestParameters.getRedirectUri());
        requestParameters.put(OauthConstants.Oauth2Parameters.RESPONSE_TYPE, OauthConstants.Oauth2ResponseType.CODE);
        requestParameters.put(OauthConstants.OauthHeader.CORRELATION_ID,
                mAuthRequestParameters.getRequestContext().getCorrelationId().toString());
        requestParameters.putAll(PlatformIdHelper.getPlatformIdParameters());

        addExtraQueryParameter(OauthConstants.Oauth2Parameters.LOGIN_HINT, mAuthRequestParameters.getLoginHint(), requestParameters);
        addUiBehaviorToRequestParameters(requestParameters);

        // append state in the query parameters
        requestParameters.put(OauthConstants.Oauth2Parameters.STATE, encodeProtocolState());

        // Add PKCE Challenge
        addPKCEChallengeToRequestParameters(requestParameters);

        // Enforce session continuation if user is provided in the API request
        addSessionContinuationQps(requestParameters);

        // adding extra qp
        if (!MsalUtils.isEmpty(mAuthRequestParameters.getExtraQueryParam())) {
            appendExtraQueryParameters(mAuthRequestParameters.getExtraQueryParam(), requestParameters);
        }

        if (!MsalUtils.isEmpty(mAuthRequestParameters.getSliceParameters())) {
            appendExtraQueryParameters(mAuthRequestParameters.getSliceParameters(), requestParameters);
        }

        return requestParameters;
    }

    private void appendExtraQueryParameters(final String queryParams, final Map<String, String> requestParams) throws MsalClientException {
        final Map<String, String> extraQps = MsalUtils.decodeUrlToMap(queryParams, "&");
        final Set<Map.Entry<String, String>> extraQpEntries = extraQps.entrySet();
        for (final Map.Entry<String, String> extraQpEntry : extraQpEntries) {
            if (requestParams.containsKey(extraQpEntry.getKey())) {
                throw new MsalClientException(MsalClientException.DUPLICATE_QUERY_PARAMETER, "Extra query parameter " + extraQpEntry.getKey() + " is already sent by "
                        + "the SDK. ");
            }

            requestParams.put(extraQpEntry.getKey(), extraQpEntry.getValue());
        }
    }

    private void addSessionContinuationQps(final Map<String, String> requestParams) {
        final User user = mAuthRequestParameters.getUser();
        if (user != null) {
            addExtraQueryParameter(OauthConstants.Oauth2Parameters.LOGIN_REQ, user.getUid(), requestParams);
            addExtraQueryParameter(OauthConstants.Oauth2Parameters.DOMAIN_REQ, user.getUtid(), requestParams);
            addExtraQueryParameter(OauthConstants.Oauth2Parameters.LOGIN_HINT, user.getDisplayableId(), requestParams);
        }
    }

    private void addPKCEChallengeToRequestParameters(final Map<String, String> requestParameters) throws MsalClientException {
        try {
            if (sAuthorizationRequest.getPkceChallenge() == null) {
                sAuthorizationRequest.setPkceChallenge(PkceChallenge.newPkceChallenge());
            }

            // Add it to our Authorization request
            requestParameters.put(OauthConstants.Oauth2Parameters.CODE_CHALLENGE, sAuthorizationRequest.getPkceChallenge().getCodeChallenge());
            requestParameters.put(OauthConstants.Oauth2Parameters.CODE_CHALLENGE_METHOD, sAuthorizationRequest.getPkceChallenge().getCodeChallengeMethod());
        } catch (final ClientException exception) {
            Logger.errorPII(TAG, mRequestContext, exception.getMessage(), exception);
            throw new MsalClientException(exception.getErrorCode(), exception.getMessage(), exception);
        }
    }

    private void addUiBehaviorToRequestParameters(final Map<String, String> requestParameters) {
        final UiBehavior uiBehavior = mAuthRequestParameters.getUiBehavior();
        if (uiBehavior == UiBehavior.FORCE_LOGIN) {
            requestParameters.put(OauthConstants.Oauth2Parameters.PROMPT, OauthConstants.PromptValue.LOGIN);
        } else if (uiBehavior == UiBehavior.SELECT_ACCOUNT) {
            requestParameters.put(OauthConstants.Oauth2Parameters.PROMPT, OauthConstants.PromptValue.SELECT_ACCOUNT);
        } else if (uiBehavior == UiBehavior.CONSENT) {
            requestParameters.put(OauthConstants.Oauth2Parameters.PROMPT, OauthConstants.PromptValue.CONSENT);
        }
    }

    private String encodeProtocolState() throws UnsupportedEncodingException {
        final String state = String.format("a=%s&r=%s", MsalUtils.urlFormEncode(
                mAuthRequestParameters.getAuthority().getAuthority()),
                MsalUtils.urlFormEncode(MsalUtils.convertSetToString(
                        mAuthRequestParameters.getScope(), " ")));
        return Base64.encodeToString(state.getBytes("UTF-8"), Base64.NO_PADDING | Base64.URL_SAFE);
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

    private void addExtraQueryParameter(final String key, final String value, final Map<String, String> requestParams) {
        if (!MsalUtils.isEmpty(key) && !MsalUtils.isEmpty(value)) {
            requestParams.put(key, value);
        }
    }

    /**
     * Internal static class to create a weak reference of the passed-in activity. The library itself doesn't control the
     * passed-in activity's lifecycle.
     */
    static class ActivityWrapper {
        private WeakReference<Activity> mReferencedActivity;

        ActivityWrapper(final Activity activity) {
            mReferencedActivity = new WeakReference<Activity>(activity);
        }

        void startActivityForResult(final Intent intent, int requestCode) throws MsalClientException {
            if (mReferencedActivity.get() == null) {
                throw new MsalClientException(MsalClientException.UNRESOLVABLE_INTENT, "The referenced object is already being garbage collected.");
            }

            mReferencedActivity.get().startActivityForResult(intent, requestCode);
        }
    }
}
