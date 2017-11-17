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
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
    private static CountDownLatch sResultLock = new CountDownLatch(1);

    private final ActivityWrapper mActivityWrapper;
    private PKCEChallengeFactory.PKCEChallenge mPKCEChallenge;

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
        final String authorizeUri;
        try {
            Logger.info(TAG, mAuthRequestParameters.getRequestContext(), "Prepare authorize request uri for interactive flow.");
            authorizeUri = appendQueryStringToAuthorizeEndpoint();
        } catch (final UnsupportedEncodingException e) {
            throw new MsalClientException(MsalClientException.UNSUPPORTED_ENCODING, e.getMessage(), e);
        }

        final Intent intentToLaunch = new Intent(mContext, AuthenticationActivity.class);
        intentToLaunch.putExtra(Constants.REQUEST_URL_KEY, authorizeUri);
        intentToLaunch.putExtra(Constants.REQUEST_ID, mRequestId);
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

    @Override
    void setAdditionalOauthParameters(final Oauth2Client oauth2Client) {
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.GRANT_TYPE,
                OauthConstants.Oauth2GrantType.AUTHORIZATION_CODE);
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.CODE, sAuthorizationResult.getAuthCode());
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.REDIRECT_URI,
                mAuthRequestParameters.getRedirectUri());
        // Adding code verifier per PKCE spec. See https://tools.ietf.org/html/rfc7636
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.CODE_VERIFIER, mPKCEChallenge.mCodeVerifier);
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

    String appendQueryStringToAuthorizeEndpoint() throws UnsupportedEncodingException, MsalClientException {
        String authorizationUrl = MsalUtils.appendQueryParameterToUrl(
                mAuthRequestParameters.getAuthority().getAuthorizeEndpoint(),
                createAuthorizationRequestParameters());

        Logger.infoPII(TAG, mAuthRequestParameters.getRequestContext(), "Request uri to authorize endpoint is: " + authorizationUrl);
        return authorizationUrl;
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
        // Create our Challenge
        mPKCEChallenge = PKCEChallengeFactory.newPKCEChallenge();

        // Add it to our Authorization request
        requestParameters.put(OauthConstants.Oauth2Parameters.CODE_CHALLENGE, mPKCEChallenge.mCodeChallenge);
        requestParameters.put(OauthConstants.Oauth2Parameters.CODE_CHALLENGE_METHOD, PKCEChallengeFactory.PKCEChallenge.ChallengeMethod.S256.name());
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
        final String decodeState = decodeState(stateInResponse);
        final Map<String, String> stateMap = MsalUtils.decodeUrlToMap(decodeState, "&");

        if (stateMap.size() != 2
                || !mAuthRequestParameters.getAuthority().getAuthority().equals(stateMap.get("a"))) {
            throw new MsalClientException(MsalClientException.STATE_MISMATCH, Constants.MsalErrorMessage.STATE_NOT_THE_SAME);
        }

        final Set<String> scopesInState = MsalUtils.getScopesAsSet(stateMap.get("r"));
        final Set<String> scopesInRequest = mAuthRequestParameters.getScope();
        if (scopesInState.size() != scopesInRequest.size() && !scopesInState.containsAll(scopesInRequest)) {
            throw new MsalClientException(MsalClientException.STATE_MISMATCH, Constants.MsalErrorMessage.STATE_NOT_THE_SAME);
        }
    }

    private String decodeState(final String encodedState) {
        if (MsalUtils.isEmpty(encodedState)) {
            return null;
        }

        final byte[] stateBytes = Base64.decode(encodedState, Base64.NO_PADDING | Base64.URL_SAFE);
        return new String(stateBytes, Charset.defaultCharset());
    }

    private void addExtraQueryParameter(final String key, final String value, final Map<String, String> requestParams) {
        if (!MsalUtils.isEmpty(key) && !MsalUtils.isEmpty(value)) {
            requestParams.put(key, value);
        }
    }

    /**
     * Factory class for PKCE Challenges.
     */
    private static class PKCEChallengeFactory {

        private static final int CODE_VERIFIER_BYTE_SIZE = 32;
        private static final int ENCODE_MASK = Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP;
        private static final String DIGEST_ALGORITHM = "SHA-256";
        private static final String ISO_8859_1 = "ISO_8859_1";

        static class PKCEChallenge {

            /**
             * The client creates a code challenge derived from the code
             * verifier by using one of the following transformations.
             * <p>
             * Sophisticated attack scenarios allow the attacker to
             * observe requests (in addition to responses) to the
             * authorization endpoint.  The attacker is, however, not able to
             * act as a man in the middle. To mitigate this,
             * "code_challenge_method" value must be set either to "S256" or
             * a value defined by a cryptographically secure
             * "code_challenge_method" extension. In this implementation "S256" is used.
             * <p>
             * Example for the S256 code_challenge_method
             *
             * @see <a href="https://tools.ietf.org/html/rfc7636#page-17">RFC-7636</a>
             */
            enum ChallengeMethod {
                S256
            }

            /**
             * A cryptographically random string that is used to correlate the
             * authorization request to the token request.
             * <p>
             * code-verifier = 43*128unreserved
             * where...
             * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
             * ALPHA = %x41-5A / %x61-7A
             * DIGIT = %x30-39
             */
            private final String mCodeVerifier;

            /**
             * A challenge derived from the code verifier that is sent in the
             * authorization request, to be verified against later.
             */
            private final String mCodeChallenge;

            PKCEChallenge(String codeVerifier, String codeChallenge) {
                this.mCodeVerifier = codeVerifier;
                this.mCodeChallenge = codeChallenge;
            }
        }

        /**
         * Creates a new instance of {@link PKCEChallenge}.
         *
         * @return the newly created Challenge
         * @throws MsalException if the Challenge could not be created
         */
        static PKCEChallenge newPKCEChallenge() throws MsalClientException {
            // Generate the code_verifier as a high-entropy cryptographic random String
            final String codeVerifier = generateCodeVerifier();

            // Create a code_challenge derived from the code_verifier
            final String codeChallenge = generateCodeVerifierChallenge(codeVerifier);

            return new PKCEChallenge(codeVerifier, codeChallenge);
        }

        private static String generateCodeVerifier() {
            final byte[] verifierBytes = new byte[CODE_VERIFIER_BYTE_SIZE];
            new SecureRandom().nextBytes(verifierBytes);
            return Base64.encodeToString(verifierBytes, ENCODE_MASK);
        }

        private static String generateCodeVerifierChallenge(final String verifier) throws MsalClientException {
            try {
                MessageDigest digester = MessageDigest.getInstance(DIGEST_ALGORITHM);
                digester.update(verifier.getBytes(ISO_8859_1));
                byte[] digestBytes = digester.digest();
                return Base64.encodeToString(digestBytes, ENCODE_MASK);
            } catch (final NoSuchAlgorithmException e) {
                throw new MsalClientException(MsalClientException.NO_SUCH_ALGORITHM, "Failed to generate the code verifier challenge", e);
            } catch (final UnsupportedEncodingException e) {
                throw new MsalClientException(MsalClientException.UNSUPPORTED_ENCODING,
                        "Every implementation of the Java platform is required to support ISO-8859-1."
                                + "Consult the release documentation for your implementation.", e);
            }
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
