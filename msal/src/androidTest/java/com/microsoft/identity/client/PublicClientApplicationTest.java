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
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.internal.MsalUtils;
import com.microsoft.identity.client.internal.controllers.RequestCodes;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.internal.controllers.ApiDispatcher;
import com.microsoft.identity.common.internal.net.HttpUrlConnectionFactory;
import com.microsoft.identity.common.internal.util.StringUtil;

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
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

/**
 * Tests for {@link PublicClientApplication}.
 */
@RunWith(AndroidJUnit4.class)
public final class PublicClientApplicationTest {
    private Context mAppContext;
    private String mRedirectUri;
    private static final String CLIENT_ID = "client-id";
    private static final String DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common";
    private static final String ALTERNATE_AUTHORITY = "https://login.microsoftonline.com/alternateAuthority";
    private static final String[] SCOPE = {"scope1", "scope2"};
    private static final int EXPECTED_USER_SIZE = 3;
    private static final String DEFAULT_TOKEN_ENDPOINT = "/oauth2/v2.0/token";
    private static final String DEFAULT_CLIENT_INFO = AndroidTestUtil.createRawClientInfo(AndroidTestUtil.UID, AndroidTestUtil.UTID);

    @Before
    public void setUp() {
        System.setProperty(
                "dexmaker.dexcache",
                androidx.test.platform.app.InstrumentationRegistry
                        .getInstrumentation()
                        .getTargetContext()
                        .getCacheDir()
                        .getPath()
        );

        System.setProperty(
                "org.mockito.android.target",
                ApplicationProvider
                        .getApplicationContext()
                        .getCacheDir()
                        .getPath()
        );

        mAppContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext().getApplicationContext();
        mRedirectUri = "msauth-client-id://" + mAppContext.getPackageName();
        Telemetry.disableForTest(true);
    }

    @After
    public void tearDown() {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
        AndroidTestUtil.removeAllTokens(mAppContext);
        Telemetry.disableForTest(false);
    }

    /**
     * Verify correct exception is thrown if {@link BrowserTabActivity} does not have the correct intent-filer.
     */
    @Test(expected = IllegalStateException.class)
    @Ignore
    public void testNoCustomTabSchemeConfigured() throws PackageManager.NameNotFoundException {
        final Context context = new MockContext(mAppContext);
        mockPackageManagerWithClientId(context, null, CLIENT_ID);

        new PublicClientApplication(context, CLIENT_ID);
    }

    /**
     * Verify correct exception is thrown if callback is not provided.
     */
    @Test(expected = IllegalArgumentException.class)
    @Ignore
    public void testCallBackEmpty() throws PackageManager.NameNotFoundException {
        final Context context = new MockContext(mAppContext);
        mockPackageManagerWithClientId(context, null, CLIENT_ID);
        mockHasCustomTabRedirect(context);

        final PublicClientApplication application = new PublicClientApplication(context, CLIENT_ID);
        application.acquireToken(getActivity(context), SCOPE, null);
    }

    @Test(expected = IllegalStateException.class)
    @Ignore
    public void testInternetPermissionMissing() throws PackageManager.NameNotFoundException {
        final Context context = new MockContext(mAppContext);
        final PackageManager packageManager = context.getPackageManager();
        mockPackageManagerWithClientId(context, null, CLIENT_ID);
        mockHasCustomTabRedirect(context);
        Mockito.when(packageManager.checkPermission(Mockito.refEq("android.permission.INTERNET"),
                Mockito.refEq(mAppContext.getPackageName()))).thenReturn(PackageManager.PERMISSION_DENIED);

        new PublicClientApplication(context, CLIENT_ID);
    }

