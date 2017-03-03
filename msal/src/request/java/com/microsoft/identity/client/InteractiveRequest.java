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

import java.io.UnsupportedEncodingException;
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
    private final Set<String> mAdditionalScope = new HashSet<>();

    static final String DISABLE_CHROMETAB = "disablechrometab"; // TODO: remove it
    static final int BROWSER_FLOW = 1001;
    private static AuthorizationResult sAuthorizationResult;
    private static CountDownLatch sResultLock = new CountDownLatch(1);

    private final Activity mActivity;

    /**
     * Constructor for {@link InteractiveRequest}.
     * @param activity {@link Activity} used to launch the {@link AuthenticationActivity}.
     * @param authRequestParameters {@link AuthenticationRequestParameters} that is holding all the parameters for oauth request.
     * @param additionalScope An array of additional scopes.
     */
    InteractiveRequest(final Activity activity, final AuthenticationRequestParameters authRequestParameters,
                       final String[] additionalScope) {
        super(activity.getApplicationContext(), authRequestParameters);

        mActivity = activity;

        // validate redirect
        if (MSALUtils.isEmpty(authRequestParameters.getRedirectUri())) {
            throw new IllegalArgumentException("redirect is empty");
        } // TODO: We need to validate redirect is as expected to make custom tab work.

        // validate additional scope
        if (additionalScope != null && additionalScope.length > 0) {
            final Set<String> additionalScopeSet = new HashSet<>(Arrays.asList(additionalScope));
            validateInputScopes(additionalScopeSet);

            mAdditionalScope.addAll(additionalScopeSet);
        }

        // verify the UI option. If UI option is set as as_as_current_user, login hint has to be provided.
        if (MSALUtils.isEmpty(authRequestParameters.getLoginHint())
                && authRequestParameters.getUIOption() == UIOptions.ACT_AS_CURRENT_USER) {
            throw new IllegalArgumentException(
                    "loginhint has to be provided if setting UI option as ACT_AS_CURRENT_USER");
        }
    }

    /**
     * Pre token request. Launch either chrome custom tab or chrome to get the auth code back.
     */
    @Override
    synchronized void preTokenRequest() throws MSALUserCancelException, AuthenticationException {
        final String authorizeUri;
        try {
            authorizeUri = appendQueryStringToAuthorizeEndpoint();
        } catch (final UnsupportedEncodingException e) {
            throw new AuthenticationException(MSALError.UNSUPPORTED_ENCODING, e.getMessage(), e);
        }

        final Intent intentToLaunch = new Intent(mContext, AuthenticationActivity.class);
        intentToLaunch.putExtra(Constants.REQUEST_URL_KEY, authorizeUri);
        intentToLaunch.putExtra(Constants.REQUEST_ID, mRequestId);

        // TODO: put a request id.
        if (!resolveIntent(intentToLaunch)) {
            // TODO: what is the exception to throw
            throw new AuthenticationException();
        }

        throwIfNetworkNotAvailable();

        mActivity.startActivityForResult(intentToLaunch, BROWSER_FLOW);
        // lock the thread until onActivityResult release the lock.
        try {
            if (sResultLock.getCount() == 0) {
                sResultLock = new CountDownLatch(1);
            }

            sResultLock.await();
            //CHECKSTYLE:OFF: checkstyle:EmptyBlock
        } catch (final InterruptedException e) {
            // TODO: logging.
        }

        processAuthorizationResult(sAuthorizationResult);
    }

    @Override
    void setAdditionalOauthParameters(final Oauth2Client oauth2Client) {
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.GRANT_TYPE,
                OauthConstants.Oauth2GrantType.AUTHORIZATION_CODE);
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.CODE, sAuthorizationResult.getAuthCode());
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.REDIRECT_URI,
                mAuthRequestParameters.getRedirectUri());
    }

    @Override
    AuthenticationResult postTokenRequest() throws AuthenticationException {
        if (!isAccessTokenReturned()) {
            throw new AuthenticationException(MSALError.OAUTH_ERROR, ""
                    + "ErrorCode: " + mTokenResponse.getError() + "; ErrorDescription: " + mTokenResponse.getErrorDescription());
        }

        return super.postTokenRequest();
    }

    static synchronized void onActivityResult(int requestCode, int resultCode, final Intent data) {
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

    String appendQueryStringToAuthorizeEndpoint() throws UnsupportedEncodingException {
        String authorizationUrl = MSALUtils.appendQueryParameterToUrl(
                mAuthRequestParameters.getAuthority().getAuthorizeEndpoint(),
                createRequestParameters());

        final String extraQP = mAuthRequestParameters.getExtraQueryParam();
        if (!MSALUtils.isEmpty(extraQP)) {
            String parsedQP = extraQP;
            if (!extraQP.startsWith("&")) {
                parsedQP = "&" + parsedQP;
            }

            authorizationUrl += parsedQP;
        }

        return authorizationUrl;
    }

    private boolean resolveIntent(final Intent intent) {
        final ResolveInfo resolveInfo = mContext.getPackageManager().resolveActivity(intent, 0);
        return resolveInfo != null;
    }

    private Map<String, String> createRequestParameters() throws UnsupportedEncodingException {
        final Map<String, String> requestParameters = new HashMap<>();

        final Set<String> scopes = new HashSet<>(mAuthRequestParameters.getScope());
        scopes.addAll(mAdditionalScope);
        final Set<String> requestedScopes = getDecoratedScope(scopes);
        requestParameters.put(OauthConstants.Oauth2Parameters.SCOPE,
                MSALUtils.convertSetToString(requestedScopes, " "));
        requestParameters.put(OauthConstants.Oauth2Parameters.CLIENT_ID, mAuthRequestParameters.getClientId());
        requestParameters.put(OauthConstants.Oauth2Parameters.REDIRECT_URI, mAuthRequestParameters.getRedirectUri());
        requestParameters.put(OauthConstants.Oauth2Parameters.RESPONSE_TYPE, OauthConstants.Oauth2ResponseType.CODE);
        requestParameters.put(OauthConstants.OauthHeader.CORRELATION_ID,
                mAuthRequestParameters.getRequestContext().getCorrelationId().toString());
        requestParameters.putAll(PlatformIdHelper.getPlatformIdParameters());

        if (!MSALUtils.isEmpty(mAuthRequestParameters.getPolicy())) {
            requestParameters.put(OauthConstants.Oauth2Parameters.POLICY, mAuthRequestParameters.getPolicy());
        }

        if (!MSALUtils.isEmpty(mAuthRequestParameters.getLoginHint())) {
            requestParameters.put(OauthConstants.Oauth2Parameters.LOGIN_HINT, mAuthRequestParameters.getLoginHint());
        }

        // TODO: comment out the code for adding haschrome=1. Evo displays the Cancel button, and the returned url would
        // contain the error=access_denied&error_subcode=cancel
        // add hasChrome
//        if (MSALUtils.isEmpty(mAuthRequestParameters.getExtraQueryParam())
//                || mAuthRequestParameters.getExtraQueryParam().contains(OauthConstants.Oauth2Parameters.HAS_CHROME)) {
//            requestParameters.put(OauthConstants.Oauth2Parameters.HAS_CHROME, "1");
//        }

        addUiOptionToRequestParameters(requestParameters);

        // append state in the query parameters
        requestParameters.put(OauthConstants.Oauth2Parameters.STATE, encodeProtocolState());

        return requestParameters;
    }

    private void addUiOptionToRequestParameters(final Map<String, String> requestParameters) {
        final UIOptions uiOptions = mAuthRequestParameters.getUIOption();
        if (uiOptions == UIOptions.FORCE_LOGIN) {
            requestParameters.put(OauthConstants.Oauth2Parameters.PROMPT, OauthConstants.PromptValue.LOGIN);
        } else if (uiOptions == UIOptions.SELECT_ACCOUNT) {
            requestParameters.put(OauthConstants.Oauth2Parameters.PROMPT, OauthConstants.PromptValue.SELECT_ACCOUNT);
        } else if (uiOptions == UIOptions.ACT_AS_CURRENT_USER) {
            requestParameters.put(OauthConstants.Oauth2Parameters.RESTRICT_TO_HINT, "true");
        }
    }

    private String encodeProtocolState() throws UnsupportedEncodingException {
        final String state = String.format("a=%s&r=%s", MSALUtils.urlEncode(
                mAuthRequestParameters.getAuthority().getAuthority()),
                MSALUtils.urlEncode(MSALUtils.convertSetToString(
                        mAuthRequestParameters.getScope(), " ")));
        return Base64.encodeToString(state.getBytes("UTF-8"), Base64.NO_PADDING | Base64.URL_SAFE);
    }

    private void processAuthorizationResult(final AuthorizationResult authorizationResult)
            throws MSALUserCancelException, AuthenticationException {
        if (authorizationResult == null) {
            // TODO: throw unknown error
            //CHECKSTYLE:ON: checkstyle:EmptyBlock
        }

        switch (authorizationResult.getAuthorizationStatus()) {
            case USER_CANCEL:
                throw new MSALUserCancelException();
            case FAIL:
                // TODO: if clicking on the cancel button in the signin page, we get sub_error with the returned url,
                // however we cannot take dependency on the sub_error. Then how do we know user click on the cancel button
                // and that's actually a cancel request? Is server going to return some error code that we can use?
                throw new AuthenticationException(MSALError.AUTH_FAILED, authorizationResult.getError() + ";"
                        + authorizationResult.getErrorDescription());
            case SUCCESS:
                // verify if the state is the same as the one we send
                verifyStateInResponse(authorizationResult.getState());

                // Happy path, continue the process to use code for new access token.
                return;
            default:
                throw new IllegalStateException("Unknown status code");
        }
    }

    private void verifyStateInResponse(final String stateInResponse) throws AuthenticationException {
        final String decodeState = decodeState(stateInResponse);
        final Map<String, String> stateMap = MSALUtils.decodeUrlToMap(decodeState, "&");

        if (stateMap.size() != 2
                || !mAuthRequestParameters.getAuthority().getAuthority().equals(stateMap.get("a"))) {
            throw new AuthenticationException(MSALError.AUTH_FAILED, Constants.MSALErrorMessage.STATE_NOT_THE_SAME);
        }

        final Set<String> scopesInState = MSALUtils.getScopesAsSet(stateMap.get("r"));
        final Set<String> scopesInRequest = mAuthRequestParameters.getScope();
        if (scopesInState.size() != scopesInRequest.size() && !scopesInState.containsAll(scopesInRequest)) {
            throw new AuthenticationException(MSALError.AUTH_FAILED, Constants.MSALErrorMessage.STATE_NOT_THE_SAME);
        }
    }

    private String decodeState(final String encodedState) {
        if (MSALUtils.isEmpty(encodedState)) {
            return null;
        }

        final byte[] stateBytes = Base64.decode(encodedState, Base64.NO_PADDING | Base64.URL_SAFE);
        return new String(stateBytes);
    }
}
