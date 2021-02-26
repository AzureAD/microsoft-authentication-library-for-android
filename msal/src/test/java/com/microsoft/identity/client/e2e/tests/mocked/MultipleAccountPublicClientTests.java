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

import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.MultiTenantAccount;
import com.microsoft.identity.client.RoboTestCacheHelper;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.e2e.shadows.ShadowAuthorityForMockHttpResponse;
import com.microsoft.identity.client.e2e.shadows.ShadowMsalUtils;
import com.microsoft.identity.client.e2e.shadows.ShadowOpenIdProviderConfigurationClient;
import com.microsoft.identity.client.e2e.shadows.ShadowStorageHelper;
import com.microsoft.identity.client.e2e.tests.AcquireTokenAbstractTest;
import com.microsoft.identity.client.e2e.utils.RoboTestUtils;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.internal.AsyncResult;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.net.HttpClient;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.result.ResultFuture;
import com.microsoft.identity.internal.testutils.MockHttpClient;
import com.microsoft.identity.internal.testutils.TestConstants;
import com.microsoft.identity.internal.testutils.TestUtils;
import com.microsoft.identity.internal.testutils.mocks.MockServerResponse;
import com.microsoft.identity.internal.testutils.mocks.MockTokenCreator;
import com.microsoft.identity.internal.testutils.mocks.MockTokenResponse;
import com.microsoft.identity.internal.testutils.shadows.ShadowHttpClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper.getAccount;
import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.USER_READ_SCOPE;
import static com.microsoft.identity.internal.testutils.TestUtils.getSharedPreferences;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(shadows = {
        ShadowStorageHelper.class,
        ShadowAuthorityForMockHttpResponse.class,
        ShadowMsalUtils.class,
        ShadowHttpClient.class,
        ShadowOpenIdProviderConfigurationClient.class
}, sdk = 28)
public class MultipleAccountPublicClientTests extends AcquireTokenAbstractTest {
    private TestCaseData testCaseData;
    private IMultipleAccountPublicClientApplication mMultipleAccountPCA;
    private boolean homeAccountSignedIn = false;

    public MultipleAccountPublicClientTests(final String name, final TestCaseData testCaseData) {
        this.testCaseData = testCaseData;
    }

    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
    public static Iterable<Object[]> getTestDataForInteractiveAcquireTokenCallToSetupCache() {
        final String userName = "testUser@multipleAccountPCATests.onmicrosoft.com";
        final String uid = UUID.randomUUID().toString();
        final String utid = UUID.randomUUID().toString();
        return Arrays.asList(
                new Object[]{
                    "Cache setup by performing acquireToken requests for Home account and 1 Guest account in Foreign cloud",
                    new TestCaseData(
                        userName,
                        uid + "." + utid,
                        new ArrayList<UserAccountData>(Arrays.asList(
                            new UserAccountData("https://login.microsoftonline.com", uid, utid),
                            new UserAccountData("https://login.microsoftonline.us", UUID.randomUUID().toString(), UUID.randomUUID().toString())
                        ))
                )},
                new Object[]{
                    "Cache setup by performing acquireToken requests for Home account and 2 Guest accounts in 2 separate Foreign cloud ",
                    new TestCaseData(
                        userName,
                        uid + "." + utid,
                        new ArrayList<>(Arrays.asList(
                            new UserAccountData("https://login.microsoftonline.com", uid, utid),
                            new UserAccountData("https://login.microsoftonline.de", UUID.randomUUID().toString(), UUID.randomUUID().toString()),
                            new UserAccountData("https://login.microsoftonline.us", UUID.randomUUID().toString(), UUID.randomUUID().toString())
                        ))
                )},
                new Object[]{
                    "Cache setup by performing acquireToken requests for Home account and 2 Guest accounts in 1 Foreign cloud ",
                    new TestCaseData(
                        userName,
                        uid + "." + utid,
                        new ArrayList<>(Arrays.asList(
                            new UserAccountData("https://login.microsoftonline.com", uid, utid),
                            new UserAccountData("https://login.microsoftonline.us", UUID.randomUUID().toString(), UUID.randomUUID().toString()),
                            new UserAccountData("https://login.microsoftonline.us", UUID.randomUUID().toString(), UUID.randomUUID().toString())
                        ))
                )},
                new Object[]{
                    "Cache setup by performing acquireToken requests for 2 Guest accounts in 1 Foreign cloud ",
                    new TestCaseData(
                            userName,
                            uid + "." + utid,
                            new ArrayList<>(Arrays.asList(
                                    new UserAccountData("https://login.microsoftonline.us", UUID.randomUUID().toString(), UUID.randomUUID().toString()),
                                    new UserAccountData("https://login.microsoftonline.us", UUID.randomUUID().toString(), UUID.randomUUID().toString())
                            ))
                    )},
                new Object[]{
                    "Cache setup by performing acquireToken requests for Home account ",
                    new TestCaseData(
                            userName,
                            uid + "." + utid,
                            new ArrayList<>(Arrays.asList(
                                    new UserAccountData("https://login.microsoftonline.com", uid, utid)
                            ))
                    )},
                new Object[]{
                        "Cache setup by performing acquireToken requests for Home account and 1 guest tenant in home cloud ",
                        new TestCaseData(
                                userName,
                                uid + "." + utid,
                                new ArrayList<>(Arrays.asList(
                                        new UserAccountData("https://login.microsoftonline.com", uid, utid),
                                        new UserAccountData("https://login.microsoftonline.com", UUID.randomUUID().toString(), UUID.randomUUID().toString())
                                ))
                        )}
        );
    }

