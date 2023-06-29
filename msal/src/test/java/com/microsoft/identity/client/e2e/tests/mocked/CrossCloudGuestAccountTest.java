// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.identity.client.e2e.tests.mocked;

import android.os.Build;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.MultiTenantAccount;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.e2e.shadows.ShadowAuthorityForMockHttpResponse;
import com.microsoft.identity.client.e2e.shadows.ShadowPublicClientApplicationConfiguration;
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager;
import com.microsoft.identity.client.e2e.tests.AcquireTokenAbstractTest;
import com.microsoft.identity.client.e2e.utils.RoboTestUtils;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.java.cache.IMultiTypeNameValueStorage;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.http.HttpRequestMatcher;
import com.microsoft.identity.http.MockHttpClient;
import com.microsoft.identity.internal.testutils.TestConstants;
import com.microsoft.identity.internal.testutils.mocks.MockServerResponse;
import com.microsoft.identity.shadow.ShadowHttpClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper.getAccount;
import static com.microsoft.identity.common.java.cache.AbstractAccountCredentialCache.SHA1_APPLICATION_IDENTIFIER_ACCESS_TOKEN_CLEARED;
import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.USER_READ_SCOPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(shadows = {
        ShadowAndroidSdkStorageEncryptionManager.class,
        ShadowAuthorityForMockHttpResponse.class,
        ShadowPublicClientApplicationConfiguration.class,
        ShadowHttpClient.class,
}, sdk = {Build.VERSION_CODES.N})
public class CrossCloudGuestAccountTest extends AcquireTokenAbstractTest {
    private final TestCaseData mTestCaseData;
    private final MockHttpClient mMockHttpClient = MockHttpClient.install();
    private IMultipleAccountPublicClientApplication mMultipleAccountPCA;
    private boolean mHomeAccountSignedIn = false;

    public CrossCloudGuestAccountTest(final String name, final TestCaseData testCaseData) {
        this.mTestCaseData = testCaseData;
    }

    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
    public static Iterable<Object[]> getTestDataForInteractiveAcquireTokenCallToSetupCache() {
        final String userName = "testUser@multipleAccountPCATests.onmicrosoft.com";
        final String homeCloud = "https://login.microsoftonline.com";
        final String foreignCloud = "https://login.microsoftonline.us";
        final String foreignCloud2 = "https://login.microsoftonline.de";
        final String uid = randomUuidString();
        final String utid = randomUuidString();

        return Arrays.asList(
                new Object[]{
                        "User signed into Home tenant and 1 Guest tenant in Foreign cloud",
                        new TestCaseData(
                                userName,
                                uid + "." + utid,
                                new ArrayList<UserAccountData>(Arrays.asList(
                                        new UserAccountData(homeCloud, uid, utid),
                                        new UserAccountData(foreignCloud, randomUuidString(), randomUuidString())
                                ))
                        )},
                new Object[]{
                        "User signed into Home tenant and Guest tenants in 2 separate Foreign clouds",
                        new TestCaseData(
                                userName,
                                uid + "." + utid,
                                new ArrayList<>(Arrays.asList(
                                        new UserAccountData(homeCloud, uid, utid),
                                        new UserAccountData(foreignCloud, randomUuidString(), randomUuidString()),
                                        new UserAccountData(foreignCloud2, randomUuidString(), randomUuidString())
                                ))
                        )},
                new Object[]{
                        "User signed into Home tenant and 2 Guest tenants in Foreign cloud ",
                        new TestCaseData(
                                userName,
                                uid + "." + utid,
                                new ArrayList<>(Arrays.asList(
                                        new UserAccountData(homeCloud, uid, utid),
                                        new UserAccountData(foreignCloud, randomUuidString(), randomUuidString()),
                                        new UserAccountData(foreignCloud, randomUuidString(), randomUuidString())
                                ))
                        )},
                new Object[]{
                        "User signed into 2 Guest tenants in 1 Foreign cloud ",
                        new TestCaseData(
                                userName,
                                uid + "." + utid,
                                new ArrayList<>(Arrays.asList(
                                        new UserAccountData(foreignCloud, randomUuidString(), randomUuidString()),
                                        new UserAccountData(foreignCloud, randomUuidString(), randomUuidString())
                                ))
                        )},
                new Object[]{
                        "User signed into Home tenant ",
                        new TestCaseData(
                                userName,
                                uid + "." + utid,
                                new ArrayList<>(Arrays.asList(
                                        new UserAccountData(homeCloud, uid, utid)
                                ))
                        )},
                new Object[]{
                        "User signed into Home tenant and 1 guest tenant in home cloud ",
                        new TestCaseData(
                                userName,
                                uid + "." + utid,
                                new ArrayList<>(Arrays.asList(
                                        new UserAccountData(homeCloud, uid, utid),
                                        new UserAccountData(homeCloud, randomUuidString(), randomUuidString())
                                ))
                        )},
                new Object[]{
                        "User signed into multiple tenants in home cloud and foreign cloud",
                        new TestCaseData(
                                userName,
                                uid + "." + utid,
                                new ArrayList<>(Arrays.asList(
                                        new UserAccountData(homeCloud, uid, utid),
                                        new UserAccountData(homeCloud, randomUuidString(), randomUuidString()),
                                        new UserAccountData(foreignCloud, randomUuidString(), randomUuidString()),
                                        new UserAccountData(foreignCloud, randomUuidString(), randomUuidString())
                                ))
                        )}
        );
    }

