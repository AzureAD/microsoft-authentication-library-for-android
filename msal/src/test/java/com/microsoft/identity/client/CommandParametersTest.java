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
import com.microsoft.identity.client.internal.CommandParametersAdapter;
import com.microsoft.identity.common.internal.cache.IAccountCredentialAdapter;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
public class CommandParametersTest {

    private static final String AAD_CP1_CONFIG_FILE = "src/test/res/raw/aad_capabilities_cp1.json";
    private static final String AAD_NONE_CONFIG_FILE = "src/test/res/raw/aad_capabilities_none.json";

    private Context mContext;
    private Activity mActivity;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        final Activity mockedActivity = Mockito.mock(Activity.class);
        Mockito.when(mockedActivity.getApplicationContext()).thenReturn(mContext);

        mActivity = mockedActivity;
    }


    @Test
    public void testAcquireTokenSilentOperationWithClaimsWithCapabilities() {
        SilentTokenCommandParameters commandParameters = CommandParametersAdapter.createSilentTokenCommandParameters(getConfiguration(AAD_CP1_CONFIG_FILE), getCache(), getAcquireTokenSilentParametersWithClaims());
        Assert.assertEquals(true, commandParameters.isForceRefresh());
    }

    @Test
    public void testAcquireTokenSilentOperationWithClaimsWithoutCapabilities() {
        SilentTokenCommandParameters commandParameters = CommandParametersAdapter.createSilentTokenCommandParameters(getConfiguration(AAD_NONE_CONFIG_FILE), getCache(), getAcquireTokenSilentParametersWithClaims());
        Assert.assertEquals(true, commandParameters.isForceRefresh());
    }

    @Test
    public void testAcquireTokenSilentOperationWithoutClaimsWithCapabilities() {
        SilentTokenCommandParameters commandParameters = CommandParametersAdapter.createSilentTokenCommandParameters(getConfiguration(AAD_CP1_CONFIG_FILE), getCache(), getAcquireTokenSilentParametersWithoutClaims());
        Assert.assertEquals(false, commandParameters.isForceRefresh());
    }

    @Test
    public void testAcquireTokenSilentOperationWithoutClaimsWithoutCapabilities() {
        SilentTokenCommandParameters commandParameters = CommandParametersAdapter.createSilentTokenCommandParameters(getConfiguration(AAD_NONE_CONFIG_FILE), getCache(), getAcquireTokenSilentParametersWithoutClaims());

        Assert.assertEquals(false, commandParameters.isForceRefresh());
    }

    @Test
    public void testAcquireTokenOperationWithClaimsWithCapabilities() {
        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter.createInteractiveTokenCommandParameters(getConfiguration(AAD_CP1_CONFIG_FILE), getCache(), getAcquireTokenParametersWithClaims());
        Assert.assertEquals(true, commandParameters.isForceRefresh());
    }

    @Test
    public void testAcquireTokenOperationWithClaimsWithoutCapabilities() {
        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter.createInteractiveTokenCommandParameters(getConfiguration(AAD_NONE_CONFIG_FILE), getCache(), getAcquireTokenParametersWithClaims());
        Assert.assertEquals(true, commandParameters.isForceRefresh());
    }

    @Test
    public void testAcquireTokenOperationWithoutClaimsWithCapabilities() {
        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter.createInteractiveTokenCommandParameters(getConfiguration(AAD_CP1_CONFIG_FILE), getCache(), getAcquireTokenParametersWithoutClaims());
        Assert.assertEquals(false, commandParameters.isForceRefresh());
    }

    @Test
    public void testAcquireTokenOperationWithoutClaimsWithoutCapabilities() {
        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter.createInteractiveTokenCommandParameters(getConfiguration(AAD_NONE_CONFIG_FILE), getCache(), getAcquireTokenParametersWithoutClaims());
        Assert.assertEquals(false, commandParameters.isForceRefresh());
    }

    @Test
    public void testAcquireTokenOperationWithoutCorrelationId() {
        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter.createInteractiveTokenCommandParameters(getConfiguration(AAD_CP1_CONFIG_FILE), getCache(), getAcquireTokenParametersWithoutCorrelationId());
        Assert.assertNull(commandParameters.getCorrelationId());
    }

    @Test
    public void testAcquireTokenOperationWithCorrelationId() {
        final UUID correlationId = UUID.randomUUID();
        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter.createInteractiveTokenCommandParameters(getConfiguration(AAD_NONE_CONFIG_FILE), getCache(), getAcquireTokenParametersWithCorrelationId(correlationId));
        Assert.assertNotNull(commandParameters.getCorrelationId());
        Assert.assertEquals(correlationId.toString(), commandParameters.getCorrelationId());
    }

    @Test
    public void testAcquireTokenSilentOperationWithoutCorrelationId() {
        SilentTokenCommandParameters commandParameters = CommandParametersAdapter.createSilentTokenCommandParameters(getConfiguration(AAD_CP1_CONFIG_FILE), getCache(), getAcquireTokenSilentParametersWithoutCorrelationId());
        Assert.assertNull(commandParameters.getCorrelationId());
    }

    @Test
    public void testAcquireTokenSilentOperationWithCorrelationId() {
        final UUID correlationId = UUID.randomUUID();
        SilentTokenCommandParameters commandParameters = CommandParametersAdapter.createSilentTokenCommandParameters(getConfiguration(AAD_NONE_CONFIG_FILE), getCache(), getAcquireTokenSilentParametersWithCorrelationId(correlationId));
        Assert.assertNotNull(commandParameters.getCorrelationId());
        Assert.assertEquals(correlationId.toString(), commandParameters.getCorrelationId());
    }

    private ClaimsRequest getAccessTokenClaimsRequest(@NonNull String claimName, @NonNull String claimValue) {
        ClaimsRequest cp1ClaimsRequest = new ClaimsRequest();
        RequestedClaimAdditionalInformation info = new RequestedClaimAdditionalInformation();
        info.setValues(new ArrayList<Object>(Arrays.asList(claimValue)));
        cp1ClaimsRequest.requestClaimInAccessToken(claimName, info);
        return cp1ClaimsRequest;
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

    private AcquireTokenSilentParameters getAcquireTokenSilentParametersWithoutCorrelationId() {
        AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(new ArrayList<String>(Arrays.asList("User.Read")))
                .fromAuthority("https://login.microsoftonline.com/common")
                .build();

        return parameters;
    }

    private AcquireTokenSilentParameters getAcquireTokenSilentParametersWithCorrelationId(final UUID correlationId) {
        AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(new ArrayList<String>(Arrays.asList("User.Read")))
                .fromAuthority("https://login.microsoftonline.com/common")
                .withCorrelationId(correlationId)
                .build();

        return parameters;
    }

    private AcquireTokenParameters getAcquireTokenParametersWithoutCorrelationId() {
        AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .withClaims(getAccessTokenClaimsRequest("device_id", ""))
                .withScopes(new ArrayList<String>(Arrays.asList("User.Read")))
                .startAuthorizationFromActivity(mActivity)
                .build();

        return parameters;
    }

    private AcquireTokenParameters getAcquireTokenParametersWithCorrelationId(final UUID correlationId) {
        AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .withClaims(getAccessTokenClaimsRequest("device_id", ""))
                .withScopes(new ArrayList<String>(Arrays.asList("User.Read")))
                .startAuthorizationFromActivity(mActivity)
                .withCorrelationId(correlationId)
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
