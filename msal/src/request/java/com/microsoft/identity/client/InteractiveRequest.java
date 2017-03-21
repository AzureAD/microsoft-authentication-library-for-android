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
    private final Set<String> mAdditionalScope = new HashSet<>();

    static final int BROWSER_FLOW = 1001;
    private static AuthorizationResult sAuthorizationResult;
    private static CountDownLatch sResultLock = new CountDownLatch(1);

    private final Activity mActivity;
    private PKCEChallengeFactory.PKCEChallenge mPKCEChallenge;

    /**
     * Constructor for {@link InteractiveRequest}.
     *
     * @param activity              {@link Activity} used to launch the {@link AuthenticationActivity}.
     * @param authRequestParameters {@link AuthenticationRequestParameters} that is holding all the parameters for oauth request.
     * @param additionalScope       An array of additional scopes.
     */
    InteractiveRequest(final Activity activity, final AuthenticationRequestParameters authRequestParameters,
                       final String[] additionalScope, final ApiEvent.Builder apiEventBuilder) {
        super(activity.getApplicationContext(), authRequestParameters, apiEventBuilder);
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
    }

    /**
     * Pre token request. Launch either chrome custom tab or chrome to get the auth code back.
     */
    @Override
    synchronized void preTokenRequest() throws MSALUserCancelException, AuthenticationException {
        final String authorizeUri;
        try {
            Logger.info(TAG, mAuthRequestParameters.getRequestContext(), "Prepare authorize request uri for interactive flow.");
            authorizeUri = appendQueryStringToAuthorizeEndpoint();
        } catch (final UnsupportedEncodingException e) {
            throw new AuthenticationException(MSALError.UNSUPPORTED_ENCODING, e.getMessage(), e);
        }

        final Intent intentToLaunch = new Intent(mContext, AuthenticationActivity.class);
        intentToLaunch.putExtra(Constants.REQUEST_URL_KEY, authorizeUri);
        intentToLaunch.putExtra(Constants.REQUEST_ID, mRequestId);
        intentToLaunch.putExtra(
                Constants.TELEMETRY_REQUEST_ID,
                mAuthRequestParameters.getRequestContext().getTelemetryRequestId().value
        );

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
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.CODE_VERIFIER, mPKCEChallenge.codeVerifier);
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

    String appendQueryStringToAuthorizeEndpoint() throws UnsupportedEncodingException, AuthenticationException {
        String authorizationUrl = MSALUtils.appendQueryParameterToUrl(
                mAuthRequestParameters.getAuthority().getAuthorizeEndpoint(),
                createAuthorizationRequestParameters());

        final String extraQP = mAuthRequestParameters.getExtraQueryParam();
        if (!MSALUtils.isEmpty(extraQP)) {
            String parsedQP = extraQP;
            if (!extraQP.startsWith("&")) {
                parsedQP = "&" + parsedQP;
            }

            authorizationUrl += parsedQP;
        }

        Logger.infoPII(TAG, mAuthRequestParameters.getRequestContext(), "Request uri to authorize endpoint is: " + authorizationUrl);
        return authorizationUrl;
    }

    private boolean resolveIntent(final Intent intent) {
        final ResolveInfo resolveInfo = mContext.getPackageManager().resolveActivity(intent, 0);
        return resolveInfo != null;
    }

    private Map<String, String> createAuthorizationRequestParameters() throws UnsupportedEncodingException, AuthenticationException {
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

        addUiBehaviorToRequestParameters(requestParameters);

        // append state in the query parameters
        requestParameters.put(OauthConstants.Oauth2Parameters.STATE, encodeProtocolState());

        // Add PKCE Challenge
        addPKCEChallengeToRequestParameters(requestParameters);

        return requestParameters;
    }

    private void addPKCEChallengeToRequestParameters(final Map<String, String> requestParameters) throws AuthenticationException {
        // Create our Challenge
        mPKCEChallenge = PKCEChallengeFactory.newPKCEChallenge();

        // Add it to our Authorization request
        requestParameters.put(OauthConstants.Oauth2Parameters.CODE_CHALLENGE, mPKCEChallenge.codeChallenge);
        requestParameters.put(OauthConstants.Oauth2Parameters.CODE_CHALLENGE_METHOD, PKCEChallengeFactory.PKCEChallenge.ChallengeMethod.S256.name());
    }

    private void addUiBehaviorToRequestParameters(final Map<String, String> requestParameters) {
        final UIBehavior uiBehavior = mAuthRequestParameters.getUiBehavior();
        if (uiBehavior == UIBehavior.FORCE_LOGIN) {
            requestParameters.put(OauthConstants.Oauth2Parameters.PROMPT, OauthConstants.PromptValue.LOGIN);
        } else if (uiBehavior == UIBehavior.SELECT_ACCOUNT) {
            requestParameters.put(OauthConstants.Oauth2Parameters.PROMPT, OauthConstants.PromptValue.SELECT_ACCOUNT);
        } else if (uiBehavior == UIBehavior.CONSENT) {
            requestParameters.put(OauthConstants.Oauth2Parameters.PROMPT, OauthConstants.PromptValue.CONSENT);
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
            Logger.error(TAG, mAuthRequestParameters.getRequestContext(), "Authorization result is null", null);
            // TODO: throw unknown error
            //CHECKSTYLE:ON: checkstyle:EmptyBlock
        }

        final AuthorizationResult.AuthorizationStatus status = authorizationResult.getAuthorizationStatus();
        Logger.info(TAG, mAuthRequestParameters.getRequestContext(), "Authorize request status is: " + status.toString());
        switch (status) {
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

    /**
     * Factory class for PKCE Challenges
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
            final String codeVerifier;

            /**
             * A challenge derived from the code verifier that is sent in the
             * authorization request, to be verified against later.
             */
            final String codeChallenge;

            /**
             * A method that was used to derive code challenge.
             */
            final ChallengeMethod method;

            PKCEChallenge(String codeVerifier, String codeChallenge, ChallengeMethod method) {
                this.codeVerifier = codeVerifier;
                this.codeChallenge = codeChallenge;
                this.method = method;
            }
        }

        /**
         * Creates a new instance of {@link PKCEChallenge}
         *
         * @return the newly created Challenge
         * @throws AuthenticationException if the Challenge could not be created
         */
        static PKCEChallenge newPKCEChallenge() throws AuthenticationException {
            // Generate the code_verifier as a high-entropy cryptographic random String
            final String codeVerifier = generateCodeVerifier();

            // Create a code_challenge derived from the code_verifier
            final String codeChallenge = generateCodeVerifierChallenge(codeVerifier);

            // Set the challenge_method - only SHA-256 should be used
            final PKCEChallenge.ChallengeMethod challengeMethod = PKCEChallenge.ChallengeMethod.S256;

            return new PKCEChallenge(codeVerifier, codeChallenge, challengeMethod);
        }

        private static String generateCodeVerifier() {
            final byte[] verifierBytes = new byte[CODE_VERIFIER_BYTE_SIZE];
            new SecureRandom().nextBytes(verifierBytes);
            return Base64.encodeToString(verifierBytes, ENCODE_MASK);
        }

        private static String generateCodeVerifierChallenge(final String verifier) throws AuthenticationException {
            try {
                MessageDigest digester = MessageDigest.getInstance(DIGEST_ALGORITHM);
                digester.update(verifier.getBytes(ISO_8859_1));
                byte[] digestBytes = digester.digest();
                return Base64.encodeToString(digestBytes, ENCODE_MASK);
            } catch (NoSuchAlgorithmException e) {
                throw new AuthenticationException(MSALError.NO_SUCH_ALGORITHM);
            } catch (UnsupportedEncodingException e) {
                throw new AuthenticationException(
                        MSALError.UNSUPPORTED_ENCODING,
                        "Every implementation of the Java platform is required to support ISO-8859-1."
                                + "Consult the release documentation for your implementation.",
                        e
                );
            }
        }
    }
}
