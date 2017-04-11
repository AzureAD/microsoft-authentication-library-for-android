package com.microsoft.identity.client;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;

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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link PublicClientApplication}.
 */
@RunWith(AndroidJUnit4.class)
public final class PublicClientApplicationTest extends AndroidTestCase {
    private Context mAppContext;
    private String mRedirectUri;
    private TokenCache mTokenCache;
    private static final String CLIENT_ID = "client-id";
    private static final String DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common";
    private static final String ALTERNATE_AUTHORITY = "https://login.microsoftonline.com/alternateAuthority";
    private static final String[] SCOPE = {"scope1", "scope2"};
    private static final int EXPECTED_USER_SIZE = 3;
    private static final String DEFAULT_TOKEN_ENDPOINT = "/oauth2/v2.0/token";
    private static final String DEFAULT_CLIENT_INFO = AndroidTestUtil.createRawClientInfo(AndroidTestUtil.UID, AndroidTestUtil.UTID);

    @Before
    public void setUp() throws Exception {
        super.setUp();
        InstrumentationRegistry.getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache",
                InstrumentationRegistry.getContext().getCacheDir().getPath());
        Authority.VALIDATED_AUTHORITY.clear();

        mAppContext = InstrumentationRegistry.getContext().getApplicationContext();
        mRedirectUri = "msauth-client-id://" + mAppContext.getPackageName();
        mTokenCache = new TokenCache(mAppContext);
        Telemetry.disableForTest(true);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
        AndroidTestUtil.removeAllTokens(mAppContext);
        Telemetry.disableForTest(false);
    }

    /**
     * Verify correct exception is thrown if activity is not provided.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testActivityNull() {
        new PublicClientApplication(null);
    }

    /**
     * Verify correct exception is thrown if client id is not set in the manifest.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testClientIdNotInManifest() throws PackageManager.NameNotFoundException {
        final ApplicationInfo applicationInfo = Mockito.mock(ApplicationInfo.class);
        // meta data is empty, no client id there.
        applicationInfo.metaData = new Bundle();

        final Context context = new MockContext(mAppContext);
        final PackageManager mockedPackageManager = context.getPackageManager();
        Mockito.when(mockedPackageManager.getApplicationInfo(
                Mockito.refEq(mAppContext.getPackageName()), Mockito.eq(
                        PackageManager.GET_META_DATA))).thenReturn(applicationInfo);

        new PublicClientApplication(context);
    }

    /**
     * Verify correct exception is thrown if cannot retrieve package info.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testApplicationInfoIsNull() throws PackageManager.NameNotFoundException {
        final Context context = new MockContext(mAppContext);
        final PackageManager mockedPackageManager = context.getPackageManager();
        Mockito.when(mockedPackageManager.getApplicationInfo(
                Mockito.refEq(mAppContext.getPackageName()), Mockito.eq(
                        PackageManager.GET_META_DATA))).thenReturn(null);

        new PublicClientApplication(context);
    }

    /**
     * Verify correct exception is thrown if {@link BrowserTabActivity} does not have the correct intent-filer.
     */
    @Test(expected = IllegalStateException.class)
    public void testNoCustomTabSchemeConfigured() throws PackageManager.NameNotFoundException {
        final Context context = new MockContext(mAppContext);
        mockPackageManagerWithClientId(context, null, CLIENT_ID);

        new PublicClientApplication(context);
    }

    /**
     * Verify correct exception is thrown if meta-data is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMetaDataIsNull() throws PackageManager.NameNotFoundException {
        final ApplicationInfo applicationInfo = Mockito.mock(ApplicationInfo.class);
        // null metadata
        applicationInfo.metaData = null;

        final Context context = new MockContext(mAppContext);
        final PackageManager mockedPackageManager = context.getPackageManager();
        Mockito.when(mockedPackageManager.getApplicationInfo(
                Mockito.refEq(mAppContext.getPackageName()), Mockito.eq(
                        PackageManager.GET_META_DATA))).thenReturn(applicationInfo);

        new PublicClientApplication(context);
    }

    /**
     * Verify correct exception is thrown if callback is not provided.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCallBackEmpty() throws PackageManager.NameNotFoundException {
        final Context context = new MockContext(mAppContext);
        mockPackageManagerWithClientId(context, null, CLIENT_ID);
        mockHasCustomTabRedirect(context);

        final PublicClientApplication application = new PublicClientApplication(context);
        application.acquireToken(getActivity(context), SCOPE, null);
    }

    @Test(expected = IllegalStateException.class)
    public void testInternetPermissionMissing() throws PackageManager.NameNotFoundException {
        final Context context = new MockContext(mAppContext);
        final PackageManager packageManager = context.getPackageManager();
        mockPackageManagerWithClientId(context, null, CLIENT_ID);
        mockHasCustomTabRedirect(context);
        Mockito.when(packageManager.checkPermission(Mockito.refEq("android.permission.INTERNET"),
                Mockito.refEq(mAppContext.getPackageName()))).thenReturn(PackageManager.PERMISSION_DENIED);

        new PublicClientApplication(context);
    }

    /**
     * Verify that users are correctly retrieved.
     */
    @Test
    public void testGetUsers() throws MsalException, PackageManager.NameNotFoundException {
        final PublicClientApplication application = new PublicClientApplication(getMockedContext(CLIENT_ID));
        assertTrue(application.getUsers().size() == 0);
        // prepare token cache
        // save token with Displayable as: Displayable1 UniqueId: UniqueId1 HomeObjectId: homeOID
        final String displayable1 = "Displayable1";
        final String uniqueId1 = "UniqueId1";
        final String homeOid1 = "HomeOid1";
        final String uTid1 = "uTid1";
        String idToken = getIdToken(displayable1, uniqueId1, homeOid1);
        String clientInfo = AndroidTestUtil.createRawClientInfo(uniqueId1, uTid1);

        saveTokenResponse(mTokenCache, TokenCacheTest.AUTHORITY, CLIENT_ID, getTokenResponse(idToken, clientInfo));

        // prepare token cache for same client id, same displayable, uniqueId but different oid
        final String homeOid2 = "HomeOid2";
        final String uid2 = "uniqueId2";
        final String utid2 = "uTid2";
        idToken = getIdToken(displayable1, uniqueId1, homeOid2);
        clientInfo = AndroidTestUtil.createRawClientInfo(uid2, utid2);
        saveTokenResponse(mTokenCache, TokenCacheTest.AUTHORITY, CLIENT_ID, getTokenResponse(idToken, clientInfo));

        List<User> users = application.getUsers();
        assertTrue(users.size() == 2);

        // prepare token cache for same client id, different diplayable, uniqueid and oid
        final String displayable3 = "Displayable3";
        final String uniqueId3 = "UniqueId3";
        final String homeOid3 = "HomeOid3";
        final String uTid3 = "uTid3";
        idToken = getIdToken(displayable3, uniqueId3, homeOid3);
        clientInfo = AndroidTestUtil.createRawClientInfo(uniqueId3, uTid3);
        saveTokenResponse(mTokenCache, TokenCacheTest.AUTHORITY, CLIENT_ID, getTokenResponse(idToken, clientInfo));

        users = application.getUsers();
        assertTrue(users.size() == EXPECTED_USER_SIZE);
        final User userForDisplayable3 = application.getUser(MSALUtils.getUniqueUserIdentifier(uniqueId3, uTid3));
        assertNotNull(userForDisplayable3);
        assertTrue(userForDisplayable3.getDisplayableId().equals(displayable3));
        assertTrue(userForDisplayable3.getUserIdentifier().equals(MSALUtils.getUniqueUserIdentifier(uniqueId3, uTid3)));

        // prepare token cache for different client id, same displayable3 user
        final String anotherClientId = "anotherClientId";
        saveTokenResponse(mTokenCache, TokenCacheTest.AUTHORITY, anotherClientId, getTokenResponse(idToken, clientInfo));
        final PublicClientApplication anotherApplication = new PublicClientApplication(getMockedContext(anotherClientId));
        assertTrue(application.getUsers().size() == EXPECTED_USER_SIZE);
        users = anotherApplication.getUsers();
        assertTrue(users.size() == 1);
        final User userForAnotherClient = application.getUser(MSALUtils.getUniqueUserIdentifier(uniqueId3, uTid3));
        assertNotNull(userForAnotherClient);
        assertTrue(userForAnotherClient.getDisplayableId().equals(displayable3));
        assertTrue(userForAnotherClient.getUserIdentifier().equals(MSALUtils.getUniqueUserIdentifier(uniqueId3, uTid3)));
    }

    /**
     * Verify {@link PublicClientApplication#acquireToken(Activity, String[], AuthenticationCallback)}.
     * AcquireToken interactive call ask token for scope1 and scope2.
     * AcquireTokenSilent call ask token for scope3. No access token will be found. Refresh token returned in the interactive
     * request will be used for silent request. Since no intersection between {scope1, scope2} and {scope3}, there will be
     * two access token entries in the cache.
     */
    @Test
    public void testAcquireTokenSuccess() throws PackageManager.NameNotFoundException, IOException,
            InterruptedException {
        new GetTokenBaseTestCase() {
            private User mUser;

            @Override
            void mockHttpRequest() throws IOException {
                mockSuccessResponse(convertScopesArrayToString(SCOPE), AndroidTestUtil.ACCESS_TOKEN, DEFAULT_CLIENT_INFO);
            }

            @Override
            void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(activity, SCOPE, new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        Assert.assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                        mUser = authenticationResult.getUser();

                        releaseLock.countDown();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        fail("Unexpected Error");
                    }

                    @Override
                    public void onCancel() {
                        fail("Unexpected Cancel");
                    }
                });
            }

            @Override
            String getFinalAuthUrl() throws UnsupportedEncodingException {
                return mRedirectUri + "?code=1234&state=" + AndroidTestUtil.encodeProtocolState(
                        DEFAULT_AUTHORITY, new HashSet<>(Arrays.asList(SCOPE)));
            }

            @Override
            protected void makeSilentRequest(final PublicClientApplication application, final CountDownLatch silentResultLock)
                    throws IOException, InterruptedException {
                final String scopeForSilent = "scope3";
                mockSuccessResponse(scopeForSilent, AndroidTestUtil.ACCESS_TOKEN, DEFAULT_CLIENT_INFO);

                application.acquireTokenSilentAsync(new String[]{scopeForSilent}, mUser, new AuthenticationCallback() {

                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        assertTrue(authenticationResult.getAccessToken().equals(AndroidTestUtil.ACCESS_TOKEN));
                        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 2);
                        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);
                        silentResultLock.countDown();
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
            }
        }.performTest();
    }

    @Test
    public void testClientInfoNotReturned() throws PackageManager.NameNotFoundException, IOException,
            InterruptedException {
        new GetTokenBaseTestCase() {

            @Override
            void mockHttpRequest() throws IOException {
                final String idToken = TokenCacheTest.getDefaultIdToken();
                final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_OK, AndroidTestUtil.getSuccessResponse(idToken, AndroidTestUtil.ACCESS_TOKEN, AndroidTestUtil.REFRESH_TOKEN, ""));
                Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
                HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
            }

            @Override
            void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(activity, SCOPE, new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        Assert.assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                        final User user = authenticationResult.getUser();
                        Assert.assertTrue(user.getUid().equals(""));
                        Assert.assertTrue(user.getUtid().equals(""));
                        Assert.assertTrue(user.getDisplayableId().equals(TokenCacheTest.DISPLAYABLE));

                        releaseLock.countDown();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        fail("Unexpected Error");
                    }

                    @Override
                    public void onCancel() {
                        fail("Unexpected Cancel");
                    }
                });
            }

            @Override
            String getFinalAuthUrl() throws UnsupportedEncodingException {
                return mRedirectUri + "?code=1234&state=" + AndroidTestUtil.encodeProtocolState(
                        DEFAULT_AUTHORITY, new HashSet<>(Arrays.asList(SCOPE)));
            }
        }.performTest();
    }

    public void testAcquireTokenSilentNoAuthorityProvidedMultipleInTheCache() throws PackageManager.NameNotFoundException, IOException,
            InterruptedException {
        new GetTokenBaseTestCase() {
            private User mUser;

            @Override
            void mockHttpRequest() throws IOException {
                mockSuccessResponse(convertScopesArrayToString(SCOPE), AndroidTestUtil.ACCESS_TOKEN, DEFAULT_CLIENT_INFO);
            }

            @Override
            void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(activity, SCOPE, new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        Assert.assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                        mUser = authenticationResult.getUser();

                        releaseLock.countDown();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        fail("Unexpected Error");
                    }

                    @Override
                    public void onCancel() {
                        fail("Unexpected Cancel");
                    }
                });
            }

            @Override
            String getFinalAuthUrl() throws UnsupportedEncodingException {
                return mRedirectUri + "?code=1234&state=" + AndroidTestUtil.encodeProtocolState(
                        DEFAULT_AUTHORITY, new HashSet<>(Arrays.asList(SCOPE)));
            }

            @Override
            protected void makeSilentRequest(final PublicClientApplication application, final CountDownLatch silentResultLock)
                    throws IOException, InterruptedException {
                AndroidTestMockUtil.mockSuccessTenantDiscovery(SilentRequestTest.AUTHORIZE_ENDPOINT, SilentRequestTest.TOKEN_ENDPOINT);
                mockSuccessResponse(convertScopesArrayToString(SCOPE), AndroidTestUtil.ACCESS_TOKEN, DEFAULT_CLIENT_INFO);
                mockSuccessResponse(convertScopesArrayToString(SCOPE), "new access token", DEFAULT_CLIENT_INFO);

                final String anotherAuthority = "https://login.microsoftonline.com/tenant123";
                final CountDownLatch resultLock = new CountDownLatch(1);
                application.acquireTokenSilentAsync(SCOPE, mUser, anotherAuthority, true, new AuthenticationCallback() {

                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        assertTrue(authenticationResult.getAccessToken().equals(AndroidTestUtil.ACCESS_TOKEN));
                        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 2);
                        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);
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

                resultLock.await();

                // make another silent request with no authoirty
                application.acquireTokenSilentAsync(SCOPE, mUser, new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        fail();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        assertTrue(exception instanceof MsalClientException);
                        assertTrue(exception.getErrorCode().equals(MSALError.MULTIPLE_MATCHING_TOKENS_DETECTED));

                        assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 1);
                        silentResultLock.countDown();
                    }

                    @Override
                    public void onCancel() {
                        fail();
                    }
                });
            }
        }.performTest();
    }

    /**
     * Verify {@link PublicClientApplication#acquireToken(Activity, String[], String, UIBehavior, String, String[],
     * String, AuthenticationCallback)}. Also check if authority is set on the manifest, we read the authority
     * from manifest meta-data.
     */
    @Test
    public void testAuthoritySetInManifestGetTokenFailed()
            throws PackageManager.NameNotFoundException, IOException, InterruptedException {
        new GetTokenBaseTestCase() {

            @Override
            protected String getAlternateAuthorityInManifest() {
                return ALTERNATE_AUTHORITY;
            }

            @Override
            void mockHttpRequest() throws IOException {
                final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_BAD_REQUEST, AndroidTestUtil.getErrorResponseMessage("invalid_request"));
                Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
                HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
            }

            @Override
            void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(activity, SCOPE, "somehint", new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        fail();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        assertTrue(exception instanceof MsalServiceException);

                        final MsalServiceException serviceException = (MsalServiceException) exception;
                        assertTrue(MSALError.INVALID_REQUEST.equals(serviceException.getErrorCode()));
                        assertTrue(!serviceException.getMessage().isEmpty());
                        assertTrue(serviceException.getHttpStatusCode() == HttpURLConnection.HTTP_BAD_REQUEST);
                        releaseLock.countDown();
                    }

                    @Override
                    public void onCancel() {
                        fail();
                    }
                });
            }

            @Override
            String getFinalAuthUrl() throws UnsupportedEncodingException {
                return mRedirectUri + "?code=1234&state=" + AndroidTestUtil.encodeProtocolState(
                        ALTERNATE_AUTHORITY, new HashSet<>(Arrays.asList(SCOPE)));
            }

            @Override
            protected void performAdditionalVerify(Activity testActivity) {
                Mockito.verify(testActivity).startActivityForResult(Mockito.argThat(
                        new ArgumentMatcher<Intent>() {
                            @Override
                            public boolean matches(Object argument) {
                                final String data = ((Intent) argument).getStringExtra(Constants.REQUEST_URL_KEY);
                                return data.startsWith(ALTERNATE_AUTHORITY);
                            }
                        }), Matchers.eq(InteractiveRequest.BROWSER_FLOW));
            }
        }.performTest();
    }

    /**
     * Verify {@link PublicClientApplication#acquireToken(Activity, String[], String, UIBehavior, String, String[], String, AuthenticationCallback)}.
     */
    // TODO: suppress the test. The purpose is that the API call will eventually send back the cancel to caller.
    @Ignore
    @Test
    public void testGetTokenWithExtraQueryParam()
            throws PackageManager.NameNotFoundException, IOException, InterruptedException {
        new GetTokenBaseTestCase() {

            @Override
            void mockHttpRequest() throws IOException {
                // do nothing. The intention for this test is the callback receives cancel if returned url contains
                // cancel error.
            }

            @Override
            void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(activity, SCOPE, "somehint", UIBehavior.FORCE_LOGIN, "extra=param",
                        null, null, new AuthenticationCallback() {
                            @Override
                            public void onSuccess(AuthenticationResult authenticationResult) {
                                fail("unexpected success result");
                            }

                            @Override
                            public void onError(MsalException exception) {
                                fail("Unexpected Error");
                            }

                            @Override
                            public void onCancel() {
                                releaseLock.countDown();
                            }
                        });
            }

            @Override
            String getFinalAuthUrl() throws UnsupportedEncodingException {
                return mRedirectUri + "?error=access_denied&error_subcode=cancel";
            }

            @Override
            protected void performAdditionalVerify(Activity testActivity) {
                Mockito.verify(testActivity).startActivityForResult(Mockito.argThat(new ArgumentMatcher<Intent>() {
                    @Override
                    public boolean matches(Object argument) {
                        if (((Intent) argument).getStringExtra(Constants.REQUEST_URL_KEY) != null) {
                            return true;
                        }

                        return false;
                    }
                }), Mockito.eq(InteractiveRequest.BROWSER_FLOW));
            }
        }.performTest();
    }

    @Test
    public void testB2cAuthorityNotInTrustedList() throws PackageManager.NameNotFoundException, IOException, InterruptedException {
        final String unsupportedB2cAuthority = "https://somehost/tfp/sometenant/somepolicy";

        new GetTokenBaseTestCase() {
            @Override
            protected String getAlternateAuthorityInManifest() {
                return unsupportedB2cAuthority;
            }

            @Override
            void mockHttpRequest() throws IOException {
                // do nothing. The intention for this test is the callback receives cancel if returned url contains
                // cancel error.
            }

            @Override
            void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(activity, SCOPE, "somehint", UIBehavior.FORCE_LOGIN, "extra=param",
                        null, null, new AuthenticationCallback() {
                            @Override
                            public void onSuccess(AuthenticationResult authenticationResult) {
                                fail("unexpected success result");
                            }

                            @Override
                            public void onError(MsalException exception) {
                                try {
                                    assertTrue(exception instanceof MsalClientException);
                                    assertTrue(exception.getErrorCode().equals(MSALError.AUTHORITY_VALIDATION_NOT_SUPPORTED));
                                } finally {
                                    releaseLock.countDown();
                                }
                            }

                            @Override
                            public void onCancel() {
                                fail("unexpected cancel");
                            }
                        });
            }

            @Override
            String getFinalAuthUrl() throws UnsupportedEncodingException {
                return "";
            }
        }.performTest();
    }

    /**
     * Verify {@link PublicClientApplication#acquireToken(Activity, String[], String, UIBehavior, String, String[], String, AuthenticationCallback)}.
     * AcquireToken asks token for {scope1, scope2}.
     * AcquireTokenSilent asks for {scope2}. Since forcePrompt is set for the silent request, RT request will be sent. There is
     * intersection, old entry will be removed. There will be only one access token left.
     */
    @Test
    public void testGetTokenWithScopeIntersection() throws PackageManager.NameNotFoundException, IOException,
            InterruptedException {
        new GetTokenBaseTestCase() {
            private User mUser;

            @Override
            void mockHttpRequest() throws IOException {
                mockSuccessResponse(convertScopesArrayToString(SCOPE), AndroidTestUtil.ACCESS_TOKEN, DEFAULT_CLIENT_INFO);
            }

            @Override
            void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(activity, SCOPE, "", UIBehavior.FORCE_LOGIN, null, null, null,
                        new AuthenticationCallback() {
                            @Override
                            public void onSuccess(AuthenticationResult authenticationResult) {
                                Assert.assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                                mUser = authenticationResult.getUser();
                                releaseLock.countDown();
                            }

                            @Override
                            public void onError(MsalException exception) {
                                fail("Unexpected Error");
                            }

                            @Override
                            public void onCancel() {
                                fail("Unexpected Cancel");
                            }
                        });
            }

            @Override
            String getFinalAuthUrl() throws UnsupportedEncodingException {
                return mRedirectUri + "?code=1234&state=" + AndroidTestUtil.encodeProtocolState(
                        DEFAULT_AUTHORITY, new HashSet<>(Arrays.asList(SCOPE)));
            }

            @Override
            protected void makeSilentRequest(final PublicClientApplication application, final CountDownLatch silentResultLock)
                    throws IOException, InterruptedException {
                final String silentRequestScope = "scope2";
                final String newAccessToken = "some new access token";
                AndroidTestMockUtil.mockSuccessTenantDiscovery(SilentRequestTest.AUTHORIZE_ENDPOINT, SilentRequestTest.TOKEN_ENDPOINT);
                mockSuccessResponse(silentRequestScope, newAccessToken, DEFAULT_CLIENT_INFO);

                application.acquireTokenSilentAsync(new String[]{silentRequestScope}, mUser, null, true, new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        assertTrue(authenticationResult.getAccessToken().equals(newAccessToken));

                        final List<AccessTokenCacheItem> accessTokenItems = AndroidTestUtil.getAllAccessTokens(mAppContext);
                        assertTrue(accessTokenItems.size() == 1);
                        assertTrue(accessTokenItems.get(0).getAccessToken().equals(newAccessToken));
                        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);
                        silentResultLock.countDown();
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
            }
        }.performTest();
    }

    @Test
    public void testSilentRequestFailure() throws MsalException, InterruptedException, IOException {
        final PublicClientApplication application = new PublicClientApplication(mAppContext);

        // prepare token in the cache
        final String rawClientInfo = AndroidTestUtil.createRawClientInfo(AndroidTestUtil.UID, AndroidTestUtil.UTID);
        saveTokenResponse(mTokenCache, AndroidTestUtil.DEFAULT_AUTHORITY_WITH_TENANT, CLIENT_ID, TokenCacheTest.getTokenResponseForDefaultUser(
                AndroidTestUtil.ACCESS_TOKEN, AndroidTestUtil.REFRESH_TOKEN, "scope1 scope2", AndroidTestUtil.getExpiredDate(), rawClientInfo));

        final IdToken idToken = new IdToken(AndroidTestUtil.getRawIdToken("another Displayable", "another uniqueId", "another homeobj"));
        final ClientInfo clientInfoForDifferentUser = new ClientInfo(AndroidTestUtil.createRawClientInfo("another uid", "another utid"));
        final User user = User.create(idToken, clientInfoForDifferentUser);

        AndroidTestMockUtil.mockSuccessTenantDiscovery(SilentRequestTest.AUTHORIZE_ENDPOINT, SilentRequestTest.TOKEN_ENDPOINT);
        final CountDownLatch silentLock = new CountDownLatch(1);
        application.acquireTokenSilentAsync(new String[]{"scope1", "scope2"}, user, new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(MsalException exception) {
                assertTrue(exception instanceof MsalUiRequiredException);
                assertTrue(exception.getErrorCode().equals(MSALError.NO_TOKENS_FOUND));
                assertNull(exception.getCause());
                silentLock.countDown();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });
        silentLock.await();
    }

    @Test
    public void testTurnOffAuthorityValidation() throws MsalException, IOException, InterruptedException {
        final String testAuthority = "https://someauthority.test.com/sometenant";
        final PublicClientApplication application = new PublicClientApplication(mAppContext, CLIENT_ID, testAuthority);
        application.setValidateAuthority(false);

        //mock tenant discovery response
        // prepare token in the cache
        final String rawClientInfo = AndroidTestUtil.createRawClientInfo(AndroidTestUtil.UID, AndroidTestUtil.UTID);
        saveTokenResponse(mTokenCache, testAuthority, CLIENT_ID, TokenCacheTest.getTokenResponseForDefaultUser(
                AndroidTestUtil.ACCESS_TOKEN, AndroidTestUtil.REFRESH_TOKEN, "scope1 scope2", AndroidTestUtil.getExpiredDate(), rawClientInfo));


        final User user = User.create(new IdToken(TokenCacheTest.getDefaultIdToken()), new ClientInfo(rawClientInfo));
        AndroidTestMockUtil.mockSuccessTenantDiscovery(SilentRequestTest.AUTHORIZE_ENDPOINT, SilentRequestTest.TOKEN_ENDPOINT);

        final String accessToken = "access token from token refresh";
        mockSuccessResponse("scope1 scope2", accessToken, DEFAULT_CLIENT_INFO);
        final CountDownLatch silentLock = new CountDownLatch(1);
        application.acquireTokenSilentAsync(new String[]{"scope1", "scope2"}, user, null, true, new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                assertTrue(authenticationResult.getAccessToken().equals(accessToken));
                silentLock.countDown();
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

        silentLock.await();
    }

    public void testAcquireTokenWithUserSucceed() throws PackageManager.NameNotFoundException, InterruptedException, IOException {
        new GetTokenBaseTestCase() {
            private User mUser;
            private String clientInfo = AndroidTestUtil.createRawClientInfo(AndroidTestUtil.UID, AndroidTestUtil.UTID);

            @Override
            void mockHttpRequest() throws IOException {
                mUser = new User(AndroidTestUtil.PREFERRED_USERNAME, AndroidTestUtil.NAME, AndroidTestUtil.ISSUER, AndroidTestUtil.UID, AndroidTestUtil.UTID);
                mockSuccessResponse(convertScopesArrayToString(SCOPE), AndroidTestUtil.ACCESS_TOKEN, clientInfo);
            }

            @Override
            void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(activity, SCOPE, mUser, UIBehavior.SELECT_ACCOUNT, null, new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        Assert.assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                        final User user = authenticationResult.getUser();
                        assertTrue(AndroidTestUtil.UID.equals(user.getUid()));
                        assertTrue(AndroidTestUtil.UTID.equals(user.getUtid()));

                        releaseLock.countDown();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        fail("Unexpected Error");
                    }

                    @Override
                    public void onCancel() {
                        fail("Unexpected Cancel");
                    }
                });
            }

            @Override
            String getFinalAuthUrl() throws UnsupportedEncodingException {
                return mRedirectUri + "?code=1234&state=" + AndroidTestUtil.encodeProtocolState(
                        DEFAULT_AUTHORITY, new HashSet<>(Arrays.asList(SCOPE)));
            }

            @Override
            protected void makeSilentRequest(final PublicClientApplication application, final CountDownLatch silentResultLock)
                    throws IOException, InterruptedException {
                final String scopeForSilent = "scope3";
                AndroidTestMockUtil.mockSuccessTenantDiscovery(SilentRequestTest.AUTHORIZE_ENDPOINT, SilentRequestTest.TOKEN_ENDPOINT);
                mockSuccessResponse(scopeForSilent, AndroidTestUtil.ACCESS_TOKEN, clientInfo);

                application.acquireTokenSilentAsync(new String[]{scopeForSilent}, mUser, new AuthenticationCallback() {

                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        assertTrue(authenticationResult.getAccessToken().equals(AndroidTestUtil.ACCESS_TOKEN));
                        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 2);
                        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);
                        silentResultLock.countDown();
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
            }
        }.performTest();
    }

    @Test
    public void testAcquireTokenUserMismatch() throws PackageManager.NameNotFoundException, InterruptedException, IOException {
        new GetTokenBaseTestCase() {
            private User mUser = new User(AndroidTestUtil.PREFERRED_USERNAME, AndroidTestUtil.NAME, AndroidTestUtil.ISSUER, AndroidTestUtil.UID, AndroidTestUtil.UTID);
            private String mRawClientInfo = AndroidTestUtil.createRawClientInfo("different-uid", "different-utid");

            @Override
            void mockHttpRequest() throws IOException {
                final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_OK, AndroidTestUtil.getSuccessResponse(AndroidTestUtil.TEST_IDTOKEN, AndroidTestUtil.ACCESS_TOKEN,
                                convertScopesArrayToString(SCOPE), mRawClientInfo));
                Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
                HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
            }

            @Override
            void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(activity, SCOPE, mUser, UIBehavior.SELECT_ACCOUNT, null, null, null, new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        fail();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        assertTrue(exception instanceof MsalClientException);
                        assertTrue(exception.getErrorCode().equals(MSALError.USER_MISMATCH));

                        // verify that no token is stored.
                        Assert.assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 0);
                        Assert.assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 0);
                        releaseLock.countDown();
                    }

                    @Override
                    public void onCancel() {
                        fail("Unexpected Cancel");
                    }
                });
            }

            @Override
            String getFinalAuthUrl() throws UnsupportedEncodingException {
                return mRedirectUri + "?code=1234&state=" + AndroidTestUtil.encodeProtocolState(
                        DEFAULT_AUTHORITY, new HashSet<>(Arrays.asList(SCOPE)));
            }
        }.performTest();
    }

    @Test
    public void testAcquireTokenWithUserFailed() throws PackageManager.NameNotFoundException, InterruptedException, IOException {
        new GetTokenBaseTestCase() {
            private User mUser = new User(AndroidTestUtil.PREFERRED_USERNAME, AndroidTestUtil.NAME, AndroidTestUtil.ISSUER, AndroidTestUtil.UID, AndroidTestUtil.UTID);

            @Override
            void mockHttpRequest() throws IOException {
                final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_BAD_REQUEST, AndroidTestUtil.getErrorResponseMessage("invalid_request"));
                Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
                HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
            }

            @Override
            void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(activity, SCOPE, mUser, UIBehavior.SELECT_ACCOUNT, null, null, null, new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        fail();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        assertTrue(exception instanceof MsalServiceException);
                        assertTrue(exception.getErrorCode().equals(MSALError.INVALID_REQUEST));
                        releaseLock.countDown();
                    }

                    @Override
                    public void onCancel() {
                        fail("Unexpected Cancel");
                    }
                });
            }

            @Override
            String getFinalAuthUrl() throws UnsupportedEncodingException {
                return mRedirectUri + "?code=1234&state=" + AndroidTestUtil.encodeProtocolState(
                        DEFAULT_AUTHORITY, new HashSet<>(Arrays.asList(SCOPE)));
            }
        }.performTest();

    }

    static String getIdToken(final String displayable, final String uniqueId, final String homeOid) {
        return AndroidTestUtil.createIdToken(AndroidTestUtil.AUDIENCE, AndroidTestUtil.ISSUER, AndroidTestUtil.NAME, uniqueId, displayable,
                AndroidTestUtil.SUBJECT, AndroidTestUtil.TENANT_ID, AndroidTestUtil.VERSION);
    }

    private TokenResponse getTokenResponse(final String idToken, final String clientInfo) throws MsalException {
        return new TokenResponse(AndroidTestUtil.ACCESS_TOKEN, idToken, AndroidTestUtil.REFRESH_TOKEN, new Date(), new Date(),
                new Date(), "scope", "Bearer", clientInfo);
    }

    static void saveTokenResponse(final TokenCache tokenCache, final String authority, final String clientId,
                                  final TokenResponse response) throws MsalException {
        tokenCache.saveAccessToken(authority, clientId, response, AndroidTestUtil.getTestRequestContext());

        try {
            final URL authorityUrl = new URL(authority);
            tokenCache.saveRefreshToken(authorityUrl.getHost(), clientId, response, AndroidTestUtil.getTestRequestContext());
        } catch (MalformedURLException e) {
            throw new MsalClientException(MSALError.MALFORMED_URL, "unable to create url");
        }
    }

    private void mockPackageManagerWithClientId(final Context context, final String alternateAuthorityInManifest,
                                                final String clientId)
            throws PackageManager.NameNotFoundException {
        final PackageManager mockedPackageManager = context.getPackageManager();
        final ApplicationInfo applicationInfo = Mockito.mock(ApplicationInfo.class);
        // meta data is empty, no client id there.
        applicationInfo.metaData = new Bundle();
        applicationInfo.metaData.putString("com.microsoft.identity.client.ClientId", clientId);
        if (!MSALUtils.isEmpty(alternateAuthorityInManifest)) {
            applicationInfo.metaData.putString("com.microsoft.identity.client.Authority", alternateAuthorityInManifest);
        }

        Mockito.when(mockedPackageManager.getApplicationInfo(
                Mockito.refEq(mAppContext.getPackageName()), Mockito.eq(
                        PackageManager.GET_META_DATA))).thenReturn(applicationInfo);

        final PackageInfo mockedPackageInfo = Mockito.mock(PackageInfo.class);
        Mockito.when(mockedPackageManager.getPackageInfo(Mockito.anyString(), Mockito.anyInt())).thenReturn(mockedPackageInfo);
    }

    private void mockHasCustomTabRedirect(final Context context) {
        final PackageManager packageManager = context.getPackageManager();

        final List<ResolveInfo> resolveInfos = new ArrayList<>();
        Mockito.when(packageManager.queryIntentActivities(Matchers.any(Intent.class),
                Matchers.eq(PackageManager.GET_RESOLVED_FILTER))).thenReturn(resolveInfos);

        final ResolveInfo mockedResolveInfo1 = Mockito.mock(ResolveInfo.class);
        final ActivityInfo mockedActivityInfo1 = Mockito.mock(ActivityInfo.class);
        mockedActivityInfo1.name = BrowserTabActivity.class.getName();
        mockedResolveInfo1.activityInfo = mockedActivityInfo1;
        resolveInfos.add(mockedResolveInfo1);
    }

    private void mockAuthenticationActivityResolvable(final Context context) {
        final PackageManager packageManager = context.getPackageManager();
        Mockito.when(packageManager.resolveActivity(Mockito.any(Intent.class),
                Mockito.anyInt())).thenReturn(Mockito.mock(ResolveInfo.class));
    }

    private Activity getActivity(final Context context) {
        final Activity mockedActivity = Mockito.mock(Activity.class);
        Mockito.when(mockedActivity.getApplicationContext()).thenReturn(context);

        return mockedActivity;
    }

    private void mockSuccessResponse(final String scopes, final String accessToken, final String clientInfo) throws IOException {
        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponse(AndroidTestUtil.TEST_IDTOKEN, accessToken, scopes, clientInfo));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
    }

    private String convertScopesArrayToString(final String[] scopes) {
        final Set<String> scopesInSet = new HashSet<>(Arrays.asList(scopes));
        return MSALUtils.convertSetToString(scopesInSet, " ");
    }

    private Context getMockedContext(final String clientId) throws PackageManager.NameNotFoundException {
        final Context context = new MockContext(mAppContext);
        mockPackageManagerWithClientId(context, null, clientId);
        mockHasCustomTabRedirect(context);
        mockAuthenticationActivityResolvable(context);

        return context;
    }

    private static class MockContext extends ContextWrapper {
        private final PackageManager mPackageManager;

        MockContext(final Context context) {
            super(context);
            mPackageManager = Mockito.mock(PackageManager.class);
        }

        @Override
        public PackageManager getPackageManager() {
            return mPackageManager;
        }
    }

    abstract class GetTokenBaseTestCase {

        abstract void mockHttpRequest() throws IOException;

        abstract void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
                                           final Activity activity,
                                           final CountDownLatch releaseLock);

        abstract String getFinalAuthUrl() throws UnsupportedEncodingException;

        protected String getAlternateAuthorityInManifest() {
            return null;
        }

        protected void performAdditionalVerify(final Activity testActivity) {
        }

        protected void makeSilentRequest(final PublicClientApplication publicClientApplication, final CountDownLatch silentLock)
                throws IOException, InterruptedException {
            silentLock.countDown();
        }

        public void performTest() throws PackageManager.NameNotFoundException, IOException, InterruptedException {
            final Context context = new MockContext(mAppContext);
            mockPackageManagerWithClientId(context, getAlternateAuthorityInManifest(), CLIENT_ID);
            mockHasCustomTabRedirect(context);
            mockAuthenticationActivityResolvable(context);

            if (!MSALUtils.isEmpty(getAlternateAuthorityInManifest())) {
                AndroidTestMockUtil.mockSuccessTenantDiscovery(getAlternateAuthorityInManifest() + Authority.DEFAULT_AUTHORIZE_ENDPOINT,
                        ALTERNATE_AUTHORITY + DEFAULT_TOKEN_ENDPOINT);
            } else {
                AndroidTestMockUtil.mockSuccessTenantDiscovery(SilentRequestTest.AUTHORIZE_ENDPOINT, SilentRequestTest.TOKEN_ENDPOINT);
            }

            mockHttpRequest();

            final CountDownLatch resultLock = new CountDownLatch(1);
            final Activity testActivity = getActivity(context);
            final PublicClientApplication application = new PublicClientApplication(context);
            makeAcquireTokenCall(application, testActivity, resultLock);

            // having the thread delayed for preTokenRequest to finish. Here we mock the
            // startActivityForResult, nothing actually happened when AuthenticationActivity is called.
            resultLock.await(InteractiveRequestTest.THREAD_DELAY_TIME, TimeUnit.MILLISECONDS);

            final Intent resultIntent = new Intent();
            resultIntent.putExtra(Constants.AUTHORIZATION_FINAL_URL, getFinalAuthUrl());
            InteractiveRequest.onActivityResult(InteractiveRequest.BROWSER_FLOW,
                    Constants.UIResponse.AUTH_CODE_COMPLETE, resultIntent);

            resultLock.await();

            performAdditionalVerify(testActivity);

            final CountDownLatch silentRequestLock = new CountDownLatch(1);
            makeSilentRequest(application, silentRequestLock);
            silentRequestLock.await();
        }
    }
}