    @Test
    @Ignore
    public void testUnknownAuthorityException() throws PackageManager.NameNotFoundException, IOException,
            InterruptedException {
        new GetTokenBaseTestCase() {

            @Override
            void mockHttpRequest() throws IOException {
                mockSuccessResponse(convertScopesArrayToString(SCOPE), AndroidTestUtil.ACCESS_TOKEN, DEFAULT_CLIENT_INFO);
            }

            @Override
            void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final CountDownLatch releaseLock) {

                AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withScopes(Arrays.asList(SCOPE))
                        .withLoginHint("loginhint")
                        .fromAuthority("https://someauthority")
                        .callback(new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                fail("Unexpected success");
                            }

                            @Override
                            public void onError(MsalException exception) {
                                Assert.assertTrue(exception instanceof MsalClientException);
                                Assert.assertEquals(exception.getErrorCode(), MsalClientException.UNKNOWN_AUTHORITY);
                            }

                            @Override
                            public void onCancel() {
                                fail("Unexpected Cancel");
                            }
                        })
                        .build();
                publicClientApplication.acquireToken(parameters);
            }

            @Override
            String getFinalAuthUrl() throws UnsupportedEncodingException {
                return null;
            }

        }.performTest();

    }

    @Test(expected = IllegalArgumentException.class)
    @Ignore
    public void testAcquireTokenInteractiveScopeWithEmptyString() throws PackageManager.NameNotFoundException, IOException,
            InterruptedException {
        new GetTokenBaseTestCase() {
            @Override
            void mockHttpRequest() throws IOException {
                final String idToken = AndroidTestUtil.getDefaultIdToken();
                final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_OK, AndroidTestUtil.getSuccessResponse(idToken, AndroidTestUtil.ACCESS_TOKEN, AndroidTestUtil.REFRESH_TOKEN, ""));
                Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
                HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
            }

            @Override
            void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(activity, new String[]{" "}, new AuthenticationCallback() {
                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        Assert.assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                        final IAccount account = authenticationResult.getAccount();

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

    @Test
    @Ignore
    public void testClientInfoNotReturned() throws PackageManager.NameNotFoundException, IOException,
            InterruptedException {
        new GetTokenBaseTestCase() {

            @Override
            void mockHttpRequest() throws IOException {
                final String idToken = AndroidTestUtil.getDefaultIdToken();
                final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_OK, AndroidTestUtil.getSuccessResponse(idToken, AndroidTestUtil.ACCESS_TOKEN, AndroidTestUtil.REFRESH_TOKEN, "eyJ1aWQiOiI1MGE0YjhhMS0zMzNiLTQwNDEtOGQzNS0wYTg2MDY2YzE1YTgiLCJ1dGlkIjoiMGE4NGU5NTctODg0Yi00NmQxLTk0OGYtYTUwMWIwZWE2NmYyIn0="));
                Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
                HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
            }

            @Override
            void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(activity, SCOPE, new AuthenticationCallback() {
                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        Assert.assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                        final IAccount account = authenticationResult.getAccount();

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

    /**
     * Verify {@link PublicClientApplication#acquireToken(Activity, String[], String, UiBehavior, List, String[],
     * String, AuthenticationCallback)}. Also check if authority is set on the manifest, we read the authority
     * from manifest meta-data.
     * <p>
     * NOTE: Ignoring until we've updated the code to do authority validation per the new design.  Currently setting an authority other than the default will fail.
     */
    @Test
    @Ignore
    public void testAuthoritySetInManifestGetTokenFailed()
            throws PackageManager.NameNotFoundException, IOException, InterruptedException {

        // TODO: Revisit this once we're ready to update test cases for ISingle and IMuliple PCA.
//        new GetTokenBaseTestCase() {
//
//            @Override
//            protected String getAlternateAuthorityInManifest() {
//                return ALTERNATE_AUTHORITY;
//            }
//
//            @Override
//            void mockHttpRequest() throws IOException {
//                final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithFailureResponse(
//                        HttpURLConnection.HTTP_BAD_REQUEST, AndroidTestUtil.getErrorResponseMessage("invalid_request"));
//                Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
//                HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
//            }
//
//            @Override
//            void makeAcquireTokenCall(final PublicClientApplication publicClientApplication,
//                                      final Activity activity,
//                                      final CountDownLatch releaseLock) {
//
//                publicClientApplication.acquireToken(activity, SCOPE, "somehint", new AuthenticationCallback() {
//                    @Override
//                    public void onSuccess(IAuthenticationResult authenticationResult) {
//                        fail();
//                    }
//
//                    @Override
//                    public void onError(MsalException exception) {
//                        assertTrue(exception instanceof MsalServiceException);
//
//                        final MsalServiceException serviceException = (MsalServiceException) exception;
//                        assertTrue(MsalServiceException.INVALID_REQUEST.equals(serviceException.getErrorCode()));
//                        assertTrue(!serviceException.getMessage().isEmpty());
//                        assertTrue(serviceException.getHttpStatusCode() == HttpURLConnection.HTTP_BAD_REQUEST);
//                        releaseLock.countDown();
//                    }
//
//                    @Override
//                    public void onCancel() {
//                        fail();
//                    }
//                });
//            }
//
//            @Override
//            String getFinalAuthUrl() throws UnsupportedEncodingException {
//                return mRedirectUri + "?code=1234&state=" + AndroidTestUtil.encodeProtocolState(
//                        ALTERNATE_AUTHORITY, new HashSet<>(Arrays.asList(SCOPE)));
//            }
//
//            @Override
//            protected void performAdditionalVerify(Activity testActivity) {
//                Mockito.verify(testActivity).startActivityForResult(Mockito.argThat(
//                        new ArgumentMatcher<Intent>() {
//                            @Override
//                            public boolean matches(Intent argument) {
//                                final String data = argument.getStringExtra(Constants.REQUEST_URL_KEY);
//                                return data.startsWith(ALTERNATE_AUTHORITY);
//                            }
//                        }), Matchers.eq(RequestCodes.LOCAL_AUTHORIZATION_REQUEST));
//            }
//        }.performTest();
    }

    /**
     * Verify {@link PublicClientApplication#acquireToken(Activity, String[], String, UiBehavior, List, String[], String, AuthenticationCallback)}.
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
                AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withScopes(Arrays.asList(SCOPE))
                        .withLoginHint("somehint")
                        .withUiBehavior(UiBehavior.FORCE_LOGIN)
                        .withAuthorizationQueryStringParameters(new ArrayList<Pair<String, String>>() {{
                            add(new Pair<>("extra", "param"));
                        }})
                        .callback(new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
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
                        })
                        .build();
                publicClientApplication.acquireToken(parameters);
            }

            @Override
            String getFinalAuthUrl() throws UnsupportedEncodingException {
                return mRedirectUri + "?error=access_denied&error_subcode=cancel";
            }

            @Override
            protected void performAdditionalVerify(Activity testActivity) {
                Mockito.verify(testActivity).startActivityForResult(Mockito.argThat(new ArgumentMatcher<Intent>() {
                    @Override
                    public boolean matches(Intent argument) {
                        if (argument.getStringExtra(Constants.REQUEST_URL_KEY) != null) {
                            return true;
                        }

                        return false;
                    }
                }), Mockito.eq(RequestCodes.LOCAL_AUTHORIZATION_REQUEST));
            }
        }.performTest();
    }

    @Test
    @Ignore
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
                AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withScopes(Arrays.asList(SCOPE))
                        .withLoginHint("somehint")
                        .withUiBehavior(UiBehavior.FORCE_LOGIN)
                        .withAuthorizationQueryStringParameters(new ArrayList<Pair<String, String>>() {{
                            add(new Pair<>("extra", "param"));
                        }})
                        .callback(new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                fail("unexpected success result");
                            }

                            @Override
                            public void onError(MsalException exception) {
                                try {
                                    assertTrue(exception instanceof MsalClientException);
                                    assertTrue(exception.getErrorCode().equals(MsalClientException.AUTHORITY_VALIDATION_NOT_SUPPORTED));
                                } finally {
                                    releaseLock.countDown();
                                }
                            }

                            @Override
                            public void onCancel() {
                                fail("unexpected cancel");
                            }
                        })
                        .build();
                publicClientApplication.acquireToken(parameters);
            }

            @Override
            String getFinalAuthUrl() throws UnsupportedEncodingException {
                return "";
            }
        }.performTest();
    }

    @Test
    @Ignore
    public void testSecretKeysAreSet() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final PublicClientApplication pca = new PublicClientApplication(mAppContext, CLIENT_ID);
        final PublicClientApplicationConfiguration appConfig = pca.getConfiguration();

        SecretKeyFactory keyFactory = SecretKeyFactory
                .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
        SecretKey generatedSecretKey = keyFactory.generateSecret(
                new PBEKeySpec(
                        "test_password".toCharArray(),
                        "byte-code-for-your-salt".getBytes(),
                        100,
                        256
                )
        );
        SecretKey secretKey = new SecretKeySpec(generatedSecretKey.getEncoded(), "AES");
        final byte[] encodedSecretKey = secretKey.getEncoded();

        appConfig.setTokenCacheSecretKeys(encodedSecretKey);

        // Check that the AuthenticationSettings.INSTANCE.secretKey matches the value configured
        assertEquals(
                encodedSecretKey,
                AuthenticationSettings.INSTANCE.getSecretKeyData()
        );
    }

    static String getIdToken(final String displayable, final String uniqueId, final String homeOid) {
        return AndroidTestUtil.createIdToken(
                AndroidTestUtil.AUDIENCE,
                AndroidTestUtil.ISSUER,
                AndroidTestUtil.NAME,
                uniqueId,
                displayable,
                AndroidTestUtil.SUBJECT,
                AndroidTestUtil.TENANT_ID,
                AndroidTestUtil.VERSION,
                null
        );
    }

    private void mockPackageManagerWithClientId(final Context context, final String alternateAuthorityInManifest,
                                                final String clientId)
            throws PackageManager.NameNotFoundException {
        final PackageManager mockedPackageManager = context.getPackageManager();
        final ApplicationInfo applicationInfo = Mockito.mock(ApplicationInfo.class);
        // meta data is empty, no client id there.
        applicationInfo.metaData = new Bundle();
        if (!MsalUtils.isEmpty(clientId)) {
            applicationInfo.metaData.putString("com.microsoft.identity.client.ClientId", clientId);
        }

        if (!MsalUtils.isEmpty(alternateAuthorityInManifest)) {
            applicationInfo.metaData.putString("com.microsoft.identity.client.AuthorityMetadata", alternateAuthorityInManifest);
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
        return StringUtil.convertSetToString(scopesInSet, " ");
    }

    private Context getMockedContext(final String clientId) throws PackageManager.NameNotFoundException {
        final Context context = new MockContext(mAppContext);
        mockPackageManagerWithClientId(context, null, clientId);
        mockHasCustomTabRedirect(context);
        mockAuthenticationActivityResolvable(context);
        AndroidTestUtil.mockNetworkConnected(context, true);

        return context;
    }

    private static class MockContext extends ContextWrapper {
        private final PackageManager mPackageManager;
        private final ConnectivityManager mConnectivityManager;

        MockContext(final Context context) {
            super(context);
            mPackageManager = Mockito.mock(PackageManager.class);
            mConnectivityManager = Mockito.mock(ConnectivityManager.class);
        }

        @Override
        public PackageManager getPackageManager() {
            return mPackageManager;
        }

        @Override
        public Object getSystemService(String name) {
            if (Context.CONNECTIVITY_SERVICE.equals(name)) {
                return mConnectivityManager;
            }

            return super.getSystemService(name);
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
            AndroidTestUtil.mockNetworkConnected(context, true);

            if (!MsalUtils.isEmpty(getAlternateAuthorityInManifest())) {
                AndroidTestMockUtil.mockSuccessTenantDiscovery(getAlternateAuthorityInManifest() + "/oauth2/v2.0/authorize",
                        ALTERNATE_AUTHORITY + DEFAULT_TOKEN_ENDPOINT);
            } else {
                AndroidTestMockUtil.mockSuccessTenantDiscovery(AndroidTestUtil.AUTHORIZE_ENDPOINT, AndroidTestUtil.TOKEN_ENDPOINT);
            }

            mockHttpRequest();

            final CountDownLatch resultLock = new CountDownLatch(1);
            final Activity testActivity = getActivity(context);

            final PublicClientApplication application = new PublicClientApplication(context, CLIENT_ID);
            makeAcquireTokenCall(application, testActivity, resultLock);


            // having the thread delayed for preTokenRequest to finish. Here we mock the
            // startActivityForResult, nothing actually happened when AuthenticationActivity is called.
            resultLock.await(AndroidTestUtil.THREAD_DELAY_TIME, TimeUnit.MILLISECONDS);

            final Intent resultIntent = new Intent();
            resultIntent.putExtra(Constants.AUTHORIZATION_FINAL_URL, getFinalAuthUrl());
            ApiDispatcher.completeInteractive(RequestCodes.LOCAL_AUTHORIZATION_REQUEST,
                    Constants.UIResponse.AUTH_CODE_COMPLETE, resultIntent);

            resultLock.await();

            performAdditionalVerify(testActivity);

            final CountDownLatch silentRequestLock = new CountDownLatch(1);
            makeSilentRequest(application, silentRequestLock);
            silentRequestLock.await();
        }
    }
}