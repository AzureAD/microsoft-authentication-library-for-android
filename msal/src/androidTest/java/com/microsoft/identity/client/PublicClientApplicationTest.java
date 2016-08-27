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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link PublicClientApplication}.
 */
@RunWith(AndroidJUnit4.class)
public final class PublicClientApplicationTest extends AndroidTestCase {
    private Context mAppContext;
    private String mRedirectUri;
    private static final String CLIENT_ID = "client-id";
    private static final String DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common";
    private static final String ALTERNATE_AUTHORITY = "https://login.microsoftonline.com/alternateAuthority";
    private static final String[] SCOPE = {"scope1", "scope2"};

    @Before
    public void setUp() throws Exception {
        super.setUp();
        InstrumentationRegistry.getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache",
                InstrumentationRegistry.getContext().getCacheDir().getPath());

        mAppContext = InstrumentationRegistry.getContext().getApplicationContext();
        mRedirectUri = "msauth-client-id://" + mAppContext.getPackageName();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    /**
     * Verify correct exception is thrown if activity is not provided.
     */
    @Test (expected = IllegalArgumentException.class)
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
        mockPackageManagerWithClientId(context, false);

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
        mockPackageManagerWithClientId(context, false);
        mockHasCustomTabRedirect(context);

        final PublicClientApplication application = new PublicClientApplication(getActivity(context));
        application.acquireToken(SCOPE, null);
    }

    @Test(expected = IllegalStateException.class)
    public void testInternetPermissionMissing() throws PackageManager.NameNotFoundException {
        final Context context = new MockActivityContext(mAppContext);
        final PackageManager packageManager = context.getPackageManager();
        mockPackageManagerWithClientId(context, false);
        mockHasCustomTabRedirect(context);
        Mockito.when(packageManager.checkPermission(Mockito.refEq("android.permission.INTERNET"),
                Mockito.refEq(mAppContext.getPackageName()))).thenReturn(PackageManager.PERMISSION_DENIED);

        new PublicClientApplication(getActivity(context));
    }

    /**
     * Verify {@link PublicClientApplication#acquireToken(String[], AuthenticationCallback)}.
     */
    @Test
    public void testAcquireTokenSuccess() throws PackageManager.NameNotFoundException, IOException,
            InterruptedException {
        new GetTokenBaseTestCase() {

            @Override
            void mockHttpRequest() throws IOException {
                InteractiveRequestTest.mockSuccessHttpRequestCall();
            }

            @Override
            void makeAcquireTokenCall(PublicClientApplication publicClientApplication,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(SCOPE, new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        Assert.assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getToken()));
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
        }.performTest();
    }

    /**
     * Verify {@link PublicClientApplication#acquireToken(String[], String, UIOptions, String, String[],
     * String, String, AuthenticationCallback)}. Also check if authority is set on the manifest, we read the authority
     * from manifest meta-data.
     */
    @Test
    public void testAuthoritySetInManifestGetTokenFailed()
            throws PackageManager.NameNotFoundException, IOException, InterruptedException {
        new GetTokenBaseTestCase() {

            @Override
            protected boolean isSetAlternateAuthority() {
                return true;
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
     * Verify {@link PublicClientApplication#acquireToken(String[], String, UIOptions, String, AuthenticationCallback)}.
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
                publicClientApplication.acquireToken(SCOPE, "somehint", UIOptions.FORCE_LOGIN, "extra=param",
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
            String getFinalAuthUrl() {
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

    /**
     * Verify {@link PublicClientApplication#acquireToken(String[], String, UIOptions, String, AuthenticationCallback)}.
     */
    @Test
    public void testGetTokenWithPolicy() throws PackageManager.NameNotFoundException, IOException,
            InterruptedException {
        new GetTokenBaseTestCase() {

            @Override
            void mockHttpRequest() throws IOException {
                InteractiveRequestTest.mockSuccessHttpRequestCall();
            }

            @Override
            void makeAcquireTokenCall(PublicClientApplication publicClientApplication,
                                      final CountDownLatch releaseLock) {
                publicClientApplication.acquireToken(SCOPE, "", UIOptions.FORCE_LOGIN, null, null, null, "singin",
                        new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        Assert.assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getToken()));
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
        }.performTest();
    }

    private void mockPackageManagerWithClientId(final Context context,
                                                final boolean addAuthorityInManifest)
            throws PackageManager.NameNotFoundException {
        final PackageManager mockedPackageManager = context.getPackageManager();
        final ApplicationInfo applicationInfo = Mockito.mock(ApplicationInfo.class);
        // meta data is empty, no client id there.
        applicationInfo.metaData = new Bundle();
        applicationInfo.metaData.putString("com.microsoft.identity.client.ClientId", CLIENT_ID);
        if (addAuthorityInManifest) {
            applicationInfo.metaData.putString("com.microsoft.identity.client.Authority", ALTERNATE_AUTHORITY);
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

        protected boolean isSetAlternateAuthority() {
            return false;
        }

        protected void performAdditionalVerify(final Activity testActivity) { }

        public void performTest() throws PackageManager.NameNotFoundException, IOException, InterruptedException {
            final Context context = new MockActivityContext(mAppContext);
            mockPackageManagerWithClientId(context, isSetAlternateAuthority());
            mockHasCustomTabRedirect(context);
            mockAuthenticationActivityResolvable(context);

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
        }
    }
}
