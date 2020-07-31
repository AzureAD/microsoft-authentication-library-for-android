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

import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.e2e.shadows.ShadowMsalUtils;
import com.microsoft.identity.client.e2e.tests.PublicClientApplicationAbstractTest;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2StrategyParameters;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.SINGLE_ACCOUNT_DCF_TEST_CONFIG_FILE_PATH;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowMsalUtils.class})
public class GetDeviceCodeTest extends PublicClientApplicationAbstractTest {

    private MicrosoftStsAuthorizationRequest.Builder builder;
    private String urlBody;
    private OAuth2Strategy strategy;

    @Before
    public void setup() {
        super.setup();

        final PublicClientApplicationConfiguration config = mApplication.getConfiguration();
        urlBody = ((AzureActiveDirectoryAuthority) config.getAuthorities().get(0)).getAudience().getCloudUrl();
        builder = new MicrosoftStsAuthorizationRequest.Builder();
        builder.setClientId(config.getClientId())
                .setScope("user.read")
                .setState("State!");

        final OAuth2StrategyParameters options = new OAuth2StrategyParameters();
        strategy = new AzureActiveDirectoryOAuth2Strategy(
                new AzureActiveDirectoryOAuth2Configuration(),
                options
        );
    }

    @Override
    public String getConfigFilePath() {
        return SINGLE_ACCOUNT_DCF_TEST_CONFIG_FILE_PATH;
    }

    @Test
    public void testGetDeviceCodeSuccessResult() {
        final MicrosoftStsAuthorizationRequest authorizationRequest = builder.build();
        final AuthorizationResult authorizationResult = runGetDeviceCodeThread(authorizationRequest, urlBody);
        final MicrosoftStsAuthorizationResponse authorizationResponse = (MicrosoftStsAuthorizationResponse) authorizationResult.getAuthorizationResponse();

        Assert.assertTrue(authorizationResult.getSuccess());
        Assert.assertNotNull(authorizationResponse);

        Assert.assertNotNull(authorizationResponse.getDeviceCode());
        Assert.assertNotNull(authorizationResponse.getUserCode());
        Assert.assertNotNull(authorizationResponse.getMessage());
        Assert.assertNotNull(authorizationResponse.getInterval());
        Assert.assertNotNull(authorizationResponse.getExpiresIn());
        Assert.assertNotNull(authorizationResponse.getVerificationUri());

        Assert.assertNull(authorizationResult.getAuthorizationErrorResponse());
    }

    @Test
    public void testGetDeviceCodeFailureNoClientId() {
        final MicrosoftStsAuthorizationRequest authorizationRequest = builder.setClientId(null).build();
        final AuthorizationResult authorizationResult = runGetDeviceCodeThread(authorizationRequest, urlBody);
        final MicrosoftStsAuthorizationErrorResponse authorizationErrorResponse = (MicrosoftStsAuthorizationErrorResponse) authorizationResult.getAuthorizationErrorResponse();

        Assert.assertFalse(authorizationResult.getSuccess());
        Assert.assertNull(authorizationResult.getAuthorizationResponse());

        Assert.assertNotNull(authorizationErrorResponse);
        Assert.assertEquals("invalid_request", authorizationErrorResponse.getError());
    }

    @Test
    public void testGetDeviceCodeFailureNoScope() {
        final MicrosoftStsAuthorizationRequest authorizationRequest = builder.setScope(null).build();
        final AuthorizationResult authorizationResult = runGetDeviceCodeThread(authorizationRequest, urlBody);
        final MicrosoftStsAuthorizationErrorResponse authorizationErrorResponse = (MicrosoftStsAuthorizationErrorResponse) authorizationResult.getAuthorizationErrorResponse();

        Assert.assertFalse(authorizationResult.getSuccess());
        Assert.assertNull(authorizationResult.getAuthorizationResponse());

        Assert.assertNotNull(authorizationErrorResponse);
        Assert.assertEquals("invalid_request", authorizationErrorResponse.getError());
    }

    @Test
    public void testGetDeviceCodeFailureBadScope() {
        final MicrosoftStsAuthorizationRequest authorizationRequest = builder.setScope("/").build();
        final AuthorizationResult authorizationResult = runGetDeviceCodeThread(authorizationRequest, urlBody);
        final MicrosoftStsAuthorizationErrorResponse authorizationErrorResponse = (MicrosoftStsAuthorizationErrorResponse) authorizationResult.getAuthorizationErrorResponse();

        Assert.assertFalse(authorizationResult.getSuccess());
        Assert.assertNull(authorizationResult.getAuthorizationResponse());

        Assert.assertNotNull(authorizationErrorResponse);
        Assert.assertEquals("invalid_scope", authorizationErrorResponse.getError());
    }

    /**
     * Helper function to run getDeviceCode(). Catches exception
     * @param authorizationRequest request to send to getDeviceCode()
     * @param urlBody url to send to getDeviceCode()
     * @return authorizationResult from getDeviceCode()
     */
    private AuthorizationResult runGetDeviceCodeThread(final MicrosoftStsAuthorizationRequest authorizationRequest, final String urlBody) {
        AuthorizationResult authorizationResult = null;
        try {
            authorizationResult = strategy.getDeviceCode(authorizationRequest, urlBody);
        }
        catch (IOException e){
            Assert.fail();
        }

        return authorizationResult;
    }
}
