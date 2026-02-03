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
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaim;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.microsoft.identity.client.internal.CommandParametersAdapter;
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.java.cache.IAccountCredentialAdapter;
import com.microsoft.identity.common.java.cache.IAccountCredentialCache;
import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.java.commands.parameters.DeviceCodeFlowCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.constants.FidoConstants;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitCodeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitPasswordCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInWithContinuationTokenCommandParameters;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;
import com.microsoft.identity.common.java.ui.PreferredAuthMethod;
import com.microsoft.identity.msal.R;
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration;
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfigurationFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
public class CommandParametersTest {

    private static final String AAD_CP1_CONFIG_FILE = "src/test/res/raw/aad_capabilities_cp1.json";
    private static final String AAD_NONE_CONFIG_FILE = "src/test/res/raw/aad_capabilities_none.json";
    private static final String WEBAUTHN_CAPABLE_CONFIG_FILE = "src/test/res/raw/webauthn_capable.json";

    private Context mContext;
    private Activity mActivity;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mActivity = Mockito.mock(Activity.class);
        Mockito.when(mActivity.getApplicationContext()).thenReturn(mContext);
    }



    @Test
    @Config(sdk=28)
    public void testPasskeyHeader_NotAddedWhenAuthorizationAgentIsDefault() throws ClientException {
        PublicClientApplicationConfiguration mockConfig = Mockito.spy(getConfiguration(WEBAUTHN_CAPABLE_CONFIG_FILE));
        Mockito.when(mockConfig.getAuthorizationAgent()).thenReturn(AuthorizationAgent.DEFAULT);

        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter
                .createInteractiveTokenCommandParameters(
                        mockConfig,
                        getCache(),
                        getAcquireTokenParametersWithClaims()
                );
        Assert.assertFalse(commandParameters
                .getRequestHeaders()
                .containsKey(FidoConstants.PASSKEY_PROTOCOL_HEADER_NAME)
        );
    }

    @Test
    @Config(sdk=28)
    public void testPasskeyHeader_NotAddedWhenWebAuthnNotCapable() throws ClientException {
        PublicClientApplicationConfiguration mockConfig = Mockito.spy(getConfiguration(WEBAUTHN_CAPABLE_CONFIG_FILE));
        Mockito.when(mockConfig.isWebauthnCapable()).thenReturn(false);

        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter
                .createInteractiveTokenCommandParameters(
                        mockConfig,
                        getCache(),
                        getAcquireTokenParametersWithClaims()
                );
        Assert.assertFalse(commandParameters
                .getRequestHeaders()
                .containsKey(FidoConstants.PASSKEY_PROTOCOL_HEADER_NAME)
        );
    }

    @Test
    @Config(sdk=28)
    public void testPasskeyHeader_NotAddedWhenWebAuthnVersionUnsupported() throws ClientException {
        PublicClientApplicationConfiguration mockConfig = Mockito.spy(getConfiguration(WEBAUTHN_CAPABLE_CONFIG_FILE));
        Mockito.when(mockConfig.getWebauthnVersion()).thenReturn("3.0");

        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter
                .createInteractiveTokenCommandParameters(
                        mockConfig,
                        getCache(),
                        getAcquireTokenParametersWithClaims()
                );
        Assert.assertFalse(commandParameters
                .getRequestHeaders()
                .containsKey(FidoConstants.PASSKEY_PROTOCOL_HEADER_NAME)
        );
    }

    @Test
    @Config(sdk=28)
    public void testPasskeyHeader_NotAddedWhenWebAuthnVersion1_0() throws ClientException {
        PublicClientApplicationConfiguration mockConfig = Mockito.spy(getConfiguration(WEBAUTHN_CAPABLE_CONFIG_FILE));
        Mockito.when(mockConfig.getWebauthnVersion()).thenReturn("1.0");

        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter
                .createInteractiveTokenCommandParameters(
                        mockConfig,
                        getCache(),
                        getAcquireTokenParametersWithClaims()
                );
        Assert.assertTrue(commandParameters
                .getRequestHeaders()
                .containsKey(FidoConstants.PASSKEY_PROTOCOL_HEADER_NAME)
        );
        Assert.assertEquals(
                FidoConstants.PASSKEY_PROTOCOL_HEADER_AUTH_ONLY,
                commandParameters.getRequestHeaders().get(FidoConstants.PASSKEY_PROTOCOL_HEADER_NAME)
        );
    }


    @Test
    public void testCreateSignInStartCommandParameters_CommandParamsContainsExpectedParams() throws ClientException {
        List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        String username = "username";
        char[] pwd = "example".toCharArray();
        List<String> scopes = new ArrayList<>(Collections.singletonList("User.Read"));
        ClaimsRequest claimsRequest = ClaimsRequest.getClaimsRequestFromJsonString("{\"access_token\":{\"acrs\":{\"essential\":true,\"value\":\"c4\"}}}");
        NativeAuthPublicClientApplicationConfiguration configuration = new NativeAuthPublicClientApplicationConfiguration();
        configuration.setChallengeTypes(challengeTypes);
        configuration.setClientId("clientId");
        configuration.setAppContext(mContext);
        configuration.setPowerOptCheckEnabled(false);

        final SignInStartCommandParameters commandParameters = CommandParametersAdapter.createSignInStartCommandParameters(
                configuration,
                null,
                username,
                pwd,
                scopes,
                claimsRequest
        );
        Assert.assertEquals(commandParameters.claimsRequestJson, ClaimsRequest.getJsonStringFromClaimsRequest(claimsRequest));
        Assert.assertEquals(commandParameters.password, pwd);
        Assert.assertEquals(commandParameters.username, username);
        Assert.assertEquals(commandParameters.scopes, scopes);
        Assert.assertEquals(commandParameters.challengeType, challengeTypes);
    }

    @Test
    public void createSignInSubmitCodeCommandParameters_CommandParamsContainsExpectedParams() throws ClientException {
        List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        String code = "123456";
        String continuationToken = "continuationToken";
        String correlationId = UUID.randomUUID().toString();
        List<String> scopes = new ArrayList<>(Collections.singletonList("User.Read"));
        String claimsRequestJson = "{\"access_token\":{\"acrs\":{\"essential\":true,\"value\":\"c4\"}}}";
        NativeAuthPublicClientApplicationConfiguration configuration = new NativeAuthPublicClientApplicationConfiguration();
        configuration.setChallengeTypes(challengeTypes);
        configuration.setClientId("clientId");
        configuration.setAppContext(mContext);
        configuration.setPowerOptCheckEnabled(false);

        final SignInSubmitCodeCommandParameters commandParameters = CommandParametersAdapter.createSignInSubmitCodeCommandParameters(
                configuration,
                null,
                code,
                continuationToken,
                correlationId,
                scopes,
                claimsRequestJson
        );
        Assert.assertEquals(commandParameters.claimsRequestJson, claimsRequestJson);
        Assert.assertEquals(commandParameters.code, code);
        Assert.assertEquals(commandParameters.continuationToken, continuationToken);
        Assert.assertEquals(commandParameters.scopes, scopes);
        Assert.assertEquals(commandParameters.challengeType, challengeTypes);
        Assert.assertEquals(commandParameters.getCorrelationId(), correlationId);
        Assert.assertFalse(commandParameters.isMFAGrantType);
    }

    @Test
    public void createSignInSubmitPasswordCommandParameters_CommandParamsContainsExpectedParams() throws ClientException {
        List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        char[] pwd = "example".toCharArray();
        String continuationToken = "continuationToken";
        String correlationId = UUID.randomUUID().toString();
        List<String> scopes = new ArrayList<>(Collections.singletonList("User.Read"));
        String claimsRequestJson = "{\"access_token\":{\"acrs\":{\"essential\":true,\"value\":\"c4\"}}}";
        NativeAuthPublicClientApplicationConfiguration configuration = new NativeAuthPublicClientApplicationConfiguration();
        configuration.setChallengeTypes(challengeTypes);
        configuration.setClientId("clientId");
        configuration.setAppContext(mContext);
        configuration.setPowerOptCheckEnabled(false);

        final SignInSubmitPasswordCommandParameters commandParameters = CommandParametersAdapter.createSignInSubmitPasswordCommandParameters(
                configuration,
                null,
                continuationToken,
                pwd,
                correlationId,
                scopes,
                claimsRequestJson
        );
        Assert.assertEquals(commandParameters.claimsRequestJson, claimsRequestJson);
        Assert.assertEquals(commandParameters.password, pwd);
        Assert.assertEquals(commandParameters.continuationToken, continuationToken);
        Assert.assertEquals(commandParameters.scopes, scopes);
        Assert.assertEquals(commandParameters.challengeType, challengeTypes);
        Assert.assertEquals(commandParameters.getCorrelationId(), correlationId);
    }

    @Test
    public void createSignInWithContinuationCommandParameters_CommandParamsContainsExpectedParams() throws ClientException {
        List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        String continuationToken = "continuationToken";
        String username = "username";
        String correlationId = UUID.randomUUID().toString();
        List<String> scopes = new ArrayList<>(Collections.singletonList("User.Read"));
        ClaimsRequest claimsRequest = ClaimsRequest.getClaimsRequestFromJsonString("{\"access_token\":{\"acrs\":{\"essential\":true,\"value\":\"c4\"}}}");
        NativeAuthPublicClientApplicationConfiguration configuration = new NativeAuthPublicClientApplicationConfiguration();
        configuration.setChallengeTypes(challengeTypes);
        configuration.setClientId("clientId");
        configuration.setAppContext(mContext);
        configuration.setPowerOptCheckEnabled(false);

        final SignInWithContinuationTokenCommandParameters commandParameters = CommandParametersAdapter.createSignInWithContinuationTokenCommandParameters(
                configuration,
                null,
                continuationToken,
                username,
                correlationId,
                scopes,
                claimsRequest
        );
        Assert.assertEquals(commandParameters.claimsRequestJson, ClaimsRequest.getJsonStringFromClaimsRequest(claimsRequest));
        Assert.assertEquals(commandParameters.continuationToken, continuationToken);
        Assert.assertEquals(commandParameters.scopes, scopes);
        Assert.assertEquals(commandParameters.challengeType, challengeTypes);
        Assert.assertEquals(commandParameters.getCorrelationId(), correlationId);
        Assert.assertEquals(commandParameters.username, username);

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

    private AcquireTokenParameters getAcquireTokenParametersPreferredAuthMethod(final @Nullable PreferredAuthMethod preferredAuthMethod) {
        final AcquireTokenParameters.Builder parametersBuilder = new AcquireTokenParameters.Builder()
                .withClaims(getAccessTokenClaimsRequest("device_id", ""))
                .withScopes(new ArrayList<String>(Arrays.asList("User.Read")))
                .startAuthorizationFromActivity(mActivity);
        if (preferredAuthMethod != null) {
            parametersBuilder.withPreferredAuthMethod(preferredAuthMethod);
        }
        return parametersBuilder.build();
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

    private DeviceCodeFlowParameters getDeviceCodeFlowParametersWithClaimsWithCorrelationId(final UUID correlationId) {
        DeviceCodeFlowParameters parameters = new DeviceCodeFlowParameters.Builder()
                .withClaims(getDeviceCodeFlowClaimsRequest())
                .withScopes(new ArrayList<String>(Arrays.asList("User.Read")))
                .withCorrelationId(correlationId)
                .build();

        return parameters;
    }

    private DeviceCodeFlowParameters getDeviceCodeFlowParametersWithClaimsWithoutCorrelationId() {
        DeviceCodeFlowParameters parameters = new DeviceCodeFlowParameters.Builder()
                .withClaims(getDeviceCodeFlowClaimsRequest())
                .withScopes(new ArrayList<String>(Arrays.asList("User.Read")))
                .build();

        return parameters;
    }

    private DeviceCodeFlowParameters getDeviceCodeFlowParametersWithoutClaims() {
        DeviceCodeFlowParameters parameters = new DeviceCodeFlowParameters.Builder()
                .withScopes(new ArrayList<String>(Arrays.asList("User.Read")))
                .build();

        return parameters;
    }

    private ClaimsRequest getDeviceCodeFlowClaimsRequest() {
        RequestedClaimAdditionalInformation information = new RequestedClaimAdditionalInformation();
        information.setEssential(true);
        ClaimsRequest claimsRequest = new ClaimsRequest();
        claimsRequest.requestClaimInAccessToken("deviceid", information);
        return claimsRequest;
    }

    private void validateDeviceCodeFlowClaimsInCommandParameter(DeviceCodeFlowCommandParameters deviceCodeFlowCommandParameters) {
        Assert.assertNotNull(deviceCodeFlowCommandParameters.getClaimsRequestJson());
        ClaimsRequest claimsRequest = ClaimsRequest.getClaimsRequestFromJsonString(deviceCodeFlowCommandParameters.getClaimsRequestJson());
        Assert.assertNotNull(claimsRequest);
        Assert.assertNotNull(claimsRequest.getAccessTokenClaimsRequested());
        RequestedClaim requestedClaim = claimsRequest.getAccessTokenClaimsRequested().get(0);
        Assert.assertNotNull(requestedClaim);

        Assert.assertEquals("deviceid", requestedClaim.getName());
        Assert.assertTrue(requestedClaim.getAdditionalInformation().getEssential());
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
        @SuppressWarnings("unchecked")
        public TestOAuth2TokenCache(Context context, IAccountCredentialCache accountCredentialCache, IAccountCredentialAdapter accountCredentialAdapter) {
            super(AndroidPlatformComponentsFactory.createFromContext(context), accountCredentialCache, accountCredentialAdapter);
        }
    }

}
