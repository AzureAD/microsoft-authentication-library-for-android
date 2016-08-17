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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
    private static final String DEFAULT_AUTHORIZE_ENDPOINT = "oauth2/v2.0/authorize";
    private final Set<String> mAdditionalScope = new HashSet<>();

    static final String DISABLE_CHROMETAB = "disablechrometab";
    static AuthorizationResult sAuthorizationResult;

    private final Activity mActivity;

    static CountDownLatch sResultLock = new CountDownLatch(1);

    InteractiveRequest(final Activity activity, final AuthenticationRequestParameters authRequestParameters,
                       final String[] additionalScope) {
        super(activity.getApplication(), authRequestParameters);

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

        // verify the UI option. If UI option is set as as_as_current_user, login hint has to be provied.
        if (MSALUtils.isEmpty(authRequestParameters.getLoginHint())
                && authRequestParameters.getUIOption() == UIOptions.ACT_AS_CURRENT_USER) {
            throw new IllegalArgumentException(
                    "loginhint has to be provided if setting UI option as ACT_AS_CURRENT_USER");
        }

        // For interactive request, don't look up from cache. Inter
        mLoadFromCache = false;
    }

    final void preTokenRequest() throws MSALUserCancelException, AuthenticationException{
        final String authorizeUri;
        try {
            authorizeUri = getAuthorizationUri();
        } catch (final UnsupportedEncodingException e) {
            throw new AuthenticationException(MSALError.UNSUPPORTED_ENCODING, e.getMessage(), e);
        }

        final Intent intentToLaunch = new Intent(mContext, AuthenticationActivity.class);
        intentToLaunch.putExtra(Constants.REQUEST_URL_KEY, authorizeUri);
        intentToLaunch.putExtra(Constants.REDIRECT_INTENT, mAuthRequestParameters.getRedirectUri());
        intentToLaunch.putExtra(Constants.REQUEST_ID, mRequestId);
        if (mAuthRequestParameters.getSettings().getDisableCustomTab()) {
            intentToLaunch.putExtra(DISABLE_CHROMETAB, true);
        }

        // TODO: put a request id.
        if (!resolveIntent(intentToLaunch)) {
            // TODO: what is the exception to throw
            throw new AuthenticationException();
        }

        mActivity.startActivityForResult(intentToLaunch, Constants.UIRequest.BROWSER_FLOW);
        try {
            if (sResultLock.getCount() == 0) {
                sResultLock = new CountDownLatch(1);
            }
            sResultLock.await();
        } catch (final InterruptedException e) {
            // TODO: logging.
        }

        if (sAuthorizationResult == null) {
            // TODO: throw unknown error
        }

        if (sAuthorizationResult.getError() == Constants.MSALError.USER_CANCEL) {
            throw new MSALUserCancelException();
        } else if (sAuthorizationResult.getError() == Constants.MSALError.AUTHORIZATION_FAILED) {
            // TODO: throw the error back to developer with originally returned error code.
        }
    }

    private boolean resolveIntent(final Intent intent) {
        final ResolveInfo resolveInfo = mContext.getPackageManager().resolveActivity(intent, 0);
        return resolveInfo != null;
    }

    final void setAdditionalRequestBody(final Oauth2Client oauth2Client) {
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.GRANT_TYPE,
                OauthConstants.Oauth2GrantType.AUTHORIZATION_CODE);
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.CODE, sAuthorizationResult.getAuthCode());
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.REDIRECT_URI,
                mAuthRequestParameters.getRedirectUri());
    }

    String getAuthorizationUri() throws UnsupportedEncodingException {
        final Map<String, String> requestParameters = createRequestParameters();
        final String queryString = buildQueryParameter(requestParameters);

        return String.format("%s?%s", mAuthRequestParameters.getAuthority().getAuthorityUrl() + DEFAULT_AUTHORIZE_ENDPOINT,
                queryString);
    }

    private String buildQueryParameter(final Map<String, String> requestParameters) throws UnsupportedEncodingException {
        final Uri.Builder queryParameters = new Uri.Builder();
        for (Map.Entry<String, String> entry : requestParameters.entrySet()) {
            queryParameters.appendQueryParameter(entry.getKey(), URLEncoder.encode(entry.getValue(),
                    MSALUtils.ENCODING_UTF8));
        }

        String queryString = queryParameters.build().getQuery();
        final String extraQP = mAuthRequestParameters.getExtraQueryParam();
        if (!MSALUtils.isEmpty(extraQP)) {
            String parsedQP = extraQP;
            if (!extraQP.startsWith("&")) {
                parsedQP = "&" + parsedQP;
            }

            queryString += parsedQP;
        }

        return  queryString;
    }

    private Map<String, String> createRequestParameters() {
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
                mAuthRequestParameters.getCorrelationId().toString());
        requestParameters.putAll(PlatformIdHelper.getPlatformIdParameters());

        if (!MSALUtils.isEmpty(mAuthRequestParameters.getPolicy())) {
            requestParameters.put(OauthConstants.Oauth2Parameters.POLICY, mAuthRequestParameters.getPolicy());
        }

        if (!MSALUtils.isEmpty(mAuthRequestParameters.getLoginHint())) {
            requestParameters.put(OauthConstants.Oauth2Parameters.LOGIN_HINT, mAuthRequestParameters.getLoginHint());
        }

        // add hasChrome
        if (MSALUtils.isEmpty(mAuthRequestParameters.getExtraQueryParam())
                || mAuthRequestParameters.getExtraQueryParam().contains(OauthConstants.Oauth2Parameters.HAS_CHROME)) {
            requestParameters.put(OauthConstants.Oauth2Parameters.HAS_CHROME, "1");
        }

        addUiOptionToRequestParameters(requestParameters);

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

    static void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode != Constants.UIRequest.BROWSER_FLOW) {
            sAuthorizationResult = AuthorizationResult.getAuthorizationResultWithInvalidServerResponse();
        } else {
            // check it is the same request.
            processRedirectContainingAuthorizationResult(resultCode, data);
        }

        sResultLock.countDown();
    }

    private static void processRedirectContainingAuthorizationResult(int resultCode, final Intent data) {
        if (data == null) {
            // TODO: set authorizationResult
        } else {
            if (resultCode == Constants.UIResponse.CANCEL) {
                sAuthorizationResult = AuthorizationResult.getAuthorizationResultWithUserCancel();
            } else if (resultCode == Constants.UIResponse.AUTH_CODE_COMPLETE) {
                final String url = data.getStringExtra(Constants.AUTHORIZATION_FINAL_URL);
                sAuthorizationResult = AuthorizationResult.parseAuthorizationResponse(url);
            } else if (resultCode == Constants.UIResponse.AUTH_CODE_ERROR) {
                // TODO: handle to code error case.
            }
        }
    }

}