    @Before
    public void setup() {
        super.setup();
        mMultipleAccountPCA = (IMultipleAccountPublicClientApplication) mApplication;
        performInteractiveAcquireTokenWithMockResponsesToSetupCache();
    }

    @Test
    public void testGetAccountsReturnsSingleAccountObjectForAccountRecordsFromMultipleCloudsWithSameHomeAccountId()
    {
        // arrange
        final ResultFuture<AsyncResult<List<IAccount>>> future = new ResultFuture<>();

        // act
        mMultipleAccountPCA.getAccounts(new IPublicClientApplication.LoadAccountsCallback() {
            @Override
            public void onTaskCompleted(List<IAccount> result) {
                future.setResult(new AsyncResult<List<IAccount>>(result, null));
            }

            @Override
            public void onError(MsalException exception) {
                future.setResult(new AsyncResult<List<IAccount>>(null, exception));
            }
        });

        RoboTestUtils.flushScheduler();

        // assert
        int expectedTenantProfilesCount = homeAccountSignedIn ? testCaseData.userAccountsData.size() - 1 : testCaseData.userAccountsData.size();
        try {
            final AsyncResult<List<IAccount>> result = future.get();

            if (result.getSuccess()) {
                assertEquals("number of accounts", 1, result.getResult().size());
                MultiTenantAccount account = (MultiTenantAccount) result.getResult().get(0);
                assertEquals("number of tenant profiles", expectedTenantProfilesCount , account.getTenantProfiles().size());
                assertTrue(testCaseData.homeAccountId.contains(account.getId()));
                if(homeAccountSignedIn) {
                    assertNotNull("account.getClaims()", account.getClaims());
                    assertNotEquals("claims count",0, account.getClaims().size());
                    verifyAccountDetails(testCaseData.userAccountsData.get(0), account);
                }

                int tenantProfileIndexStart = homeAccountSignedIn? 1: 0;
                int tenantProfileIndexEnd = homeAccountSignedIn? expectedTenantProfilesCount: expectedTenantProfilesCount -1;
                for(int i = tenantProfileIndexStart; i <= tenantProfileIndexEnd; i++){
                    verifyAccountDetails(testCaseData.userAccountsData.get(i), account.getTenantProfiles().get(testCaseData.userAccountsData.get(i).tenantId));
                }
            } else {
                fail(result.getException().getMessage());
            }
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    private void verifyAccountDetails(UserAccountData expectedUserAccountData, IAccount account) {
        assertEquals("account.getTenantId()", expectedUserAccountData.tenantId, account.getTenantId());
        assertEquals("account.getId()", expectedUserAccountData.localAccountId, account.getId());
        assertEquals("account.getAuthority()", expectedUserAccountData.cloud, account.getAuthority());
        assertEquals("iss claim", expectedUserAccountData.cloud, account.getClaims().get("iss"));
        assertEquals("tid claim", expectedUserAccountData.tenantId, account.getClaims().get("tid"));
        assertEquals("oid claim", expectedUserAccountData.localAccountId, account.getClaims().get("oid"));
    }

    @Test
    public void testRemoveAccountRemovesAllCredentialsFromMultipleCloudsWithSameHomeAccountId()
    {
        // arrange
        // add mock record in cache which has different home account id than the account to be removed
        ICacheRecord cacheRecord = null;
        final TokenResponse tokenResponse = MockTokenResponse.getMockSuccessTokenResponse();

        try {
            cacheRecord = RoboTestCacheHelper.saveTokens(tokenResponse, mMultipleAccountPCA);
        } catch (ClientException e) {
            fail("Unable to save tokens to cache: " + e.getMessage());
        }

        final ResultFuture<Boolean> future = new ResultFuture<>();

        // act
        try {
            mMultipleAccountPCA.removeAccount(getAccount(), new IMultipleAccountPublicClientApplication.RemoveAccountCallback() {
                @Override
                public void onRemoved() {
                    future.setResult(true);
                }

                @Override
                public void onError(@NonNull MsalException exception) {
                    future.setException(exception);
                }
            });
        } catch (Exception e) {
            fail(e.getMessage());
        }

        RoboTestUtils.flushScheduler();

        // assert
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME);
        final Map<String, ?> cacheValues = sharedPreferences.getAll();
        for (String key: cacheValues.keySet()) {
            assertFalse("Cache record found for homeAccountId of removed account", key.contains(testCaseData.homeAccountId));
            assertTrue("Cache record found for other homeAccountId", key.contains(cacheRecord.getAccount().getHomeAccountId()));
        }
        assertEquals("Cache Values count", 4, cacheValues.size());
    }

    @Test
    public void testAcquireTokenReturnsAccessTokenForCrossCloudAccount()
    {
        // arrange
        final ResultFuture<IAuthenticationResult> future = new ResultFuture<>();

        // act
        try {
            mMultipleAccountPCA.acquireTokenSilentAsync
                    (getScopes(),
                    getAccount(),
                    testCaseData.userAccountsData.get(testCaseData.userAccountsData.size() -1).cloud + "/" + testCaseData.userAccountsData.get(testCaseData.userAccountsData.size() -1).tenantId,
                    new SilentAuthenticationCallback() {
                        @Override
                        public void onSuccess(IAuthenticationResult authenticationResult) {
                            future.setResult(authenticationResult);
                        }

                        @Override
                        public void onError(MsalException exception) {
                            future.setException(exception);
                        }
                    });
        } catch (Exception e) {
            fail(e.getMessage());
        }

        RoboTestUtils.flushScheduler();

        // assert
        IAuthenticationResult authenticationResult = null;
        try {
            authenticationResult = future.get();
            assertNotNull("authenticationResult from acquireTokeSilent call", authenticationResult);
            assertEquals("accessToken value", testCaseData.userAccountsData.get(testCaseData.userAccountsData.size() -1).getFakeAccessToken(), authenticationResult.getAccessToken());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testAcquireTokenReturnsAccessTokenForCrossCloudAccountRetrievedUsingGetAccount()
    {
        // arrange
        final IAccount[] accountUnderTest = {null};
        mMultipleAccountPCA.getAccount(testCaseData.homeAccountId, new IMultipleAccountPublicClientApplication.GetAccountCallback() {
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

        // act
        final ResultFuture<IAuthenticationResult> future = new ResultFuture<>();
        try {
            mMultipleAccountPCA.acquireTokenSilentAsync
                    (getScopes(),
                            accountUnderTest[0],
                            testCaseData.userAccountsData.get(0).cloud + "/" + testCaseData.userAccountsData.get(0).tenantId,
                            new SilentAuthenticationCallback() {
                                @Override
                                public void onSuccess(IAuthenticationResult authenticationResult) {
                                    future.setResult(authenticationResult);
                                }

                                @Override
                                public void onError(MsalException exception) {
                                    future.setException(exception);
                                }
                            });
        } catch (Exception e) {
            fail(e.getMessage());
        }

        RoboTestUtils.flushScheduler();

        // assert
        IAuthenticationResult authenticationResult = null;
        try {
            authenticationResult = future.get();
            assertNotNull("authenticationResult from acquireTokeSilent call", authenticationResult);
            assertEquals("accessToken value", testCaseData.userAccountsData.get(0).getFakeAccessToken(), authenticationResult.getAccessToken());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testAcquireTokenReturnsAccessTokenForCrossCloudAccountRetrievedUsingGetAccounts()
    {
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

        // act
        final ResultFuture<IAuthenticationResult> future = new ResultFuture<>();
        try {
            mMultipleAccountPCA.acquireTokenSilentAsync
                    (getScopes(),
                            accountsUnderTest.get(0),
                            testCaseData.userAccountsData.get(0).cloud + "/" + testCaseData.userAccountsData.get(0).tenantId,
                            new SilentAuthenticationCallback() {
                                @Override
                                public void onSuccess(IAuthenticationResult authenticationResult) {
                                    future.setResult(authenticationResult);
                                }

                                @Override
                                public void onError(MsalException exception) {
                                    future.setException(exception);
                                }
                            });
        } catch (Exception e) {
            fail(e.getMessage());
        }

        RoboTestUtils.flushScheduler();

        // assert
        IAuthenticationResult authenticationResult = null;
        try {
            authenticationResult = future.get();
            assertNotNull("authenticationResult from acquireTokeSilent call", authenticationResult);
            assertEquals("accessToken value", testCaseData.userAccountsData.get(0).getFakeAccessToken(), authenticationResult.getAccessToken());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private void performInteractiveAcquireTokenWithMockResponsesToSetupCache() {
        TestUtils.clearCache(SHARED_PREFERENCES_NAME);
        MockHttpClient.reset();
        MockHttpClient.setHttpResponse(MockServerResponse.getMockCloudDiscoveryResponse(), HttpClient.HttpMethod.GET, MockTokenCreator.CLOUD_DISCOVERY_ENDPOINT_REGEX);
        for (UserAccountData userData: testCaseData.userAccountsData) {
            HttpResponse mockResponseForHomeTenant = MockServerResponse.getMockTokenSuccessResponse(
                    userData.localAccountId,
                    userData.tenantId,
                    userData.cloud,
                    testCaseData.getRawClientInfo(),
                    userData.getFakeAccessToken());
            MockHttpClient.setHttpResponse(mockResponseForHomeTenant, HttpClient.HttpMethod.POST, userData.cloud + "/.*");
            performInteractiveAcquireTokenCall(testCaseData.userName, userData.cloud + "/organizations");
            if(testCaseData.homeAccountId.equals(userData.localAccountId + "." + userData.tenantId)){
                homeAccountSignedIn = true;
            }
        }
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

        public TestCaseData(final String userName, String homeAccountId, ArrayList<UserAccountData> userAccountsData) {
            this.userName = userName;
            this.homeAccountId = homeAccountId;
            this.userAccountsData = userAccountsData;
        }
        public String getRawClientInfo() {
            final String uid = homeAccountId.split("\\.")[0];
            final String utid = homeAccountId.split("\\.")[1];
            final String claims = "{\"uid\":\"" + uid + "\",\"utid\":\"" + utid + "\"}";

            return new String(Base64.encode(claims.getBytes(
                    Charset.forName("UTF-8")), Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE));
        }

    }

    private static class UserAccountData
    {
        public final String cloud;
        public final String localAccountId;

        public final String tenantId;

        public UserAccountData(final String cloud, final String localAccountId, final String tenantId){
            this.cloud = cloud;
            this.localAccountId = localAccountId;
            this.tenantId = tenantId;
        }
        public String getFakeAccessToken()
        {
            return "access_Token:" + cloud + "/" + tenantId + "/" + localAccountId;
        }
    }
}
