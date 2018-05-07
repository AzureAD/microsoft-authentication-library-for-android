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
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;
import android.util.Base64;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.microsoft.identity.client.AndroidTestUtil.MOCK_UID;
import static com.microsoft.identity.client.AndroidTestUtil.MOCK_UTID;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link InteractiveRequest}.
 */
@RunWith(AndroidJUnit4.class)
public final class InteractiveRequestTest extends AndroidTestCase {
    static final String AUTHORITY = "https://login.microsoftonline.com/common";
    static final String CLIENT_ID = "client-id";
    static final UUID CORRELATION_ID = UUID.randomUUID();
    static final String LOGIN_HINT = "test@test.onmicrosoft.com";
    static final int THREAD_DELAY_TIME = 200;

    /**
     * Min length of code_challenge Strings
     *
     * @see <a href="https://tools.ietf.org/html/rfc7636#section-4.1">RFC-7636</a>
     */
    private static final int CODE_CHALLENGE_LENGTH_MIN = 43;

    /**
     * Max length of code_challenge Strings
     *
     * @see <a href="https://tools.ietf.org/html/rfc7636#section-4.1">RFC-7636</a>
     */
    private static final int CODE_CHALLENGE_LENGTH_MAX = 128;

    private Context mAppContext;
    private String mRedirectUri;
    private TokenCache mTokenCache;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        InstrumentationRegistry.getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache",
                InstrumentationRegistry.getContext().getCacheDir().getPath());
        Authority.RESOLVED_AUTHORITY.clear();

        mAppContext = new MockContext(InstrumentationRegistry.getContext().getApplicationContext());
        resolveAuthenticationActivity(mAppContext, true);
        mockNetworkConnected(mAppContext, true);
        mRedirectUri = "msauth-client-id://" + mAppContext.getPackageName();
        mTokenCache = new TokenCache(mAppContext);
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
        AndroidTestUtil.removeAllTokens(mAppContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullScope() {
        new InteractiveRequest(Mockito.mock(Activity.class), getAuthRequestParameters(AUTHORITY,
                null, mRedirectUri, LOGIN_HINT, UiBehavior.FORCE_LOGIN, null), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorEmptyScope() {
        new InteractiveRequest(Mockito.mock(Activity.class), getAuthRequestParameters(AUTHORITY,
                Collections.<String>emptySet(), mRedirectUri, LOGIN_HINT, UiBehavior.FORCE_LOGIN, null), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorScopeContainsReservedScope() {
        final Set<String> scopes = new HashSet<>();
        scopes.add(OauthConstants.Oauth2Value.SCOPE_PROFILE);

        new InteractiveRequest(Mockito.mock(Activity.class), getAuthRequestParameters(AUTHORITY, scopes, mRedirectUri,
                LOGIN_HINT, UiBehavior.FORCE_LOGIN, null), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorClientIdIsNotSingleScope() {
        final Set<String> scopes = getScopesContainsReservedScope();

        new InteractiveRequest(Mockito.mock(Activity.class), getAuthRequestParameters(AUTHORITY, scopes, mRedirectUri,
                LOGIN_HINT, UiBehavior.FORCE_LOGIN, null), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorClientIdAsSingleScope() {
        final Set<String> scopes = new HashSet<>();
        scopes.add(CLIENT_ID);

        new InteractiveRequest(Mockito.mock(Activity.class), getAuthRequestParameters(AUTHORITY, scopes, mRedirectUri,
                LOGIN_HINT, UiBehavior.FORCE_LOGIN, null), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorEmptyRedirectUri() {
        new InteractiveRequest(Mockito.mock(Activity.class), getAuthRequestParameters(AUTHORITY, getScopes(), "", LOGIN_HINT,
                UiBehavior.FORCE_LOGIN, null), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtraScopesToConsentContainsReservedScope() {
        final Set<String> extraScopesToConsent = getScopesContainsReservedScope();
        new InteractiveRequest(Mockito.mock(Activity.class), getAuthRequestParameters(AUTHORITY, getScopes(), "", LOGIN_HINT,
                UiBehavior.FORCE_LOGIN, null), extraScopesToConsent.toArray(new String[extraScopesToConsent.size()]));
    }

    @Test
    public void testGetAuthorizationUriUiBehaviorIsConsent() throws UnsupportedEncodingException, MsalException {
        final InteractiveRequest interactiveRequest = new InteractiveRequest(Mockito.mock(Activity.class),
                getAuthenticationParams(AUTHORITY, UiBehavior.CONSENT, null), null);
        final String actualAuthorizationUri = interactiveRequest.appendQueryStringToAuthorizeEndpoint();
        final Uri authorityUrl = Uri.parse(actualAuthorizationUri);
        Map<String, String> queryStrings = MsalUtils.decodeUrlToMap(authorityUrl.getQuery(), "&");

        assertTrue(MsalUtils.convertSetToString(getExpectedScopes(), " ").equals(
                queryStrings.get(OauthConstants.Oauth2Parameters.SCOPE)));
        assertTrue(OauthConstants.PromptValue.CONSENT.equals(queryStrings.get(OauthConstants.Oauth2Parameters.PROMPT)));
        verifyCommonQueryString(queryStrings);
    }

    @Test
    public void testGetAuthorizationUriContainsPKCEChallenge() throws UnsupportedEncodingException, MsalException {
        final InteractiveRequest interactiveRequest = new InteractiveRequest(Mockito.mock(Activity.class),
                getAuthenticationParams(AUTHORITY, UiBehavior.CONSENT, null), null);
        final String authUriStr = interactiveRequest.appendQueryStringToAuthorizeEndpoint();
        final Uri authorizationUri = Uri.parse(authUriStr);
        final String codeChallenge = authorizationUri.getQueryParameter(OauthConstants.Oauth2Parameters.CODE_CHALLENGE);
        final String codeChallengeMethod = authorizationUri.getQueryParameter(OauthConstants.Oauth2Parameters.CODE_CHALLENGE_METHOD);
        assertNotNull(codeChallenge);
        assertTrue(codeChallenge.length() >= CODE_CHALLENGE_LENGTH_MIN);
        assertTrue(codeChallenge.length() <= CODE_CHALLENGE_LENGTH_MAX);
        assertNotNull(codeChallengeMethod);
        assertTrue(codeChallengeMethod.equals("S256"));
    }

    @Test
    public void testGetAuthorizationUriContainsSessionContinuationParams() throws UnsupportedEncodingException, MsalClientException {
        final User user = new User(AndroidTestUtil.PREFERRED_USERNAME, "name", AndroidTestUtil.ISSUER, AndroidTestUtil.UID, AndroidTestUtil.UTID);
        final InteractiveRequest interactiveRequest = new InteractiveRequest(Mockito.mock(Activity.class), getAuthenticationParams(AUTHORITY,
                UiBehavior.CONSENT, user), null);
        final String authorizeRequestUriString = interactiveRequest.appendQueryStringToAuthorizeEndpoint();
        final Uri authorizationRequestUri = Uri.parse(authorizeRequestUriString);

        assertTrue(AndroidTestUtil.UID.equals(authorizationRequestUri.getQueryParameter(OauthConstants.Oauth2Parameters.LOGIN_REQ)));
        assertTrue(AndroidTestUtil.UTID.equals(authorizationRequestUri.getQueryParameter(OauthConstants.Oauth2Parameters.DOMAIN_REQ)));
        assertTrue(AndroidTestUtil.PREFERRED_USERNAME.equals(authorizationRequestUri.getQueryParameter(OauthConstants.Oauth2Parameters.LOGIN_HINT)));
    }

    @Test
    public void testGetAuthorizationUriUiBehaviorForceLogin() throws UnsupportedEncodingException, MsalException {
        final String[] extraScope = {"extraScope"};
        final InteractiveRequest interactiveRequest = new InteractiveRequest(Mockito.mock(Activity.class),
                getAuthenticationParams(AUTHORITY, UiBehavior.FORCE_LOGIN, null), extraScope);
        final String actualAuthorizationUri = interactiveRequest.appendQueryStringToAuthorizeEndpoint();
        final Uri authorityUrl = Uri.parse(actualAuthorizationUri);
        Map<String, String> queryStrings = MsalUtils.decodeUrlToMap(authorityUrl.getQuery(), "&");

        final Set<String> expectedScopes = getExpectedScopes();
        expectedScopes.add("extraScope");
        final String[] queryStringScopes = queryStrings.get(OauthConstants.Oauth2Parameters.SCOPE).split(" ");
        for (final String param : queryStringScopes) {
            // iterate and remove from set then verify that it is empty
            assertTrue(expectedScopes.contains(param));
            expectedScopes.remove(param);
        }
        assertTrue(expectedScopes.isEmpty());
        assertTrue(OauthConstants.PromptValue.LOGIN.equals(queryStrings.get(OauthConstants.Oauth2Parameters.PROMPT)));
        verifyCommonQueryString(queryStrings);
    }

    @Test
    public void testDuplicateExtraQp() throws UnsupportedEncodingException {
        final InteractiveRequest interactiveRequest = new InteractiveRequest(Mockito.mock(Activity.class),
                getAuthRequestParameters(AUTHORITY, UiBehavior.CONSENT, "&client_id=1234", null), null);
        try {
            interactiveRequest.appendQueryStringToAuthorizeEndpoint();
            fail();
        } catch (final MsalClientException ex) {
            assertTrue(MsalClientException.DUPLICATE_QUERY_PARAMETER.equals(ex.getErrorCode()));
        }
    }

    @Test
    public void testRequestContainsValidExtraQp() throws UnsupportedEncodingException {
        final String extraQp = "&slice=testslice";
        final InteractiveRequest interactiveRequest = new InteractiveRequest(Mockito.mock(Activity.class),
                getAuthRequestParameters(AUTHORITY, UiBehavior.CONSENT, extraQp, null), null);
        try {
            final String authorizationRequestUrl = interactiveRequest.appendQueryStringToAuthorizeEndpoint();
            assertTrue(authorizationRequestUrl.contains(extraQp));
        } catch (final MsalClientException ex) {
            fail("unexpected exception message");
        }
    }

    @Test
    public void testRequestContainsSliceParameters() throws MsalClientException, UnsupportedEncodingException {
        final String sliceParameters = "&slice=testslice&a=b&c=d";
        final String extraParameters = "&e=f&m=k";

        final InteractiveRequest interactiveRequest = new InteractiveRequest(Mockito.mock(Activity.class),
                getAuthRequestParameters(AUTHORITY, UiBehavior.CONSENT, extraParameters, sliceParameters), null);
        try {
            final String authorizationRequestUrl = interactiveRequest.appendQueryStringToAuthorizeEndpoint();
            assertTrue(authorizationRequestUrl.contains("slice=testslice"));
            assertTrue(authorizationRequestUrl.contains("a=b"));
            assertTrue(authorizationRequestUrl.contains("c=d"));

            assertTrue(authorizationRequestUrl.contains("e=f"));
            assertTrue(authorizationRequestUrl.contains("m=k"));
        } catch (final MsalClientException ex) {
            fail("unexpected exception message");
        }
    }

    @Test
    public void testSliceParametersDuplicated() throws UnsupportedEncodingException {
        final String sliceParameters = "&slice=testslice&a=b&c=d";
        final String extraParameters = "slice=testslice&e=f&m=k";

        final InteractiveRequest interactiveRequest = new InteractiveRequest(Mockito.mock(Activity.class),
                getAuthRequestParameters(AUTHORITY, UiBehavior.CONSENT, extraParameters, sliceParameters), null);
        try {
            interactiveRequest.appendQueryStringToAuthorizeEndpoint();
        } catch (final MsalClientException ex) {
            assertTrue(MsalClientException.DUPLICATE_QUERY_PARAMETER.equals(ex.getErrorCode()));
        }
    }

    @Test
    public void testNetworkNotConnectedForUI() throws IOException, InterruptedException {
        final Activity testActivity = Mockito.mock(Activity.class);
        Mockito.when(testActivity.getPackageName()).thenReturn(mAppContext.getPackageName());
        Mockito.when(testActivity.getApplicationContext()).thenReturn(mAppContext);

        // mock http call
        AndroidTestMockUtil.mockSuccessTenantDiscovery(getExpectedAuthorizeEndpoint(), getExpectedTokenEndpoint());
        mockSuccessHttpRequestCallWithNoRT();

        // turn off network
        mockNetworkConnected(mAppContext, false);

        final BaseRequest request = createInteractiveRequest(AUTHORITY, testActivity);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(MsalException exception) {
                assertTrue(MsalClientException.DEVICE_NETWORK_NOT_AVAILABLE.equals(exception.getErrorCode()));
                resultLock.countDown();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        // having the thread delayed for preTokenRequest to finish. Here we mock the
        // startActivityForResult, nothing actually happened when AuthenticationActivity is called.
        resultLock.await(THREAD_DELAY_TIME, TimeUnit.MILLISECONDS);

        final Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.AUTHORIZATION_FINAL_URL, mRedirectUri
                + "?code=1234&state=" + AndroidTestUtil.encodeProtocolState(AUTHORITY, getScopes()));
        InteractiveRequest.onActivityResult(InteractiveRequest.BROWSER_FLOW,
                Constants.UIResponse.AUTH_CODE_COMPLETE, resultIntent);

        resultLock.await();

        // verify that startActivityResult is called
        Mockito.verify(testActivity, Mockito.never()).startActivityForResult(Mockito.argThat(new ArgumentMatcher<Intent>() {
            @Override
            public boolean matches(Intent argument) {
                return argument.getStringExtra(Constants.REQUEST_URL_KEY) != null;
            }
        }), Mockito.eq(InteractiveRequest.BROWSER_FLOW));
    }

    @Test
    public void testNetworkTurnedOffForTokenEndpoint() throws IOException, InterruptedException {
        final Activity testActivity = Mockito.mock(Activity.class);
        Mockito.when(testActivity.getPackageName()).thenReturn(mAppContext.getPackageName());
        Mockito.when(testActivity.getApplicationContext()).thenReturn(mAppContext);

        // mock http call
        AndroidTestMockUtil.mockSuccessTenantDiscovery(getExpectedAuthorizeEndpoint(), getExpectedTokenEndpoint());
        mockSuccessHttpRequestCallWithNoRT();

        final BaseRequest request = createInteractiveRequest(AUTHORITY, testActivity);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(MsalException exception) {
                assertTrue(MsalClientException.DEVICE_NETWORK_NOT_AVAILABLE.equals(exception.getErrorCode()));
                resultLock.countDown();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        // having the thread delayed for preTokenRequest to finish. Here we mock the
        // startActivityForResult, nothing actually happened when AuthenticationActivity is called.
        resultLock.await(THREAD_DELAY_TIME, TimeUnit.MILLISECONDS);

        final Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.AUTHORIZATION_FINAL_URL, mRedirectUri
                + "?code=1234&state=" + AndroidTestUtil.encodeProtocolState(AUTHORITY, getScopes()));

        mockNetworkConnected(mAppContext, false);

        InteractiveRequest.onActivityResult(InteractiveRequest.BROWSER_FLOW,
                Constants.UIResponse.AUTH_CODE_COMPLETE, resultIntent);

        resultLock.await();

        // verify that startActivityResult is called
        verifyStartActivityForResultCalled(testActivity);
    }

    /**
     * Verify when auth code is successfully returned, result is delivered correctly.
     */
    @Test
    public void testGetTokenCodeSuccessfullyReturnedNoClientInfoReturned() throws IOException, InterruptedException {
        final Activity testActivity = Mockito.mock(Activity.class);
        Mockito.when(testActivity.getPackageName()).thenReturn(mAppContext.getPackageName());
        Mockito.when(testActivity.getApplicationContext()).thenReturn(mAppContext);

        // mock http call
        AndroidTestMockUtil.mockSuccessTenantDiscovery(getExpectedAuthorizeEndpoint(), getExpectedTokenEndpoint());
        mockSuccessHttpRequestCallWithNoRT();

        final BaseRequest request = createInteractiveRequest(AUTHORITY, testActivity);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                Assert.assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 1);
                assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 0);

                // make sure access token is stored with tenant specific authority
                assertNull(mTokenCache.findAccessToken(getAuthenticationParams(AUTHORITY, UiBehavior.FORCE_LOGIN, null), authenticationResult.getUser()));
                final String authority = AUTHORITY.replace("common", authenticationResult.getTenantId());
                assertNotNull(mTokenCache.findAccessToken(getAuthenticationParams(authority, UiBehavior.FORCE_LOGIN, null), authenticationResult.getUser()));

                final User user = authenticationResult.getUser();
                assertTrue(user.getUid().equals(MOCK_UID));
                assertTrue(user.getUtid().equals(MOCK_UTID));

                resultLock.countDown();

            }

            @Override
            public void onError(MsalException exception) {
                fail();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        // having the thread delayed for preTokenRequest to finish. Here we mock the
        // startActivityForResult, nothing actually happened when AuthenticationActivity is called.
        resultLock.await(THREAD_DELAY_TIME, TimeUnit.MILLISECONDS);

        final Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.AUTHORIZATION_FINAL_URL, mRedirectUri
                + "?code=1234&state=" + AndroidTestUtil.encodeProtocolState(AUTHORITY, getScopes()));
        InteractiveRequest.onActivityResult(InteractiveRequest.BROWSER_FLOW,
                Constants.UIResponse.AUTH_CODE_COMPLETE, resultIntent);

        resultLock.await();

        // verify that startActivityResult is called
        verifyStartActivityForResultCalled(testActivity);
    }

    @Test
    public void testAcquireTokenExpiresInNotReturned() throws IOException, InterruptedException {
        final Activity testActivity = Mockito.mock(Activity.class);
        Mockito.when(testActivity.getPackageName()).thenReturn(mAppContext.getPackageName());
        Mockito.when(testActivity.getApplicationContext()).thenReturn(mAppContext);

        // mock http call
        AndroidTestMockUtil.mockSuccessTenantDiscovery(getExpectedAuthorizeEndpoint(), getExpectedTokenEndpoint());

        // mock response with no expires returned
        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponseNoExpires(AndroidTestUtil.TEST_IDTOKEN, AndroidTestUtil.ACCESS_TOKEN, MsalUtils.convertSetToString(getScopes(), " "),
                        AndroidTestUtil.createRawClientInfo(AndroidTestUtil.UID, AndroidTestUtil.UTID)));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        final BaseRequest request = createInteractiveRequest(AUTHORITY, testActivity);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                Assert.assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 1);
                assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);

                // make sure access token is stored with tenant specific authority
                assertNull(mTokenCache.findAccessToken(getAuthenticationParams(AUTHORITY, UiBehavior.FORCE_LOGIN, authenticationResult.getUser()), authenticationResult.getUser()));
                final String authority = AUTHORITY.replace("common", authenticationResult.getTenantId());
                assertNotNull(mTokenCache.findAccessToken(getAuthenticationParams(authority, UiBehavior.FORCE_LOGIN, authenticationResult.getUser()), authenticationResult.getUser()));

                assertNotNull(authenticationResult.getExpiresOn());
                resultLock.countDown();
            }

            @Override
            public void onError(MsalException exception) {
                fail();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        // having the thread delayed for preTokenRequest to finish. Here we mock the
        // startActivityForResult, nothing actually happened when AuthenticationActivity is called.
        resultLock.await(THREAD_DELAY_TIME, TimeUnit.MILLISECONDS);

        final Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.AUTHORIZATION_FINAL_URL, mRedirectUri
                + "?code=1234&state=" + AndroidTestUtil.encodeProtocolState(AUTHORITY, getScopes()));
        InteractiveRequest.onActivityResult(InteractiveRequest.BROWSER_FLOW,
                Constants.UIResponse.AUTH_CODE_COMPLETE, resultIntent);

        resultLock.await();

        // verify that startActivityResult is called
        verifyStartActivityForResultCalled(testActivity);
    }

    /**
     * Verify auth code is returned successfully but token request with code fails.
     */
    @Test
    public void testAuthCodeReturnedTokenRequestFailed() throws IOException, InterruptedException {
        final Activity testActivity = Mockito.mock(Activity.class);
        Mockito.when(testActivity.getPackageName()).thenReturn(mAppContext.getPackageName());
        Mockito.when(testActivity.getApplicationContext()).thenReturn(mAppContext);

        // mock http call
        AndroidTestMockUtil.mockSuccessTenantDiscovery(getExpectedAuthorizeEndpoint(), getExpectedTokenEndpoint());

        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_BAD_REQUEST, AndroidTestUtil.getErrorResponseMessage("invalid_request"));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        final BaseRequest request = createInteractiveRequest(AUTHORITY, testActivity);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(MsalException exception) {
                assertTrue(exception instanceof MsalServiceException);

                final MsalServiceException serviceException = (MsalServiceException) exception;
                assertTrue(MsalServiceException.INVALID_REQUEST.equals(serviceException.getErrorCode()));
                assertTrue(!exception.getMessage().isEmpty());
                assertTrue(serviceException.getHttpStatusCode() == HttpURLConnection.HTTP_BAD_REQUEST);
                resultLock.countDown();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        // having the thread delayed for preTokenRequest to finish. Here we mock the
        // startActivityForResult, nothing actually happened when AuthenticationActivity is called.
        resultLock.await(THREAD_DELAY_TIME, TimeUnit.MILLISECONDS);

        final Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.AUTHORIZATION_FINAL_URL, mRedirectUri + "?code=1234&state="
                + AndroidTestUtil.encodeProtocolState(AUTHORITY, getScopes()));
        InteractiveRequest.onActivityResult(InteractiveRequest.BROWSER_FLOW,
                Constants.UIResponse.AUTH_CODE_COMPLETE, resultIntent);

        resultLock.await();

        // verify that startActivityResult is called
        verifyStartActivityForResultCalled(testActivity);
    }

    @Test
    public void testGetAuthTokenUserCancelWithBackButton()
            throws IOException, InterruptedException {
        final Activity testActivity = Mockito.mock(Activity.class);
        Mockito.when(testActivity.getPackageName()).thenReturn(mAppContext.getPackageName());
        Mockito.when(testActivity.getApplicationContext()).thenReturn(mAppContext);

        // mock http call
        AndroidTestMockUtil.mockSuccessTenantDiscovery(getExpectedAuthorizeEndpoint(), getExpectedTokenEndpoint());
        mockSuccessHttpRequestCallWithNoRT();

        final BaseRequest request = createInteractiveRequest(AUTHORITY, testActivity);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(MsalException exception) {
                fail();
            }

            @Override
            public void onCancel() {
                resultLock.countDown();
            }
        });

        // having the thread delayed for preTokenRequest to finish. Here we mock the
        // startActivityForResult, nothing actually happened when AuthenticationActivity is called.
        resultLock.await(THREAD_DELAY_TIME, TimeUnit.MILLISECONDS);

        InteractiveRequest.onActivityResult(InteractiveRequest.BROWSER_FLOW,
                Constants.UIResponse.CANCEL, new Intent());

        resultLock.await();

        // verify that startActivityResult is called
        verifyStartActivityForResultCalled(testActivity);

        // Verify that there is still one http url connection left in the queue
        assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 1);
    }

    /**
     * Verify that user cancel is sent back if user click on the cancel button on the sign page.
     */
    // TODO: Suppress the test. This is the case we want to make sure we send cancel back to calling app. However,
    // If we don't read the sub_err returned from server, no way to tell if the user
    // clicks the cancel button in the signin page to cancel the request or not.
    @Ignore
    @Test
    public void testGetTokenUserCancelWithCancelInSignInPage() throws IOException,
            InterruptedException {
        final Activity testActivity = Mockito.mock(Activity.class);
        Mockito.when(testActivity.getPackageName()).thenReturn(mAppContext.getPackageName());
        Mockito.when(testActivity.getApplicationContext()).thenReturn(mAppContext);

        // mock http call
        AndroidTestMockUtil.mockSuccessTenantDiscovery(getExpectedAuthorizeEndpoint(), getExpectedTokenEndpoint());
        mockSuccessHttpRequestCallWithNoRT();

        final BaseRequest request = createInteractiveRequest(AUTHORITY, testActivity);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(MsalException exception) {
                fail();
            }

            @Override
            public void onCancel() {
                resultLock.countDown();
            }
        });

        // having the thread delayed for preTokenRequest to finish. Here we mock the
        // startActivityForResult, nothing actually happened when AuthenticationActivity is called.
        resultLock.await(THREAD_DELAY_TIME, TimeUnit.MILLISECONDS);

        final Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.AUTHORIZATION_FINAL_URL, mRedirectUri
                + "?error=access_denied&error_subcode=cancel");
        InteractiveRequest.onActivityResult(InteractiveRequest.BROWSER_FLOW,
                Constants.UIResponse.AUTH_CODE_COMPLETE, resultIntent);

        resultLock.await();

        // verify that startActivityResult is called
        verifyStartActivityForResultCalled(testActivity);
    }

    /**
     * Verify exception is handled back if receiving error in the redirect.
     */
    @Test
    public void testGetTokenAuthCodeUrlContainError() throws IOException, InterruptedException {
        final Activity testActivity = Mockito.mock(Activity.class);
        Mockito.when(testActivity.getPackageName()).thenReturn(mAppContext.getPackageName());
        Mockito.when(testActivity.getApplicationContext()).thenReturn(mAppContext);

        // mock http call
        AndroidTestMockUtil.mockSuccessTenantDiscovery(getExpectedAuthorizeEndpoint(), getExpectedTokenEndpoint());
        mockSuccessHttpRequestCallWithNoRT();

        final BaseRequest request = createInteractiveRequest(AUTHORITY, testActivity);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(final AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(final MsalException exception) {
                assertTrue(exception instanceof MsalServiceException);
                assertTrue(MsalServiceException.ACCESS_DENIED.equals(exception.getErrorCode()));
                assertFalse(exception.getMessage().contains("other_error"));
                resultLock.countDown();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        // having the thread delayed for preTokenRequest to finish. Here we mock the
        // startActivityForResult, nothing actually happened when AuthenticationActivity is called.
        resultLock.await(THREAD_DELAY_TIME, TimeUnit.MILLISECONDS);

        final Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.AUTHORIZATION_FINAL_URL, mRedirectUri
                + "?error=access_denied&other_error=other_error");
        InteractiveRequest.onActivityResult(InteractiveRequest.BROWSER_FLOW,
                Constants.UIResponse.AUTH_CODE_COMPLETE, resultIntent);

        resultLock.await();

        // verify that startActivityResult is called
        verifyStartActivityForResultCalled(testActivity);
    }

    @Test
    public void testGetTokenAuthCodeUrlContainsErrorDescription() throws IOException, InterruptedException {
        new GetTokenAuthCodeUrlContainsErrorBaseTestCase() {
            @Override
            void makeAcquireTokenCall(final CountDownLatch countDownLatch, BaseRequest request) {
                request.getToken(new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        fail("Unexpected Success");
                    }

                    @Override
                    public void onError(MsalException exception) {
                        assertTrue(exception instanceof MsalServiceException);
                        assertTrue(MsalServiceException.ACCESS_DENIED.equals(exception.getErrorCode()));
                        assertTrue(exception.getMessage().contains("some_error_description"));
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onCancel() {
                        fail("Unexpected cancel");
                    }
                });
            }

            @Override
            String getFinalUrl() {
                return "?error=access_denied&error_description=some_error_description";
            }
        }.performTest();
    }

    @Test
    public void testStateInResponseAuthorityIsDifferent() throws IOException, InterruptedException {
        new GetTokenAuthCodeUrlContainsErrorBaseTestCase() {
            @Override
            void makeAcquireTokenCall(final CountDownLatch countDownLatch, BaseRequest request) {
                request.getToken(new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        fail("unexpected success");
                    }

                    @Override
                    public void onError(MsalException exception) {
                        assertTrue(exception instanceof MsalClientException);
                        assertTrue(MsalClientException.STATE_MISMATCH.equals(exception.getErrorCode()));
                        assertTrue(Constants.MsalErrorMessage.STATE_NOT_THE_SAME.equals(exception.getMessage()));
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onCancel() {
                        fail("unexpected failure");
                    }
                });
            }

            @Override
            String getFinalUrl() throws UnsupportedEncodingException {
                return "?code=1234&state=" + AndroidTestUtil.encodeProtocolState("https://someauthority.com", getScopes());
            }
        }.performTest();
    }

    @Test
    public void testStateInResponseNotContainAuthority() throws IOException, InterruptedException {
        new GetTokenAuthCodeUrlContainsErrorBaseTestCase() {
            @Override
            void makeAcquireTokenCall(final CountDownLatch countDownLatch, BaseRequest request) {
                request.getToken(new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        fail("unexpected success");
                    }

                    @Override
                    public void onError(MsalException exception) {
                        assertTrue(MsalClientException.STATE_MISMATCH.equals(exception.getErrorCode()));
                        assertTrue(Constants.MsalErrorMessage.STATE_NOT_THE_SAME.equals(exception.getMessage()));
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onCancel() {
                        fail("unexpected failure");
                    }
                });
            }

            @Override
            String getFinalUrl() throws UnsupportedEncodingException {
                return "?code=1234&state=" + Base64.encodeToString(MsalUtils.urlFormEncode(
                        MsalUtils.convertSetToString(getScopes(), " ")).getBytes("UTF-8"), Base64.NO_PADDING | Base64.URL_SAFE);
            }
        }.performTest();
    }

    @Test
    public void testStateNotInTheResponse() throws IOException, InterruptedException {
        new GetTokenAuthCodeUrlContainsErrorBaseTestCase() {
            @Override
            void makeAcquireTokenCall(final CountDownLatch countDownLatch, BaseRequest request) {
                request.getToken(new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        fail("unexpected success");
                    }

                    @Override
                    public void onError(MsalException exception) {
                        assertTrue(MsalClientException.STATE_MISMATCH.equals(exception.getErrorCode()));
                        assertTrue(exception.getMessage().contains(Constants.MsalErrorMessage.STATE_NOT_RETURNED));
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onCancel() {
                        fail("unexpected failure");
                    }
                });
            }

            @Override
            String getFinalUrl() throws UnsupportedEncodingException {
                return "?code=1234";
            }
        }.performTest();
    }

    private AuthenticationRequestParameters getAuthenticationParams(final String authorityString, final UiBehavior uiBehavior, final User user) {
        final Authority authority = Authority.createAuthority(authorityString, true);
        authority.mAuthorizationEndpoint = SilentRequestTest.AUTHORIZE_ENDPOINT;
        authority.mTokenEndpoint = SilentRequestTest.TOKEN_ENDPOINT;

        return AuthenticationRequestParameters.create(authority, new TokenCache(mAppContext), getScopes(),
                CLIENT_ID, mRedirectUri, LOGIN_HINT, "", uiBehavior, user, "", new RequestContext(CORRELATION_ID, "", Telemetry.generateNewRequestId()));
    }

    private AuthenticationRequestParameters getAuthRequestParameters(final String authority,
                                                                     final Set<String> scopes,
                                                                     final String redirectUri,
                                                                     final String loginHint,
                                                                     final UiBehavior uiBehavior,
                                                                     final User user) {
        return AuthenticationRequestParameters.create(Authority.createAuthority(authority, true), new TokenCache(mAppContext), scopes,
                CLIENT_ID, redirectUri, loginHint, "", uiBehavior, user, null, new RequestContext(CORRELATION_ID, "", Telemetry.generateNewRequestId()));
    }

    private AuthenticationRequestParameters getAuthRequestParameters(final String authorityString, final UiBehavior uiBehavior, final String extraQp,
                                                                     final String sliceParameter) {
        final Authority authority = Authority.createAuthority(authorityString, true);
        authority.mAuthorizationEndpoint = SilentRequestTest.AUTHORIZE_ENDPOINT;
        authority.mTokenEndpoint = SilentRequestTest.TOKEN_ENDPOINT;
        return AuthenticationRequestParameters.create(authority, new TokenCache(mAppContext), getScopes(),
                CLIENT_ID, mRedirectUri, LOGIN_HINT, extraQp, uiBehavior, null, sliceParameter, new RequestContext(CORRELATION_ID, "", Telemetry.generateNewRequestId()));
    }

    private Set<String> getScopes() {
        final String[] scopes = {"scope1", "scope2"};
        return new HashSet<>(Arrays.asList(scopes));
    }

    private Set<String> getExpectedScopes() {
        final Set<String> scopes = getScopes();
        scopes.addAll(new HashSet<>(Arrays.asList(OauthConstants.Oauth2Value.RESERVED_SCOPES)));

        return scopes;
    }

    private Set<String> getScopesContainsReservedScope() {
        final Set<String> scopes = new HashSet<>();
        scopes.add(CLIENT_ID);
        scopes.add("scope");

        return scopes;
    }

    private void verifyCommonQueryString(final Map<String, String> queryStrings) {
        assertTrue(CLIENT_ID.equals(queryStrings.get(OauthConstants.Oauth2Parameters.CLIENT_ID)));
        assertTrue(mRedirectUri.equals(queryStrings.get(OauthConstants.Oauth2Parameters.REDIRECT_URI)));
        assertTrue(OauthConstants.Oauth2ResponseType.CODE.equals(queryStrings.get(
                OauthConstants.Oauth2Parameters.RESPONSE_TYPE)));
        assertTrue(CORRELATION_ID.toString().equals(queryStrings.get(OauthConstants.OauthHeader.CORRELATION_ID)));
        assertTrue(LOGIN_HINT.equals(queryStrings.get(OauthConstants.Oauth2Parameters.LOGIN_HINT)));
    }

    private String getExpectedAuthorizeEndpoint() {
        return AUTHORITY + Authority.DEFAULT_AUTHORIZE_ENDPOINT;
    }

    private String getExpectedTokenEndpoint() {
        return AUTHORITY + Authority.DEFAULT_AUTHORIZE_ENDPOINT;
    }

    static void mockSuccessHttpRequestCallWithNoRT() throws IOException {
        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponseWithNoRefreshToken(AndroidTestUtil.TEST_IDTOKEN));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
    }

    private BaseRequest createInteractiveRequest(final String authority, final Activity testActivity) {
        return new InteractiveRequest(testActivity, getAuthenticationParams(authority,
                UiBehavior.FORCE_LOGIN, null), null);
    }

    private void verifyStartActivityForResultCalled(final Activity testActivity) {
        Mockito.verify(testActivity).startActivityForResult(Mockito.argThat(new ArgumentMatcher<Intent>() {
            @Override
            public boolean matches(Intent argument) {
                return argument.getStringExtra(Constants.REQUEST_URL_KEY) != null;
            }
        }), Mockito.eq(InteractiveRequest.BROWSER_FLOW));
    }

    private void resolveAuthenticationActivity(final Context mockedContext, boolean resolvable) {
        final PackageManager mockedPackageManager = mockedContext.getPackageManager();
        final ResolveInfo resolveInfo = resolvable ? Mockito.mock(ResolveInfo.class) : null;

        Mockito.when(mockedPackageManager.resolveActivity(Matchers.any(Intent.class), Matchers.anyInt())).thenReturn(resolveInfo);
    }

    static void mockNetworkConnected(final Context mockedContext, boolean isConnected) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) mockedContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo mockedNetworkInfo = mock(NetworkInfo.class);
        Mockito.when(mockedNetworkInfo.isConnected()).thenReturn(isConnected);
        Mockito.when(connectivityManager.getActiveNetworkInfo()).thenReturn(mockedNetworkInfo);
    }

    /**
     * Base class for verify if the redirect url from authorize endpoint contains error code.
     */
    abstract class GetTokenAuthCodeUrlContainsErrorBaseTestCase {

        abstract void makeAcquireTokenCall(final CountDownLatch countDownLatch, final BaseRequest request);

        abstract String getFinalUrl() throws UnsupportedEncodingException;

        final void performTest() throws IOException, InterruptedException {
            final Activity testActivity = Mockito.mock(Activity.class);
            Mockito.when(testActivity.getPackageName()).thenReturn(mAppContext.getPackageName());
            Mockito.when(testActivity.getApplicationContext()).thenReturn(mAppContext);

            // mock http call
            AndroidTestMockUtil.mockSuccessTenantDiscovery(getExpectedAuthorizeEndpoint(), getExpectedTokenEndpoint());
            mockSuccessHttpRequestCallWithNoRT();

            final BaseRequest request = createInteractiveRequest(AUTHORITY, testActivity);
            final CountDownLatch resultLock = new CountDownLatch(1);
            makeAcquireTokenCall(resultLock, request);

            // having the thread delayed for preTokenRequest to finish. Here we mock the
            // startActivityForResult, nothing actually happened when AuthenticationActivity is called.
            resultLock.await(THREAD_DELAY_TIME, TimeUnit.MILLISECONDS);

            final Intent resultIntent = new Intent();
            resultIntent.putExtra(Constants.AUTHORIZATION_FINAL_URL, mRedirectUri
                    + getFinalUrl());
            InteractiveRequest.onActivityResult(InteractiveRequest.BROWSER_FLOW,
                    Constants.UIResponse.AUTH_CODE_COMPLETE, resultIntent);

            resultLock.await();

            // verify that startActivityResult is called
            verifyStartActivityForResultCalled(testActivity);
        }
    }

    static class MockContext extends ContextWrapper {
        private PackageManager mPackageManager;
        private boolean mIsConnectionAvailable = true;
        private ConnectivityManager mConnectivityManager;

        MockContext(final Context context) {
            super(context);
        }

        @Override
        public PackageManager getPackageManager() {
            if (mPackageManager == null) {
                mPackageManager = Mockito.mock(PackageManager.class);
            }

            return mPackageManager;
        }

        @Override
        public Object getSystemService(String name) {
            if (Context.CONNECTIVITY_SERVICE.equals(name)) {
                if (mConnectivityManager == null) {
                    mConnectivityManager = Mockito.mock(ConnectivityManager.class);
                }

                return mConnectivityManager;
            }

            return super.getSystemService(name);
        }

        public void setConnectionAvailable(boolean isConnectionAvaliable) {
            mIsConnectionAvailable = isConnectionAvaliable;
        }
    }
}
