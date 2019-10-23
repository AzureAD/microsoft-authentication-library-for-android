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
package com.microsoft.identity.client;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.microsoft.identity.client.e2e.utils.RoboTestUtils;
import com.microsoft.identity.client.internal.controllers.OperationParametersAdapter;
import com.microsoft.identity.common.internal.cache.IAccountCredentialAdapter;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class OperationParametersTest {

    private static final String AAD_CP1_CONFIG_FILE = "src/test/res/raw/aad_capabilities_cp1.json";
    private static final String AAD_NONE_CONFIG_FILE = "src/test/res/raw/aad_capabilities_none.json";

    private Context mContext;
    private Activity mActivity;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mActivity = RoboTestUtils.getMockActivity(mContext);
    }

    @Test
    public void testAcquireTokenOperationWithClaimsWithCapabilities() {
        AcquireTokenOperationParameters operationParameters = OperationParametersAdapter.createAcquireTokenOperationParameters(getAcquireTokenParametersWithClaims(), getConfiguration(AAD_CP1_CONFIG_FILE), getCache());
        Assert.assertEquals(true, operationParameters.getForceRefresh());
    }

    @Test
    public void testAcquireTokenOperationWithClaimsWithoutCapabilities() {
        AcquireTokenOperationParameters operationParameters = OperationParametersAdapter.createAcquireTokenOperationParameters(getAcquireTokenParametersWithClaims(), getConfiguration(AAD_NONE_CONFIG_FILE), getCache());
        Assert.assertEquals(true, operationParameters.getForceRefresh());
    }

    @Test
    public void testAcquireTokenOperationWithoutClaimsWithCapabilities() {
        AcquireTokenOperationParameters operationParameters = OperationParametersAdapter.createAcquireTokenOperationParameters(getAcquireTokenParametersWithoutClaims(), getConfiguration(AAD_CP1_CONFIG_FILE), getCache());
        Assert.assertEquals(false, operationParameters.getForceRefresh());
    }

    @Test
    public void testAcquireTokenOperationWithoutClaimsWithoutCapabilities() {
        AcquireTokenOperationParameters operationParameters = OperationParametersAdapter.createAcquireTokenOperationParameters(getAcquireTokenParametersWithoutClaims(), getConfiguration(AAD_NONE_CONFIG_FILE), getCache());
        Assert.assertEquals(false, operationParameters.getForceRefresh());
    }

    @Test
    public void testAcquireTokenSilentOperationWithClaimsWithCapabilities() {
        AcquireTokenSilentOperationParameters operationParameters = OperationParametersAdapter.createAcquireTokenSilentOperationParameters(getAcquireTokenSilentParametersWithClaims(), getConfiguration(AAD_CP1_CONFIG_FILE), getCache());
        Assert.assertEquals(true, operationParameters.getForceRefresh());
    }

    @Test
    public void testAcquireTokenSilentOperationWithClaimsWithoutCapabilities() {
        AcquireTokenSilentOperationParameters operationParameters = OperationParametersAdapter.createAcquireTokenSilentOperationParameters(getAcquireTokenSilentParametersWithClaims(), getConfiguration(AAD_NONE_CONFIG_FILE), getCache());
        Assert.assertEquals(true, operationParameters.getForceRefresh());
    }

    @Test
    public void testAcquireTokenSilentOperationWithoutClaimsWithCapabilities() {
        AcquireTokenSilentOperationParameters operationParameters = OperationParametersAdapter.createAcquireTokenSilentOperationParameters(getAcquireTokenSilentParametersWithoutClaims(), getConfiguration(AAD_CP1_CONFIG_FILE), getCache());
        Assert.assertEquals(false, operationParameters.getForceRefresh());
    }

    @Test
    public void testAcquireTokenSilentOperationWithoutClaimsWithoutCapabilities() {
        AcquireTokenSilentOperationParameters operationParameters = OperationParametersAdapter.createAcquireTokenSilentOperationParameters(getAcquireTokenSilentParametersWithoutClaims(), getConfiguration(AAD_NONE_CONFIG_FILE), getCache());

        Assert.assertEquals(false, operationParameters.getForceRefresh());
    }

    private ClaimsRequest getAccessTokenClaimsRequest(@NonNull String claimName, @NonNull String claimValue) {
        ClaimsRequest cp1ClaimsRequest = new ClaimsRequest();
        RequestedClaimAdditionalInformation info = new RequestedClaimAdditionalInformation();
        info.setValues(new ArrayList<Object>(Arrays.asList(claimValue)));
        cp1ClaimsRequest.requestClaimInAccessToken(claimName, info);
        return cp1ClaimsRequest;
    }

    private AcquireTokenParameters getAcquireTokenParametersWithClaims() {
        AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .withClaims(getAccessTokenClaimsRequest("device_id", ""))
                .withScopes(new ArrayList<String>(Arrays.asList("User.Read")))
                .startAuthorizationFromActivity(mActivity)
                .build();

        return parameters;
    }

    private AcquireTokenParameters getAcquireTokenParametersWithoutClaims() {
        AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .withScopes(new ArrayList<String>(Arrays.asList("User.Read")))
                .startAuthorizationFromActivity(mActivity)
                .build();

        return parameters;
    }

    private AcquireTokenSilentParameters getAcquireTokenSilentParametersWithClaims() {
        AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
                .withClaims(getAccessTokenClaimsRequest("device_id", ""))
                .withScopes(new ArrayList<String>(Arrays.asList("User.Read")))
                .fromAuthority("https://login.microsoftonline.com/common")
                .build();

        return parameters;
    }

    private AcquireTokenSilentParameters getAcquireTokenSilentParametersWithoutClaims() {
        AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(new ArrayList<String>(Arrays.asList("User.Read")))
                .fromAuthority("https://login.microsoftonline.com/common")
                .build();

        return parameters;
    }

    private PublicClientApplicationConfiguration getConfiguration(String path) {
        return PublicClientApplicationConfigurationFactory.initializeConfiguration(mContext, getConfigFile(path));
    }

    private OAuth2TokenCache getCache() {
        return new TestOAuth2TokenCache(mContext, null, null);
    }

    private File getConfigFile(String path) {
        return new File(path);
    }

    private class TestOAuth2TokenCache extends MsalOAuth2TokenCache {

        /**
         * Constructor of MsalOAuth2TokenCache.
         *
         * @param context                  Context
         * @param accountCredentialCache   IAccountCredentialCache
         * @param accountCredentialAdapter IAccountCredentialAdapter
         */
        public TestOAuth2TokenCache(Context context, IAccountCredentialCache accountCredentialCache, IAccountCredentialAdapter accountCredentialAdapter) {
            super(context, accountCredentialCache, accountCredentialAdapter);
        }
    }

}
