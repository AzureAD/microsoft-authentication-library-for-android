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
package com.microsoft.identity.client.e2e.tests.network;

import static com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper.getAccount;
import static com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper.successfulInteractiveCallback;
import static com.microsoft.identity.client.e2e.utils.RoboTestUtils.flushScheduler;
import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH;
import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.AD_GRAPH_USER_READ_SCOPE;
import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.MS_GRAPH_USER_READ_SCOPE;
import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.OFFICE_USER_READ_SCOPE;
import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.USER_READ_SCOPE;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.e2e.rules.NetworkTestsRuleChain;
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager;
import com.microsoft.identity.client.e2e.shadows.ShadowAuthority;
import com.microsoft.identity.client.e2e.shadows.ShadowPublicClientApplicationConfiguration;
import com.microsoft.identity.client.e2e.tests.AcquireTokenAbstractTest;
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowAndroidSdkStorageEncryptionManager.class,
            ShadowAuthority.class,
            ShadowPublicClientApplicationConfiguration.class
        })
public class MultiAccountAndResourceAcquireTokenNetworkTests extends AcquireTokenAbstractTest {

    @Rule public TestRule rule = NetworkTestsRuleChain.getRule();

    @Override
    public String[] getScopes() {
        return USER_READ_SCOPE;
    }

    @Override
    public String getAuthority() {
        return AcquireTokenTestHelper.getAccount().getAuthority();
    }

    @Override
    public String getConfigFilePath() {
        return MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH;
    }

    @Test // test that accounts belonging to multiple clouds can live together in the app
    public void testAcquireTokenAndSilentWithMultipleCloudAccountsSuccess() {

        final LabUserQuery[] queries =
                new LabUserQuery[] {
                    new AcquireTokenAADTest.AzureWorldWideCloudUser().getLabUserQuery(),
                    new AcquireTokenAADTest.AzureGermanyCloudUser().getLabUserQuery(),
                    new AcquireTokenAADTest.AzureUsGovCloudUser().getLabUserQuery()
                };

        final IAccount[] accounts = new IAccount[queries.length];

        // perform interactive call for each account
        for (int i = 0; i < queries.length; i++) {
            final String username = LabUserHelper.loadUserForTest(queries[i]);
            performInteractiveAcquireTokenCall(username);
            accounts[i] = getAccount();
        }

        // perform silent call for each account
        for (final IAccount account : accounts) {
            performSilentAcquireTokenCall(account);
        }
    }

    @Test // test that we can use mrrt to get a token silently for other resources
    public void testAcquireTokenSilentUsingMrrtSuccess() {
        final LabUserQuery query =
                new AcquireTokenAADTest.AzureWorldWideCloudUser().getLabUserQuery();

        final String username = LabUserHelper.loadUserForTest(query);

        final AcquireTokenParameters parameters =
                new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(mActivity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(USER_READ_SCOPE))
                        .withCallback(successfulInteractiveCallback())
                        .build();

        mApplication.acquireToken(parameters);
        flushScheduler();

        performSilentAcquireTokenCall(USER_READ_SCOPE);
        performSilentAcquireTokenCall(MS_GRAPH_USER_READ_SCOPE);
        performSilentAcquireTokenCall(OFFICE_USER_READ_SCOPE);
        performSilentAcquireTokenCall(AD_GRAPH_USER_READ_SCOPE);
    }
}
