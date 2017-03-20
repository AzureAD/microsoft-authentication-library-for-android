package com.microsoft.identity.client;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
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
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
        AndroidTestUtil.removeAllTokens(mAppContext);
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

        final Context context = new MockActivityContext(mAppContext);
        final PackageManager mockedPackageManager = context.getPackageManager();
        Mockito.when(mockedPackageManager.getApplicationInfo(
                Mockito.refEq(mAppContext.getPackageName()), Mockito.eq(
                        PackageManager.GET_META_DATA))).thenReturn(applicationInfo);

        final Activity mockedActivity = Mockito.mock(Activity.class);
        Mockito.when(mockedActivity.getApplicationContext()).thenReturn(context);

        new PublicClientApplication(mockedActivity);
    }

    /**
     * Verify correct exception is thrown if cannot retrieve package info.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testApplicationInfoIsNull() throws PackageManager.NameNotFoundException {
        final Context context = new MockActivityContext(mAppContext);
        final PackageManager mockedPackageManager = context.getPackageManager();
        Mockito.when(mockedPackageManager.getApplicationInfo(
                Mockito.refEq(mAppContext.getPackageName()), Mockito.eq(
                        PackageManager.GET_META_DATA))).thenReturn(null);

        final Activity mockedActivity = Mockito.mock(Activity.class);
        Mockito.when(mockedActivity.getApplicationContext()).thenReturn(context);

        new PublicClientApplication(mockedActivity);
    }

    /**
     * Verify correct exception is thrown if {@link BrowserTabActivity} does not have the correct intent-filer.
     */
    @Test(expected = IllegalStateException.class)
    public void testNoCustomTabSchemeConfigured() throws PackageManager.NameNotFoundException {
        final Context context = new MockActivityContext(mAppContext);
        mockPackageManagerWithClientId(context, null, CLIENT_ID);

        new PublicClientApplication(getActivity(context));
    }

    /**
     * Verify correct exception is thrown if meta-data is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMetaDataIsNull() throws PackageManager.NameNotFoundException {
        final ApplicationInfo applicationInfo = Mockito.mock(ApplicationInfo.class);
        // null metadata
        applicationInfo.metaData = null;

        final Context context = new MockActivityContext(mAppContext);
        final PackageManager mockedPackageManager = context.getPackageManager();
        Mockito.when(mockedPackageManager.getApplicationInfo(
                Mockito.refEq(mAppContext.getPackageName()), Mockito.eq(
                        PackageManager.GET_META_DATA))).thenReturn(applicationInfo);

        final Activity mockedActivity = Mockito.mock(Activity.class);
        Mockito.when(mockedActivity.getApplicationContext()).thenReturn(context);

        new PublicClientApplication(mockedActivity);
    }

    /**
     * Verify correct exception is thrown if callback is not provided.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCallBackEmpty() throws PackageManager.NameNotFoundException {
        final Context context = new MockActivityContext(mAppContext);
        mockPackageManagerWithClientId(context, null, CLIENT_ID);
        mockHasCustomTabRedirect(context);

        final PublicClientApplication application = new PublicClientApplication(getActivity(context));
        application.acquireToken(SCOPE, null);
    }

    @Test(expected = IllegalStateException.class)
    public void testInternetPermissionMissing() throws PackageManager.NameNotFoundException {
        final Context context = new MockActivityContext(mAppContext);
        final PackageManager packageManager = context.getPackageManager();
        mockPackageManagerWithClientId(context, null, CLIENT_ID);
        mockHasCustomTabRedirect(context);
        Mockito.when(packageManager.checkPermission(Mockito.refEq("android.permission.INTERNET"),
                Mockito.refEq(mAppContext.getPackageName()))).thenReturn(PackageManager.PERMISSION_DENIED);

        new PublicClientApplication(getActivity(context));
    }

    /**
     * Verify that users are correctly retrieved.
     */
    @Test
    public void testGetUsers() throws AuthenticationException, PackageManager.NameNotFoundException {
        final PublicClientApplication application = new PublicClientApplication(getMockedActivity(CLIENT_ID));
        assertTrue(application.getUsers().size() == 0);
        // prepare token cache
        // save token with Displayable as: Displayable1 UniqueId: UniqueId1 HomeObjectId: homeOID
        final String displayable1 = "Displayable1";
        final String uniqueId1 = "UniqueId1";
        final String homeOid1 = "HomeOid1";
        String idToken = getIdToken(displayable1, uniqueId1, homeOid1);

        saveTokenResponse(mTokenCache, TokenCacheTest.AUTHORITY, CLIENT_ID, getTokenResponse(idToken));

        // prepare token cache for same client id, same displayable, uniqueId but different oid
        final String homeOid2 = "HomeOid2";
        idToken = getIdToken(displayable1, uniqueId1, homeOid2);
        saveTokenResponse(mTokenCache, TokenCacheTest.AUTHORITY, CLIENT_ID, getTokenResponse(idToken));

        List<User> users = application.getUsers();
        assertTrue(users.size() == 2);

        // prepare token cache for same client id, different diplayable, uniqueid and oid
        final String displayable3 = "Displayable3";
        final String uniqueId3 = "UniqueId3";
        final String homeOid3 = "HomeOid3";
        idToken = getIdToken(displayable3, uniqueId3, homeOid3);
        saveTokenResponse(mTokenCache, TokenCacheTest.AUTHORITY, CLIENT_ID, getTokenResponse(idToken));

        users = application.getUsers();
        assertTrue(users.size() == EXPECTED_USER_SIZE);
        final User userForDisplayable3 = getUser(displayable3, users);
        assertNotNull(userForDisplayable3);
        assertTrue(userForDisplayable3.getDisplayableId().equals(displayable3));
        assertTrue(userForDisplayable3.getHomeObjectId().equals(homeOid3));

        // prepare token cache for different client id, same displayable3 user
        final String anotherClientId = "anotherClientId";
        saveTokenResponse(mTokenCache, TokenCacheTest.AUTHORITY, anotherClientId, getTokenResponse(idToken));
        final PublicClientApplication anotherApplication = new PublicClientApplication(getMockedActivity(anotherClientId));
        assertTrue(application.getUsers().size() == EXPECTED_USER_SIZE);
        users = anotherApplication.getUsers();
        assertTrue(users.size() == 1);
        final User userForAnotherClient = getUser(homeOid3, users);
        assertNotNull(userForAnotherClient);
        assertTrue(userForAnotherClient.getDisplayableId().equals(displayable3));
        assertTrue(userForAnotherClient.getHomeObjectId().equals(homeOid3));
    }

    /**
     * From the supplied {@link List} of {@link User}, return the instance with a matching displayableId or homeObjectId.
     *
     * @param userIdentifier The user identifier, could be either displayableId or homeObjectId
     * @param users          the list of Users to traverse
     * @return
     */
    private User getUser(final String userIdentifier, final List<User> users) {
        User resultUser = null;
        for (final User user : users) {
            if (userIdentifier.equals(user.getDisplayableId()) || userIdentifier.equals(user.getHomeObjectId())) {
                resultUser = user;
                break;
            }
        }
        return resultUser;
    }

    /**
     * Verify {@link PublicClientApplication#acquireToken(String[], AuthenticationCallback)}.
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
                mockSuccessResponse(convertScopesArrayToString(SCOPE), AndroidTestUtil.ACCESS_TOKEN);
            }

            @Override
            void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(SCOPE, new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        Assert.assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                        mUser = authenticationResult.getUser();

                        releaseLock.countDown();
                    }

                    @Override
                    public void onError(AuthenticationException exception) {
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
                mockSuccessResponse(scopeForSilent, AndroidTestUtil.ACCESS_TOKEN);

                application.acquireTokenSilentAsync(new String[]{scopeForSilent}, mUser, new AuthenticationCallback() {

                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        assertTrue(authenticationResult.getAccessToken().equals(AndroidTestUtil.ACCESS_TOKEN));
                        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 2);
                        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);
                        silentResultLock.countDown();
                    }

                    @Override
                    public void onError(AuthenticationException exception) {
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

    /**
     * Verify {@link PublicClientApplication#acquireToken(String[], String, UIBehavior, String, String[],
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
            void makeAcquireTokenCall(PublicClientApplication publicClientApplication,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(SCOPE, "somehint", new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        fail();
                    }

                    @Override
                    public void onError(AuthenticationException exception) {
                        assertTrue(MSALError.OAUTH_ERROR.equals(exception.getErrorCode()));
                        assertTrue(exception.getMessage().contains("invalid_request"));
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
     * Verify {@link PublicClientApplication#acquireToken(String[], String, UIBehavior, String, AuthenticationCallback)}.
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
            void makeAcquireTokenCall(PublicClientApplication publicClientApplication,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(SCOPE, "somehint", UIBehavior.FORCE_LOGIN, "extra=param",
                        new AuthenticationCallback() {
                            @Override
                            public void onSuccess(AuthenticationResult authenticationResult) {
                                fail("unexpected success result");
                            }

                            @Override
                            public void onError(AuthenticationException exception) {
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
            void makeAcquireTokenCall(PublicClientApplication publicClientApplication,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(SCOPE, "somehint", UIBehavior.FORCE_LOGIN, "extra=param",
                        new AuthenticationCallback() {
                            @Override
                            public void onSuccess(AuthenticationResult authenticationResult) {
                                fail("unexpected success result");
                            }

                            @Override
                            public void onError(AuthenticationException exception) {
                                try {
                                    assertTrue(exception.getErrorCode().equals(MSALError.UNSUPPORTED_AUTHORITY_VALIDATION));
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
     * Verify {@link PublicClientApplication#acquireToken(String[], String, UIBehavior, String, AuthenticationCallback)}.
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
                mockSuccessResponse(convertScopesArrayToString(SCOPE), AndroidTestUtil.ACCESS_TOKEN);
            }

            @Override
            void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(SCOPE, "", UIBehavior.FORCE_LOGIN, null, null, null,
                        new AuthenticationCallback() {
                            @Override
                            public void onSuccess(AuthenticationResult authenticationResult) {
                                Assert.assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                                mUser = authenticationResult.getUser();
                                releaseLock.countDown();
                            }

                            @Override
                            public void onError(AuthenticationException exception) {
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
                mockSuccessResponse(silentRequestScope, newAccessToken);

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
                    public void onError(AuthenticationException exception) {
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
    public void testSilentRequestFailure() throws AuthenticationException, InterruptedException {
        final Activity activity = Mockito.mock(Activity.class);
        Mockito.when(activity.getApplicationContext()).thenReturn(mAppContext);
        final PublicClientApplication application = new PublicClientApplication(activity);

        // prepare token in the cache
        saveTokenResponse(mTokenCache, AndroidTestUtil.DEFAULT_AUTHORITY_WITH_TENANT, CLIENT_ID, TokenCacheTest.getTokenResponseForDefaultUser(
                AndroidTestUtil.ACCESS_TOKEN, AndroidTestUtil.REFRESH_TOKEN, "scope1 scope2", AndroidTestUtil.getExpiredDate()));

        final IdToken idToken = new IdToken(AndroidTestUtil.getRawIdToken("another Displayable", "another uniqueId", "another homeobj"));
        final User user = new User(idToken);

        final CountDownLatch silentLock = new CountDownLatch(1);
        application.acquireTokenSilentAsync(new String[]{"scope1", "scope2"}, user, new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(AuthenticationException exception) {
                assertTrue(exception.getErrorCode().equals(MSALError.INTERACTION_REQUIRED));
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

    static String getIdToken(final String displayable, final String uniqueId, final String homeOid) {
        return AndroidTestUtil.createIdToken(AndroidTestUtil.AUDIENCE, AndroidTestUtil.ISSUER, AndroidTestUtil.NAME, uniqueId, displayable,
                AndroidTestUtil.SUBJECT, AndroidTestUtil.TENANT_ID, AndroidTestUtil.VERSION, homeOid);
    }

    private TokenResponse getTokenResponse(final String idToken) throws AuthenticationException {
        return new TokenResponse(AndroidTestUtil.ACCESS_TOKEN, idToken, AndroidTestUtil.REFRESH_TOKEN, new Date(), new Date(),
                new Date(), "scope", "Bearer", null);
    }

    static void saveTokenResponse(final TokenCache tokenCache, final String authority, final String clientId,
                                  final TokenResponse response) throws AuthenticationException {
        tokenCache.saveAccessToken(authority, clientId, response);
        tokenCache.saveRefreshToken(authority, clientId, response);
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

    private void mockSuccessResponse(final String scopes, final String accessToken) throws IOException {
        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponse(AndroidTestUtil.TEST_IDTOKEN, accessToken, scopes));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
    }

    private String convertScopesArrayToString(final String[] scopes) {
        final Set<String> scopesInSet = new HashSet<>(Arrays.asList(scopes));
        return MSALUtils.convertSetToString(scopesInSet, " ");
    }

    private Activity getMockedActivity(final String clientId) throws PackageManager.NameNotFoundException {
        final Context context = new MockActivityContext(mAppContext);
        mockPackageManagerWithClientId(context, null, clientId);
        mockHasCustomTabRedirect(context);
        mockAuthenticationActivityResolvable(context);

        return getActivity(context);
    }

    private static class MockActivityContext extends ContextWrapper {
        private final PackageManager mPackageManager;

        MockActivityContext(final Context context) {
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
            final Context context = new MockActivityContext(mAppContext);
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
            final PublicClientApplication application = new PublicClientApplication(testActivity);
            makeAcquireTokenCall(application, resultLock);

            // having the thread delayed for preTokenRequest to finish. Here we mock the
            // startActivityForResult, nothing actually happened when AuthenticationActivity is called.
            resultLock.await(InteractiveRequestTest.TREAD_DELAY_TIME, TimeUnit.MILLISECONDS);

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
