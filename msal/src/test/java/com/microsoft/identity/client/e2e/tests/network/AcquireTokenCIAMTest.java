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
package com.microsoft.identity.client.e2e.tests.network;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static com.microsoft.identity.client.e2e.utils.RoboTestUtils.flushScheduler;
import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.CIAM_NO_PATH_CONFIG_FILE_PATH;
import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.CIAM_TENANT_DOMAIN_CONFIG_FILE_PATH;
import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.CIAM_TENANT_GUID_CONFIG_FILE_PATH;
import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.GRAPH_DEFAULT;
import static junit.framework.Assert.fail;

/**
 * Run all tests in the {@link AcquireTokenNetworkTest} class using CIAM
 */
public abstract class AcquireTokenCIAMTest extends AcquireTokenNetworkTest {

    private static final String CIAM_ISSUER = "ciamlogin.com";

    @Override
    public String[] getScopes() {
        return GRAPH_DEFAULT;
    }

    @Override
    public String getAuthority() {
        return AcquireTokenTestHelper.getAccount().getAuthority();
    }

    @Override
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.federationProvider = LabConstants.FederationProvider.CIAM;
        return query;
    }

    public static AuthenticationCallback successfulVerifyIssuerCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Assert.assertFalse(StringUtil.isEmpty(authenticationResult.getAccessToken()));

                Assert.assertNotNull(authenticationResult.getAccount().getClaims());
                String idTokenIssuer = (String) authenticationResult.getAccount().getClaims().get("iss");
                Assert.assertNotNull(idTokenIssuer);
                Assert.assertTrue(idTokenIssuer.contains(CIAM_ISSUER));
            }

            @Override
            public void onError(MsalException exception) {
                throw new AssertionError(exception);
            }

            @Override
            public void onCancel() {
                fail("User cancelled flow");
            }
        };
    }

    public static class CiamTenantGUID extends AcquireTokenCIAMTest {
        @Override
        public String getConfigFilePath() {
            return CIAM_TENANT_GUID_CONFIG_FILE_PATH;
        }

        @Test
        public void testAcquireTokenSuccessVerifyIssuer() {
            final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                    .startAuthorizationFromActivity(mActivity)
                    .withLoginHint(mUsername)
                    .withScopes(Arrays.asList(mScopes))
                    .withCallback(successfulVerifyIssuerCallback())
                    .build();


            mApplication.acquireToken(parameters);
            flushScheduler();
        }
    }

    public static class CiamTenantDomain extends AcquireTokenCIAMTest {
        @Override
        public String getConfigFilePath() {
            return CIAM_TENANT_DOMAIN_CONFIG_FILE_PATH;
        }

        @Test
        public void testAcquireTokenSuccessVerifyIssuer() {
            final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                    .startAuthorizationFromActivity(mActivity)
                    .withLoginHint(mUsername)
                    .withScopes(Arrays.asList(mScopes))
                    .withCallback(successfulVerifyIssuerCallback())
                    .build();


            mApplication.acquireToken(parameters);
            flushScheduler();
        }
    }

    public static class CiamTenantNoPath extends AcquireTokenCIAMTest {
        @Override
        public String getConfigFilePath() {
            return CIAM_NO_PATH_CONFIG_FILE_PATH;
        }

        @Test
        public void testAcquireTokenSuccessVerifyIssuer() {
            final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                    .startAuthorizationFromActivity(mActivity)
                    .withLoginHint(mUsername)
                    .withScopes(Arrays.asList(mScopes))
                    .withCallback(successfulVerifyIssuerCallback())
                    .build();


            mApplication.acquireToken(parameters);
            flushScheduler();
        }
    }
}