    private static String randomUuidString() {
        return UUID.randomUUID().toString();
    }

    @Before
    public void setup() {
        super.setup();
        mMultipleAccountPCA = (IMultipleAccountPublicClientApplication) mApplication;
        performInteractiveAcquireTokenWithMockResponsesToSetupCache();
    }

    @After
    public void cleanup() {
        super.cleanup();
        mComponents.getStorageSupplier().getEncryptedFileStore(SHARED_PREFERENCES_NAME)
                .clear();
    }

    @Test
    public void testGetAccountsReturnsSingleAccountObjectForAccountRecordsFromMultipleCloudsWithSameHomeAccountId() {
        // arrange
        final List<IAccount> accounts = new ArrayList<>();
        int expectedTenantProfilesCount = mHomeAccountSignedIn ?
                mTestCaseData.userAccountsData.size() - 1 : mTestCaseData.userAccountsData.size();

        // act
        mMultipleAccountPCA.getAccounts(new IPublicClientApplication.LoadAccountsCallback() {
            @Override
            public void onTaskCompleted(List<IAccount> result) {
                accounts.addAll(result);
            }

            @Override
            public void onError(MsalException exception) {
                fail("Failure in getAccounts : " + exception.getMessage());
            }
        });

        RoboTestUtils.flushScheduler();

        // assert
        try {
            assertEquals("Verify getAccounts return only 1 account", 1, accounts.size());

            MultiTenantAccount account = (MultiTenantAccount) accounts.get(0);
            assertEquals("Verify count of tenant profiles matches guest accounts",
                    expectedTenantProfilesCount, account.getTenantProfiles().size());
            assertTrue(mTestCaseData.homeAccountId.contains(account.getId()));

            if (mHomeAccountSignedIn) {
                assertNotNull("Verify root account has claims", account.getClaims());
                assertNotEquals("Verify root account claims count is not 0",
                        0, account.getClaims().size());
                verifyAccountDetails(mTestCaseData.userAccountsData.get(0), account);
            }

            int tenantProfileIndexStart = mHomeAccountSignedIn ? 1 : 0;
            int tenantProfileIndexEnd = mHomeAccountSignedIn ?
                    expectedTenantProfilesCount : expectedTenantProfilesCount - 1;
            for (int i = tenantProfileIndexStart; i <= tenantProfileIndexEnd; i++) {
                verifyAccountDetails(
                        mTestCaseData.userAccountsData.get(i),
                        account.getTenantProfiles().get(mTestCaseData.userAccountsData.get(i).tenantId));
            }
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    private void verifyAccountDetails(UserAccountData expectedUserAccountData, IAccount account) {
        assertEquals("TenantProfile: verify tenantId", expectedUserAccountData.tenantId, account.getTenantId());
        assertEquals("TenantProfile: verify id", expectedUserAccountData.localAccountId, account.getId());
        assertEquals("TenantProfile: verify authority", expectedUserAccountData.authority, account.getAuthority());
        assertEquals("TenantProfile: verify iss claim", expectedUserAccountData.authority, account.getClaims().get("iss"));
        assertEquals("TenantProfile: verify tid claim", expectedUserAccountData.tenantId, account.getClaims().get("tid"));
        assertEquals("TenantProfile: verify oid claim", expectedUserAccountData.localAccountId, account.getClaims().get("oid"));
    }

    @Test
    public void testRemoveAccountRemovesAllCredentialsFromMultipleCloudsWithSameHomeAccountId() {
        // arrange
        final IAccount accountToRemove = getAccount();

        // Perform acquire token for another user, to check later that cache records for this user
        // are not removed as part of removeAccount call for accountToRemove
        final String uid = randomUuidString();
        final String utid = randomUuidString();
        final String claims = "{\"uid\":\"" + uid + "\",\"utid\":\"" + utid + "\"}";

        final String clientInfo = new String(Base64.encode(claims.getBytes(), Base64.NO_PADDING));
        final UserAccountData otherUserData = new UserAccountData("https://login.microsoftonline.com", uid, utid);
        setupMockedHttpClientResponse(otherUserData, clientInfo);
        performInteractiveAcquireTokenCall("usr2@test.com", otherUserData.authority + "/organizations");
        mMockHttpClient.uninstall();
        final IAccount accountNotToRemove = getAccount();

        // act
        try {
            mMultipleAccountPCA.removeAccount(
                    accountToRemove,
                    new IMultipleAccountPublicClientApplication.RemoveAccountCallback() {
                        @Override
                        public void onRemoved() {
                        }

                        @Override
                        public void onError(@NonNull MsalException exception) {
                            fail("Failure in removeAccount: " + exception.getMessage());
                        }
                    });
        } catch (Exception e) {
            fail(e.getMessage());
        }

        RoboTestUtils.flushScheduler();

        // assert
        final IMultiTypeNameValueStorage sharedPreferences = mComponents.getStorageSupplier()
                .getEncryptedFileStore(SHARED_PREFERENCES_NAME);
        final Map<String, ?> cacheValues = sharedPreferences.getAll();

        if (cacheValues.size() == 5) {
            assertNotNull("Verify number of Cache records (AT, RT, IdToken, AccountRecord) for non removed account; check that one of them is the sha1-cleared flag",
                    cacheValues.get(SHA1_APPLICATION_IDENTIFIER_ACCESS_TOKEN_CLEARED));
        } else {
            assertEquals("Verify number of Cache records (AT, RT, IdToken, AccountRecord) for non removed account",
                    4, cacheValues.size());
        }

        for (String key : cacheValues.keySet()) {
            assertFalse("Verify cache record not found for homeAccountId of removed account",
                    key.contains(mTestCaseData.homeAccountId));
            assertTrue("Verify cache record found for account that was not removed",
                    key.contains(accountNotToRemove.getId()));
        }
    }

    @Test
    public void testAcquireTokenSilentReturnsAccessTokenForCrossCloudAccount() {
        // arrange
        final UserAccountData lastSignedInAccount =
                mTestCaseData.userAccountsData.get(mTestCaseData.userAccountsData.size() - 1);

        final SilentAuthenticationCallback silentAuthenticationCallback = new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                // verify access token value from the authentication result matches the expected
                // access token as set in the test case data
                assertEquals("Verify accessToken value from authenticationResult matches mocked access token",
                        lastSignedInAccount.getFakeAccessToken(), authenticationResult.getAccessToken());
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
            }
        };

        // act and assert
        try {
            mMultipleAccountPCA.acquireTokenSilentAsync(
                    getScopes(),
                    getAccount(),
                    lastSignedInAccount.authority +
                            "/" + lastSignedInAccount.tenantId,
                    silentAuthenticationCallback);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentReturnsAccessTokenForCrossCloudAccountRetrievedUsingGetAccount() {
        // arrange
        final IAccount[] accountUnderTest = {null};
        mMultipleAccountPCA.getAccount(mTestCaseData.homeAccountId, new IMultipleAccountPublicClientApplication.GetAccountCallback() {
            @Override
            public void onTaskCompleted(IAccount result) {
                accountUnderTest[0] = result;
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
            }
        });

        RoboTestUtils.flushScheduler();

        final SilentAuthenticationCallback silentAuthenticationCallback = new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                // verify access token value from the authentication result matches the expected
                // access token as set in the test case data
                assertEquals("Verify accessToken value from authenticationResult matches mocked access token",
                        mTestCaseData.userAccountsData.get(0).getFakeAccessToken(), authenticationResult.getAccessToken());
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
            }
        };

        // act and assert
        try {
            mMultipleAccountPCA.acquireTokenSilentAsync(
                    getScopes(),
                    accountUnderTest[0],
                    mTestCaseData.userAccountsData.get(0).authority +
                            "/" + mTestCaseData.userAccountsData.get(0).tenantId,
                    silentAuthenticationCallback);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentReturnsAccessTokenForCrossCloudAccountRetrievedUsingGetAccounts() {
        // arrange
        final List<IAccount> accountsUnderTest = new ArrayList<>();
        mMultipleAccountPCA.getAccounts(new IPublicClientApplication.LoadAccountsCallback() {
            @Override
            public void onTaskCompleted(List<IAccount> result) {
                accountsUnderTest.addAll(result);
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
            }
        });

        RoboTestUtils.flushScheduler();

        final SilentAuthenticationCallback silentAuthenticationCallback = new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                // verify access token value from the authentication result matches the expected
                // access token as set in the test case data
                assertEquals("Verify accessToken value from authenticationResult matches mocked access token",
                        mTestCaseData.userAccountsData.get(0).getFakeAccessToken(), authenticationResult.getAccessToken());
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
            }
        };

        // act and assert
        try {
            mMultipleAccountPCA.acquireTokenSilentAsync(
                    getScopes(),
                    accountsUnderTest.get(0),
                    mTestCaseData.userAccountsData.get(0).authority +
                            "/" + mTestCaseData.userAccountsData.get(0).tenantId,
                    silentAuthenticationCallback);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentReturnsAccessTokenForCrossCloudAccountWithSilentParameters() {
        // arrange
        final UserAccountData lastSignedInAccount =
                mTestCaseData.userAccountsData.get(mTestCaseData.userAccountsData.size() - 1);

        final SilentAuthenticationCallback silentAuthenticationCallback = new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                // verify access token value from the authentication result matches the expected
                // access token as set in the test case data
                assertEquals("Verify accessToken value from authenticationResult matches mocked access token",
                        lastSignedInAccount.getFakeAccessToken(), authenticationResult.getAccessToken());
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
            }
        };

        // act and assert
        try {
            final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                    .withScopes(Arrays.asList(getScopes()))
                    .forAccount(getAccount())
                    .fromAuthority(lastSignedInAccount.authority +
                            "/" + lastSignedInAccount.tenantId)
                    .forceRefresh(false)
                    .withClaims(null)
                    .withCallback(silentAuthenticationCallback)
                    .build();

            mMultipleAccountPCA.acquireTokenSilentAsync(silentParameters);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentReturnsAccessTokenForCrossCloudAccountRetrievedUsingGetAccountWithSilentParameters() {
        // arrange
        final IAccount[] accountUnderTest = {null};
        mMultipleAccountPCA.getAccount(mTestCaseData.homeAccountId, new IMultipleAccountPublicClientApplication.GetAccountCallback() {
            @Override
            public void onTaskCompleted(IAccount result) {
                accountUnderTest[0] = result;
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
            }
        });

        RoboTestUtils.flushScheduler();

        final SilentAuthenticationCallback silentAuthenticationCallback = new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                // verify access token value from the authentication result matches the expected
                // access token as set in the test case data
                assertEquals("Verify accessToken value from authenticationResult matches mocked access token",
                        mTestCaseData.userAccountsData.get(0).getFakeAccessToken(), authenticationResult.getAccessToken());
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
            }
        };

        // act and assert
        try {
            final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                    .withScopes(Arrays.asList(getScopes()))
                    .forAccount(accountUnderTest[0])
                    .fromAuthority(mTestCaseData.userAccountsData.get(0).authority +
                            "/" + mTestCaseData.userAccountsData.get(0).tenantId)
                    .forceRefresh(false)
                    .withClaims(null)
                    .withCallback(silentAuthenticationCallback)
                    .build();

            mMultipleAccountPCA.acquireTokenSilentAsync(silentParameters);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentReturnsAccessTokenForCrossCloudAccountRetrievedUsingGetAccountsWithSilentParameters() {
        // arrange
        final List<IAccount> accountsUnderTest = new ArrayList<>();
        mMultipleAccountPCA.getAccounts(new IPublicClientApplication.LoadAccountsCallback() {
            @Override
            public void onTaskCompleted(List<IAccount> result) {
                accountsUnderTest.addAll(result);
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
            }
        });

        RoboTestUtils.flushScheduler();

        final SilentAuthenticationCallback silentAuthenticationCallback = new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                // verify access token value from the authentication result matches the expected
                // access token as set in the test case data
                assertEquals("Verify accessToken value from authenticationResult matches mocked access token",
                        mTestCaseData.userAccountsData.get(0).getFakeAccessToken(), authenticationResult.getAccessToken());
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
            }
        };

        // act and assert
        try {
            final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                    .withScopes(Arrays.asList(getScopes()))
                    .forAccount(accountsUnderTest.get(0))
                    .fromAuthority(mTestCaseData.userAccountsData.get(0).authority +
                            "/" + mTestCaseData.userAccountsData.get(0).tenantId)
                    .forceRefresh(false)
                    .withClaims(null)
                    .withCallback(silentAuthenticationCallback)
                    .build();

            mMultipleAccountPCA.acquireTokenSilentAsync(silentParameters);

            mMultipleAccountPCA.acquireTokenSilentAsync(
                    getScopes(),
                    accountsUnderTest.get(0),
                    mTestCaseData.userAccountsData.get(0).authority +
                            "/" + mTestCaseData.userAccountsData.get(0).tenantId,
                    silentAuthenticationCallback);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        RoboTestUtils.flushScheduler();
    }

    private void performInteractiveAcquireTokenWithMockResponsesToSetupCache() {
        for (UserAccountData userData : mTestCaseData.userAccountsData) {
            setupMockedHttpClientResponse(userData, mTestCaseData.getRawClientInfo());
            performInteractiveAcquireTokenCall(mTestCaseData.userName, userData.authority + "/organizations");
            mMockHttpClient.uninstall();
            if (mTestCaseData.homeAccountId.equals(userData.localAccountId + "." + userData.tenantId)) {
                mHomeAccountSignedIn = true;
            }
        }
    }

    private void setupMockedHttpClientResponse(final UserAccountData userData, final String clientInfo) {
        final HttpResponse mockResponseForHomeTenant = MockServerResponse.getMockTokenSuccessResponse(
                userData.localAccountId,
                userData.tenantId,
                userData.authority,
                clientInfo,
                userData.getFakeAccessToken());
        final HttpRequestMatcher tokenRequestMatcher =
                HttpRequestMatcher.builder()
                        .urlPattern(userData.getUrlPatternMatchingAuthority())
                        .isPOST()
                        .build();
        mMockHttpClient.intercept(tokenRequestMatcher, mockResponseForHomeTenant);
    }

    @Override
    public String[] getScopes() {
        return USER_READ_SCOPE;
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
    }

    @Override
    public String getConfigFilePath() {
        return TestConstants.Configurations.MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH;
    }

    private static class TestCaseData {
        public final String userName;
        public final String homeAccountId;
        public final ArrayList<UserAccountData> userAccountsData;

        public TestCaseData(
                final String userName, final String homeAccountId, final ArrayList<UserAccountData> userAccountsData) {
            this.userName = userName;
            this.homeAccountId = homeAccountId;
            this.userAccountsData = userAccountsData;
        }

        public String getRawClientInfo() {
            final String uid = homeAccountId.split("\\.")[0];
            final String utid = homeAccountId.split("\\.")[1];
            final String claims = "{\"uid\":\"" + uid + "\",\"utid\":\"" + utid + "\"}";

            return new String(Base64.encode(claims.getBytes(
                    StandardCharsets.UTF_8), Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE));
        }
    }

    private static class UserAccountData {
        public final String authority;
        public final String localAccountId;
        public final String tenantId;

        public UserAccountData(final String authority, final String localAccountId, final String tenantId) {
            this.authority = authority;
            this.localAccountId = localAccountId;
            this.tenantId = tenantId;
        }

        public String getFakeAccessToken() {
            return "access_Token:" + authority + "/" + tenantId + "/" + localAccountId;
        }

        public Pattern getUrlPatternMatchingAuthority() {
            final String authorityPattern = "^" + authority.replace("/", "\\/") + "\\/.*";
            final Pattern pattern = Pattern.compile(authorityPattern);
            return pattern;
        }
    }
}
