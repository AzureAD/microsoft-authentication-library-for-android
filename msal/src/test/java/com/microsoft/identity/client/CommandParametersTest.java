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
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.logging.RequestContext;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2ResendCodeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SignInAfterResetPasswordCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitCodeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitNewPasswordCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordV2StartCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.JITChallengeAuthMethodCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.JITContinueCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.MFAChallengeAuthMethodCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.MFASubmitChallengeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordResendCodeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordStartCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitCodeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitNewPasswordCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInResendCodeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitCodeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitPasswordCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInWithContinuationTokenCommandParameters;
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpResendCodeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpStartCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitCodeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitPasswordCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitUserAttributesCommandParameters;
import com.microsoft.identity.nativeauth.AuthMethod;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
public class CommandParametersTest {

    private static final String AAD_CP1_CONFIG_FILE = "src/test/res/raw/aad_capabilities_cp1.json";
    private static final String AAD_NONE_CONFIG_FILE = "src/test/res/raw/aad_capabilities_none.json";
    private static final String WEBAUTHN_CAPABLE_CONFIG_FILE = "src/test/res/raw/webauthn_capable.json";
    private static final String NATIVE_AUTH_CONFIG_FILE = "src/test/res/raw/native_auth_native_only_test_config.json";

    private Context mContext;
    private Activity mActivity;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mActivity = Mockito.mock(Activity.class);
        Mockito.when(mActivity.getApplicationContext()).thenReturn(mContext);
    }

    @Test
    public void testAcquireTokenSilentOperationWithClaimsWithCapabilities() throws ClientException {
        SilentTokenCommandParameters commandParameters = CommandParametersAdapter.createSilentTokenCommandParameters(getConfiguration(AAD_CP1_CONFIG_FILE), getCache(), getAcquireTokenSilentParametersWithClaims());
        Assert.assertEquals(true, commandParameters.isForceRefresh());
    }

    @Test
    public void testAcquireTokenSilentOperationWithClaimsWithoutCapabilities() throws ClientException {
        SilentTokenCommandParameters commandParameters = CommandParametersAdapter.createSilentTokenCommandParameters(getConfiguration(AAD_NONE_CONFIG_FILE), getCache(), getAcquireTokenSilentParametersWithClaims());
        Assert.assertEquals(true, commandParameters.isForceRefresh());
    }

    @Test
    public void testAcquireTokenSilentOperationWithoutClaimsWithCapabilities() throws ClientException {
        SilentTokenCommandParameters commandParameters = CommandParametersAdapter.createSilentTokenCommandParameters(getConfiguration(AAD_CP1_CONFIG_FILE), getCache(), getAcquireTokenSilentParametersWithoutClaims());
        Assert.assertEquals(false, commandParameters.isForceRefresh());
    }

    @Test
    public void testAcquireTokenSilentOperationWithoutClaimsWithoutCapabilities() throws ClientException {
        SilentTokenCommandParameters commandParameters = CommandParametersAdapter.createSilentTokenCommandParameters(getConfiguration(AAD_NONE_CONFIG_FILE), getCache(), getAcquireTokenSilentParametersWithoutClaims());

        Assert.assertEquals(false, commandParameters.isForceRefresh());
    }

    @Test
    public void testAcquireTokenOperationWithClaimsWithCapabilities() throws ClientException {
        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter.createInteractiveTokenCommandParameters(getConfiguration(AAD_CP1_CONFIG_FILE), getCache(), getAcquireTokenParametersWithClaims());
        Assert.assertEquals(true, commandParameters.isForceRefresh());
    }

    @Test
    public void testAcquireTokenOperationWithClaimsWithoutCapabilities() throws ClientException {
        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter.createInteractiveTokenCommandParameters(getConfiguration(AAD_NONE_CONFIG_FILE), getCache(), getAcquireTokenParametersWithClaims());
        Assert.assertEquals(true, commandParameters.isForceRefresh());
    }

    @Test
    public void testAcquireTokenOperationWithoutClaimsWithCapabilities() throws ClientException {
        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter.createInteractiveTokenCommandParameters(getConfiguration(AAD_CP1_CONFIG_FILE), getCache(), getAcquireTokenParametersWithoutClaims());
        Assert.assertEquals(false, commandParameters.isForceRefresh());
    }

    @Test
    public void testAcquireTokenOperationWithoutClaimsWithoutCapabilities() throws ClientException {
        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter.createInteractiveTokenCommandParameters(getConfiguration(AAD_NONE_CONFIG_FILE), getCache(), getAcquireTokenParametersWithoutClaims());
        Assert.assertEquals(false, commandParameters.isForceRefresh());
    }

    @Test
    public void testAcquireTokenOperationWithoutCorrelationId() throws ClientException {
        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter.createInteractiveTokenCommandParameters(getConfiguration(AAD_CP1_CONFIG_FILE), getCache(), getAcquireTokenParametersWithoutCorrelationId());
        Assert.assertNull(commandParameters.getCorrelationId());
    }

    @Test
    public void testAcquireTokenOperationWithCorrelationId() throws ClientException {
        final UUID correlationId = UUID.randomUUID();
        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter.createInteractiveTokenCommandParameters(getConfiguration(AAD_NONE_CONFIG_FILE), getCache(), getAcquireTokenParametersWithCorrelationId(correlationId));
        Assert.assertNotNull(commandParameters.getCorrelationId());
        Assert.assertEquals(correlationId.toString(), commandParameters.getCorrelationId());
    }

    @Test
    public void testAcquireTokenOperationWithPreferredAuthMethod() throws ClientException {

        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter.createInteractiveTokenCommandParameters(
                getConfiguration(AAD_NONE_CONFIG_FILE),
                getCache(),
                getAcquireTokenParametersPreferredAuthMethod(PreferredAuthMethod.QR)
        );
        Assert.assertEquals(PreferredAuthMethod.QR, commandParameters.getPreferredAuthMethod());
    }

    @Test
    public void testAcquireTokenOperationWithNoPreferredAuthMethod() throws ClientException {

        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter.createInteractiveTokenCommandParameters(
                getConfiguration(AAD_NONE_CONFIG_FILE),
                getCache(),
                getAcquireTokenParametersPreferredAuthMethod(null)
        );
        Assert.assertNull(commandParameters.getPreferredAuthMethod());
    }

    @Test
    public void testAcquireTokenSilentOperationWithoutCorrelationId() throws ClientException {
        SilentTokenCommandParameters commandParameters = CommandParametersAdapter.createSilentTokenCommandParameters(getConfiguration(AAD_CP1_CONFIG_FILE), getCache(), getAcquireTokenSilentParametersWithoutCorrelationId());
        Assert.assertNull(commandParameters.getCorrelationId());
    }

    @Test
    public void testAcquireTokenSilentOperationWithCorrelationId() throws ClientException {
        final UUID correlationId = UUID.randomUUID();
        SilentTokenCommandParameters commandParameters = CommandParametersAdapter.createSilentTokenCommandParameters(getConfiguration(AAD_NONE_CONFIG_FILE), getCache(), getAcquireTokenSilentParametersWithCorrelationId(correlationId));
        Assert.assertNotNull(commandParameters.getCorrelationId());
        Assert.assertEquals(correlationId.toString(), commandParameters.getCorrelationId());
    }

    @Test
    public void testDeviceCodeFlowOperationWithClaimsWithCorrelationId() throws ClientException {
        final UUID correlationId = UUID.randomUUID();
        DeviceCodeFlowCommandParameters commandParameters = CommandParametersAdapter.createDeviceCodeFlowWithClaimsCommandParameters(getConfiguration(AAD_NONE_CONFIG_FILE), getCache(), getDeviceCodeFlowParametersWithClaimsWithCorrelationId(correlationId));
        Assert.assertNotNull(commandParameters.getCorrelationId());
        Assert.assertEquals(correlationId.toString(), commandParameters.getCorrelationId());
        validateDeviceCodeFlowClaimsInCommandParameter(commandParameters);
    }

    @Test
    public void testDeviceCodeFlowOperationWithClaimsWithoutCorrelationId() throws ClientException {
        DeviceCodeFlowCommandParameters commandParameters = CommandParametersAdapter.createDeviceCodeFlowWithClaimsCommandParameters(getConfiguration(AAD_NONE_CONFIG_FILE), getCache(), getDeviceCodeFlowParametersWithClaimsWithoutCorrelationId());
        Assert.assertNull(commandParameters.getCorrelationId());
        validateDeviceCodeFlowClaimsInCommandParameter(commandParameters);
    }

    @Test
    public void testDeviceCodeFlowOperationWithoutClaims() throws ClientException {
        DeviceCodeFlowCommandParameters commandParameters = CommandParametersAdapter.createDeviceCodeFlowWithClaimsCommandParameters(getConfiguration(AAD_NONE_CONFIG_FILE), getCache(), getDeviceCodeFlowParametersWithoutClaims());
        Assert.assertNull(commandParameters.getCorrelationId());
        Assert.assertNull(commandParameters.getClaimsRequestJson());
    }

    @Test
    @Config(sdk=28)
    public void testAppendToExtraQueryParametersIfWebAuthnCapable_UnsetPropertyAndNullInput() {
        final List<Map.Entry<String, String>> combinedQueryParameters = CommandParametersAdapter.appendToExtraQueryParametersIfWebAuthnCapable(
                null,
                getConfiguration(AAD_NONE_CONFIG_FILE)
        );
        Assert.assertNull(combinedQueryParameters);
    }

    @Test
    @Config(sdk=28)
    public void testAppendToExtraQueryParametersIfWebAuthnCapable_UnsetPropertyAndNonNullInput() {
        final List<Map.Entry<String, String>> queryParameters = new ArrayList<>();
        queryParameters.add(new AbstractMap.SimpleEntry<>("field1", "property1"));
        final List<Map.Entry<String, String>> combinedQueryParameters = CommandParametersAdapter.appendToExtraQueryParametersIfWebAuthnCapable(
                queryParameters,
                getConfiguration(AAD_NONE_CONFIG_FILE)
        );
        Assert.assertNotNull(combinedQueryParameters);
        Assert.assertEquals(combinedQueryParameters.size(), 1);
    }

    @Test
    @Config(sdk=28)
    public void testAppendToExtraQueryParametersIfWebAuthnCapable_setPropertyAndNullInput() {
        final List<Map.Entry<String, String>> combinedQueryParameters = CommandParametersAdapter.appendToExtraQueryParametersIfWebAuthnCapable(
                null,
                getConfiguration(WEBAUTHN_CAPABLE_CONFIG_FILE)
        );
        Assert.assertNotNull(combinedQueryParameters);
        Assert.assertEquals(combinedQueryParameters.size(), 1);
    }

    @Test
    @Config(sdk=28)
    public void testAppendToExtraQueryParametersIfWebAuthnCapable_setPropertyAndNonNullInput() {
        final List<Map.Entry<String, String>> queryParameters = new ArrayList<>();
        queryParameters.add(new AbstractMap.SimpleEntry<>("field1", "property1"));
        final List<Map.Entry<String, String>> combinedQueryParameters = CommandParametersAdapter.appendToExtraQueryParametersIfWebAuthnCapable(
                queryParameters,
                getConfiguration(WEBAUTHN_CAPABLE_CONFIG_FILE)
        );
        Assert.assertNotNull(combinedQueryParameters);
        Assert.assertEquals(combinedQueryParameters.size(), 2);
    }

    @Test
    @Config(sdk=28)
    public void testAppendToExtraQueryParametersIfWebAuthnCapable_setPropertyAndParameterAlreadyPresent() {
        final List<Map.Entry<String, String>> queryParameters = new ArrayList<>();
        queryParameters.add(new AbstractMap.SimpleEntry<>(FidoConstants.WEBAUTHN_QUERY_PARAMETER_FIELD, FidoConstants.WEBAUTHN_QUERY_PARAMETER_VALUE));
        final List<Map.Entry<String, String>> combinedQueryParameters = CommandParametersAdapter.appendToExtraQueryParametersIfWebAuthnCapable(
                queryParameters,
                getConfiguration(WEBAUTHN_CAPABLE_CONFIG_FILE)
        );
        Assert.assertNotNull(combinedQueryParameters);
        Assert.assertEquals(combinedQueryParameters.size(), 1);
    }

    @Test
    @Config(sdk=28)
    public void testAppendToExtraQueryParametersIfWebAuthnCapable_setPropertyAndSingletonListInput() {
        final List<Map.Entry<String, String>> queryParameters = Collections.singletonList(new AbstractMap.SimpleEntry<>("field1", "property1"));
        final List<Map.Entry<String, String>> combinedQueryParameters = CommandParametersAdapter.appendToExtraQueryParametersIfWebAuthnCapable(
                queryParameters,
                getConfiguration(WEBAUTHN_CAPABLE_CONFIG_FILE)
        );
        Assert.assertNotNull(combinedQueryParameters);
        Assert.assertEquals(combinedQueryParameters.size(), 2);
    }

    @Test
    @Config(sdk=28)
    public void testAppendToExtraQueryParametersIfWebAuthnCapable_setPropertyAndArraysAsListInput() {
        final List<Map.Entry<String, String>> queryParameters = Arrays.asList(
                new AbstractMap.SimpleEntry<>("field1", "property1"),
                new AbstractMap.SimpleEntry<>("field2", "property2"));
        final List<Map.Entry<String, String>> combinedQueryParameters = CommandParametersAdapter.appendToExtraQueryParametersIfWebAuthnCapable(
                queryParameters,
                getConfiguration(WEBAUTHN_CAPABLE_CONFIG_FILE)
        );
        Assert.assertNotNull(combinedQueryParameters);
        Assert.assertEquals(combinedQueryParameters.size(), 3);
    }

    @Test
    @Config(sdk=28)
    public void testAppendToExtraQueryParametersIfWebAuthnCapable_setPropertyAndParameterAlreadyPresentInImmutableList() {
        final List<Map.Entry<String, String>> queryParameters = Collections.singletonList(new AbstractMap.SimpleEntry<>(
                FidoConstants.WEBAUTHN_QUERY_PARAMETER_FIELD,
                FidoConstants.WEBAUTHN_QUERY_PARAMETER_VALUE));
        final List<Map.Entry<String, String>> combinedQueryParameters = CommandParametersAdapter.appendToExtraQueryParametersIfWebAuthnCapable(
                queryParameters,
                getConfiguration(WEBAUTHN_CAPABLE_CONFIG_FILE)
        );
        Assert.assertNotNull(combinedQueryParameters);
        Assert.assertEquals(combinedQueryParameters.size(), 1);
    }

    @Test
    @Config(sdk=26)
    public void testAppendToExtraQueryParametersIfWebAuthnCapable_setPropertyAndNullInputWithOlderOs() {
        final List<Map.Entry<String, String>> combinedQueryParameters = CommandParametersAdapter.appendToExtraQueryParametersIfWebAuthnCapable(
                null,
                getConfiguration(WEBAUTHN_CAPABLE_CONFIG_FILE)
        );
        if (combinedQueryParameters != null) {
            Assert.assertTrue(combinedQueryParameters.isEmpty());
        } else {
            Assert.assertNull(combinedQueryParameters);
        }
    }

    @Test
    @Config(sdk=26)
    public void testAppendToExtraQueryParametersIfWebAuthnCapable_setPropertyAndParameterAlreadyPresentWithOlderOs() {
        final List<Map.Entry<String, String>> queryParameters = Collections.singletonList(new AbstractMap.SimpleEntry<>(
                FidoConstants.WEBAUTHN_QUERY_PARAMETER_FIELD,
                FidoConstants.WEBAUTHN_QUERY_PARAMETER_VALUE));
        final List<Map.Entry<String, String>> combinedQueryParameters = CommandParametersAdapter.appendToExtraQueryParametersIfWebAuthnCapable(
                queryParameters,
                getConfiguration(WEBAUTHN_CAPABLE_CONFIG_FILE)
        );
        Assert.assertNotNull(combinedQueryParameters);
        Assert.assertEquals(combinedQueryParameters.size(), 0);
    }

    @Test
    @Config(sdk=28)
    public void testAppendToExtraQueryParametersIfWebAuthnCapable_WebAuthnCapableFalse() {
        final List<Map.Entry<String, String>> queryParameters = Arrays.asList(
                new AbstractMap.SimpleEntry<>("field1", "property1"),
                new AbstractMap.SimpleEntry<>("field2", "property2"));
        final List<Map.Entry<String, String>> combinedQueryParameters = CommandParametersAdapter.appendToExtraQueryParametersIfWebAuthnCapable(
                queryParameters,
                getConfiguration(AAD_NONE_CONFIG_FILE)
        );
        Assert.assertNotNull(combinedQueryParameters);
        Assert.assertEquals(combinedQueryParameters.size(), 2);
    }

    @Test
    @Config(sdk=28)
    public void testAppendToExtraQueryParametersIfWebAuthnCapable_WebAuthnCapableFalseKeepPresentParam() {
        final List<Map.Entry<String, String>> queryParameters = Collections.singletonList(new AbstractMap.SimpleEntry<>(
                FidoConstants.WEBAUTHN_QUERY_PARAMETER_FIELD,
                FidoConstants.WEBAUTHN_QUERY_PARAMETER_VALUE));
        final List<Map.Entry<String, String>> combinedQueryParameters = CommandParametersAdapter.appendToExtraQueryParametersIfWebAuthnCapable(
                queryParameters,
                getConfiguration(AAD_NONE_CONFIG_FILE)
        );
        Assert.assertNotNull(combinedQueryParameters);
        Assert.assertEquals(combinedQueryParameters.size(), 1);
    }


    @Test
    @Config(sdk=28)
    public void testPasskeyHeader_AddedWhenWebAuthnConfigurationEnabled() throws ClientException {
        InteractiveTokenCommandParameters commandParameters = CommandParametersAdapter
                .createInteractiveTokenCommandParameters(
                        getConfiguration(WEBAUTHN_CAPABLE_CONFIG_FILE),
                        getCache(),
                        getAcquireTokenParametersWithClaims()
                );
        Assert.assertTrue(commandParameters
                .getRequestHeaders()
                .containsKey(FidoConstants.PASSKEY_PROTOCOL_HEADER_NAME)
        );
        Assert.assertEquals(
                FidoConstants.PASSKEY_PROTOCOL_HEADER_AUTH_AND_REG,
                commandParameters.getRequestHeaders().get(FidoConstants.PASSKEY_PROTOCOL_HEADER_NAME)
        );
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

    @Test
    public void testCreateResetPasswordV2StartCommandParameters_CommandParamsContainsExpectedParams() throws ClientException {
        final String correlationId = "00000000-0000-0000-0000-000000000001";
        final RequestContext requestContext = new RequestContext();
        requestContext.put(DiagnosticContext.CORRELATION_ID, correlationId);
        DiagnosticContext.INSTANCE.setRequestContext(requestContext);

        try {
            final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(NATIVE_AUTH_CONFIG_FILE);
            final OAuth2TokenCache tokenCache = getCache();
            final String username = "username@example.com";
            final List<String> scopes = Arrays.asList("openid", "profile", "User.Read");

            final ResetPasswordV2StartCommandParameters commandParameters =
                    CommandParametersAdapter.createResetPasswordV2StartCommandParameters(
                            configuration,
                            tokenCache,
                            username,
                            scopes
                    );

            Assert.assertEquals(username, commandParameters.username);
            Assert.assertEquals(scopes, commandParameters.scopes);
            Assert.assertEquals(configuration.getChallengeTypes(), commandParameters.challengeType);
            Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
            Assert.assertSame(tokenCache, commandParameters.getOAuth2TokenCache());
        } finally {
            DiagnosticContext.INSTANCE.clear();
        }
    }

    @Test
    public void testCreateNativeAuthV2SubmitCodeCommandParameters_CommandParamsContainsExpectedParams() throws ClientException {
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(NATIVE_AUTH_CONFIG_FILE);
        final OAuth2TokenCache tokenCache = getCache();
        final String code = "123456";
        final List<String> scopes = Arrays.asList("api://scope/read", "offline_access");
        final String correlationId = "00000000-0000-0000-0000-000000000002";
        final NativeAuthV2ContinuationState continuationState =
                getNativeAuthV2ContinuationState(correlationId, scopes, null);

        final NativeAuthV2SubmitCodeCommandParameters commandParameters =
                CommandParametersAdapter.createNativeAuthV2SubmitCodeCommandParameters(
                        configuration,
                        tokenCache,
                        code,
                        continuationState
                );

        Assert.assertEquals(code, commandParameters.code);
        Assert.assertSame(continuationState, commandParameters.continuationState);
        Assert.assertEquals(scopes, commandParameters.scopes);
        Assert.assertEquals(configuration.getChallengeTypes(), commandParameters.challengeType);
        Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
        Assert.assertSame(tokenCache, commandParameters.getOAuth2TokenCache());

    }

    @Test
    public void testCreateNativeAuthV2ResendCodeCommandParameters_CommandParamsContainsExpectedParams() throws ClientException {
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(NATIVE_AUTH_CONFIG_FILE);
        final OAuth2TokenCache tokenCache = getCache();
        final List<String> scopes = Arrays.asList("api://scope/read", "offline_access");
        final String correlationId = "00000000-0000-0000-0000-000000000003";
        final NativeAuthV2ContinuationState continuationState =
                getNativeAuthV2ContinuationState(correlationId, scopes, null);

        final NativeAuthV2ResendCodeCommandParameters commandParameters =
                CommandParametersAdapter.createNativeAuthV2ResendCodeCommandParameters(
                        configuration,
                        tokenCache,
                        continuationState
                );

        Assert.assertSame(continuationState, commandParameters.continuationState);
        Assert.assertEquals(scopes, commandParameters.scopes);
        Assert.assertEquals(configuration.getChallengeTypes(), commandParameters.challengeType);
        Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
        Assert.assertSame(tokenCache, commandParameters.getOAuth2TokenCache());
    }

    @Test
    public void testCreateNativeAuthV2SubmitNewPasswordCommandParameters_CommandParamsContainsExpectedParams() throws ClientException {
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(NATIVE_AUTH_CONFIG_FILE);
        final OAuth2TokenCache tokenCache = getCache();
        final char[] newPassword = "newPassword!".toCharArray();
        final List<String> scopes = Arrays.asList("api://scope/read", "offline_access");
        final String correlationId = "00000000-0000-0000-0000-000000000004";
        final NativeAuthV2ContinuationState continuationState =
                getNativeAuthV2ContinuationState(correlationId, scopes, null);

        final NativeAuthV2SubmitNewPasswordCommandParameters commandParameters =
                CommandParametersAdapter.createNativeAuthV2SubmitNewPasswordCommandParameters(
                        configuration,
                        tokenCache,
                        newPassword,
                        continuationState
                );

        Assert.assertArrayEquals(newPassword, commandParameters.newPassword);
        Assert.assertSame(continuationState, commandParameters.continuationState);
        Assert.assertEquals(scopes, commandParameters.scopes);
        Assert.assertEquals(configuration.getChallengeTypes(), commandParameters.challengeType);
        Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
        Assert.assertSame(tokenCache, commandParameters.getOAuth2TokenCache());
    }

    @Test
    public void testCreateNativeAuthV2SignInAfterResetPasswordCommandParameters_CommandParamsContainsExpectedParams() throws ClientException {
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(NATIVE_AUTH_CONFIG_FILE);
        final OAuth2TokenCache tokenCache = getCache();
        final String correlationId = "00000000-0000-0000-0000-000000000005";
        final List<String> continuationScopes = Arrays.asList("continuation.scope");
        final String continuationClaimsRequestJson = "{\"access_token\":{\"xms_cc\":{\"values\":[\"cp1\"]}}}";
        final NativeAuthV2ContinuationState continuationState =
                getNativeAuthV2ContinuationState(correlationId, continuationScopes, continuationClaimsRequestJson);
        final NativeAuthV2SignInAfterResetPasswordCommandParameters commandParameters =
                CommandParametersAdapter.createNativeAuthV2SignInAfterResetPasswordCommandParameters(
                        configuration,
                        tokenCache,
                        continuationState
                );

        Assert.assertSame(continuationState, commandParameters.continuationState);
        Assert.assertEquals(continuationScopes, commandParameters.scopes);
        Assert.assertEquals(continuationClaimsRequestJson, commandParameters.claimsRequestJson);
        Assert.assertEquals(configuration.getChallengeTypes(), commandParameters.challengeType);
        Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
        Assert.assertSame(tokenCache, commandParameters.getOAuth2TokenCache());
    }

    private NativeAuthPublicClientApplicationConfiguration getNativeAuthConfiguration(final List<String> challengeTypes) {
        final NativeAuthPublicClientApplicationConfiguration configuration = new NativeAuthPublicClientApplicationConfiguration();
        configuration.setChallengeTypes(challengeTypes);
        configuration.setClientId("clientId");
        configuration.setAppContext(mContext);
        configuration.setPowerOptCheckEnabled(false);
        return configuration;
    }

    @Test
    public void createSignUpStartCommandParameters_CommandParamsContainsExpectedParams() {
        final List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        final String username = "user@contoso.com";
        final char[] password = "example".toCharArray();
        final Map<String, String> userAttributes = new HashMap<>();
        userAttributes.put("city", "Redmond");
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(challengeTypes);

        final SignUpStartCommandParameters commandParameters = CommandParametersAdapter.createSignUpStartCommandParameters(
                configuration,
                null,
                username,
                password,
                userAttributes
        );
        Assert.assertNotNull(commandParameters);
        Assert.assertEquals(username, commandParameters.username);
        Assert.assertEquals(password, commandParameters.password);
        Assert.assertEquals(userAttributes, commandParameters.userAttributes);
        Assert.assertEquals(challengeTypes, commandParameters.challengeType);
    }

    @Test
    public void createSignUpSubmitCodeCommandParameters_CommandParamsContainsExpectedParams() {
        final List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        final String code = "123456";
        final String continuationToken = "continuationToken";
        final String correlationId = UUID.randomUUID().toString();
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(challengeTypes);

        final SignUpSubmitCodeCommandParameters commandParameters = CommandParametersAdapter.createSignUpSubmitCodeCommandParameters(
                configuration,
                null,
                code,
                continuationToken,
                correlationId
        );
        Assert.assertNotNull(commandParameters);
        Assert.assertEquals(code, commandParameters.code);
        Assert.assertEquals(continuationToken, commandParameters.continuationToken);
        Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
        Assert.assertEquals(challengeTypes, commandParameters.challengeType);
    }

    @Test
    public void createSignUpResendCodeCommandParameters_CommandParamsContainsExpectedParams() {
        final List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        final String continuationToken = "continuationToken";
        final String correlationId = UUID.randomUUID().toString();
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(challengeTypes);

        final SignUpResendCodeCommandParameters commandParameters = CommandParametersAdapter.createSignUpResendCodeCommandParameters(
                configuration,
                null,
                continuationToken,
                correlationId
        );
        Assert.assertNotNull(commandParameters);
        Assert.assertEquals(continuationToken, commandParameters.continuationToken);
        Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
        Assert.assertEquals(challengeTypes, commandParameters.challengeType);
    }

    @Test
    public void createSignUpSubmitUserAttributesCommandParameters_CommandParamsContainsExpectedParams() {
        final List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        final String continuationToken = "continuationToken";
        final String correlationId = UUID.randomUUID().toString();
        final Map<String, String> userAttributes = new HashMap<>();
        userAttributes.put("city", "Redmond");
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(challengeTypes);

        final SignUpSubmitUserAttributesCommandParameters commandParameters = CommandParametersAdapter.createSignUpStarSubmitUserAttributesCommandParameters(
                configuration,
                null,
                continuationToken,
                correlationId,
                userAttributes
        );
        Assert.assertNotNull(commandParameters);
        Assert.assertEquals(userAttributes, commandParameters.userAttributes);
        Assert.assertEquals(continuationToken, commandParameters.continuationToken);
        Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
        Assert.assertEquals(challengeTypes, commandParameters.challengeType);
    }

    @Test
    public void createSignUpSubmitPasswordCommandParameters_CommandParamsContainsExpectedParams() {
        final List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        final String continuationToken = "continuationToken";
        final String correlationId = UUID.randomUUID().toString();
        final char[] password = "example".toCharArray();
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(challengeTypes);

        final SignUpSubmitPasswordCommandParameters commandParameters = CommandParametersAdapter.createSignUpSubmitPasswordCommandParameters(
                configuration,
                null,
                continuationToken,
                correlationId,
                password
        );
        Assert.assertNotNull(commandParameters);
        Assert.assertEquals(password, commandParameters.password);
        Assert.assertEquals(continuationToken, commandParameters.continuationToken);
        Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
        Assert.assertEquals(challengeTypes, commandParameters.challengeType);
    }

    @Test
    public void createSignInResendCodeCommandParameters_CommandParamsContainsExpectedParams() {
        final List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        final String continuationToken = "continuationToken";
        final String correlationId = UUID.randomUUID().toString();
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(challengeTypes);

        final SignInResendCodeCommandParameters commandParameters = CommandParametersAdapter.createSignInResendCodeCommandParameters(
                configuration,
                null,
                correlationId,
                continuationToken
        );
        Assert.assertNotNull(commandParameters);
        Assert.assertEquals(continuationToken, commandParameters.continuationToken);
        Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
        Assert.assertEquals(challengeTypes, commandParameters.challengeType);
    }

    @Test
    public void createMFAChallengeAuthMethodCommandParameters_CommandParamsContainsExpectedParams() throws ClientException {
        final List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        final String continuationToken = "continuationToken";
        final String correlationId = UUID.randomUUID().toString();
        final AuthMethod authMethod = new AuthMethod("authMethodId", "oob", "user@contoso.com", "email");
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(challengeTypes);

        final MFAChallengeAuthMethodCommandParameters commandParameters = CommandParametersAdapter.createMFAChallengeAuthMethodCommandParameters(
                configuration,
                null,
                continuationToken,
                correlationId,
                authMethod
        );
        Assert.assertNotNull(commandParameters);
        Assert.assertEquals(continuationToken, commandParameters.continuationToken);
        Assert.assertEquals(authMethod.getId(), commandParameters.authMethodId);
        Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
        Assert.assertEquals(challengeTypes, commandParameters.challengeType);
    }

    @Test
    public void createMFASubmitChallengeCommandParameters_CommandParamsContainsExpectedParams() throws ClientException {
        final List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        final String challenge = "123456";
        final String continuationToken = "continuationToken";
        final String correlationId = UUID.randomUUID().toString();
        final List<String> scopes = new ArrayList<>(Collections.singletonList("User.Read"));
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(challengeTypes);

        final MFASubmitChallengeCommandParameters commandParameters = CommandParametersAdapter.createMFASubmitChallengeCommandParameters(
                configuration,
                null,
                challenge,
                correlationId,
                continuationToken,
                scopes
        );
        Assert.assertNotNull(commandParameters);
        Assert.assertEquals(challenge, commandParameters.challenge);
        Assert.assertEquals(continuationToken, commandParameters.continuationToken);
        Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
        Assert.assertEquals(challengeTypes, commandParameters.challengeType);
    }

    @Test
    public void createResetPasswordStartCommandParameters_CommandParamsContainsExpectedParams() {
        final List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        final String username = "user@contoso.com";
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(challengeTypes);

        final ResetPasswordStartCommandParameters commandParameters = CommandParametersAdapter.createResetPasswordStartCommandParameters(
                configuration,
                null,
                username
        );
        Assert.assertNotNull(commandParameters);
        Assert.assertEquals(username, commandParameters.username);
        Assert.assertEquals(challengeTypes, commandParameters.challengeType);
    }

    @Test
    public void createResetPasswordSubmitCodeCommandParameters_CommandParamsContainsExpectedParams() {
        final List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        final String code = "123456";
        final String continuationToken = "continuationToken";
        final String correlationId = UUID.randomUUID().toString();
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(challengeTypes);

        final ResetPasswordSubmitCodeCommandParameters commandParameters = CommandParametersAdapter.createResetPasswordSubmitCodeCommandParameters(
                configuration,
                null,
                code,
                correlationId,
                continuationToken
        );
        Assert.assertNotNull(commandParameters);
        Assert.assertEquals(code, commandParameters.code);
        Assert.assertEquals(continuationToken, commandParameters.continuationToken);
        Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
        Assert.assertEquals(challengeTypes, commandParameters.challengeType);
    }

    @Test
    public void createResetPasswordResendCodeCommandParameters_CommandParamsContainsExpectedParams() {
        final List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        final String continuationToken = "continuationToken";
        final String correlationId = UUID.randomUUID().toString();
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(challengeTypes);

        final ResetPasswordResendCodeCommandParameters commandParameters = CommandParametersAdapter.createResetPasswordResendCodeCommandParameters(
                configuration,
                null,
                correlationId,
                continuationToken
        );
        Assert.assertNotNull(commandParameters);
        Assert.assertEquals(continuationToken, commandParameters.continuationToken);
        Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
        Assert.assertEquals(challengeTypes, commandParameters.challengeType);
    }

    @Test
    public void createResetPasswordSubmitNewPasswordCommandParameters_CommandParamsContainsExpectedParams() {
        final List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        final String continuationToken = "continuationToken";
        final String correlationId = UUID.randomUUID().toString();
        final char[] password = "example".toCharArray();
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(challengeTypes);

        final ResetPasswordSubmitNewPasswordCommandParameters commandParameters = CommandParametersAdapter.createResetPasswordSubmitNewPasswordCommandParameters(
                configuration,
                null,
                continuationToken,
                correlationId,
                password
        );
        Assert.assertNotNull(commandParameters);
        Assert.assertEquals(password, commandParameters.newPassword);
        Assert.assertEquals(continuationToken, commandParameters.continuationToken);
        Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
        Assert.assertEquals(challengeTypes, commandParameters.challengeType);
    }

    @Test
    public void createJITChallengeAuthMethodCommandParameters_CommandParamsContainsExpectedParams() throws ClientException {
        final List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        final String verificationContact = "user@contoso.com";
        final String challengeChannel = "email";
        final String challengeType = "oob";
        final String continuationToken = "continuationToken";
        final String correlationId = UUID.randomUUID().toString();
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(challengeTypes);

        final JITChallengeAuthMethodCommandParameters commandParameters = CommandParametersAdapter.createJITChallengeAuthMethodCommandParameters(
                configuration,
                null,
                verificationContact,
                challengeChannel,
                challengeType,
                correlationId,
                continuationToken
        );
        Assert.assertNotNull(commandParameters);
        Assert.assertEquals(verificationContact, commandParameters.verificationContact);
        Assert.assertEquals(challengeChannel, commandParameters.challengeChannel);
        Assert.assertEquals(continuationToken, commandParameters.continuationToken);
        Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
    }

    @Test
    public void createJITSubmitChallengeCommandParameters_CommandParamsContainsExpectedParams() throws ClientException {
        final List<String> challengeTypes = new ArrayList<>(Collections.singletonList("OOB"));
        final String grantType = "oob";
        final String code = "123456";
        final String continuationToken = "continuationToken";
        final String correlationId = UUID.randomUUID().toString();
        final NativeAuthPublicClientApplicationConfiguration configuration = getNativeAuthConfiguration(challengeTypes);

        final JITContinueCommandParameters commandParameters = CommandParametersAdapter.createJITSubmitChallengeCommandParameters(
                configuration,
                null,
                grantType,
                code,
                correlationId,
                continuationToken
        );
        Assert.assertNotNull(commandParameters);
        Assert.assertEquals(grantType, commandParameters.grantType);
        Assert.assertEquals(code, commandParameters.code);
        Assert.assertEquals(continuationToken, commandParameters.continuationToken);
        Assert.assertEquals(correlationId, commandParameters.getCorrelationId());
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

    private NativeAuthPublicClientApplicationConfiguration getNativeAuthConfiguration(String path) {
        return NativeAuthPublicClientApplicationConfigurationFactory.Companion.initializeNativeAuthConfiguration(
                mContext,
                getConfigFile(path)
        );
    }

    private NativeAuthV2ContinuationState getNativeAuthV2ContinuationState(
            @NonNull final String correlationId,
            @NonNull final List<String> scopes,
            @Nullable final String claimsRequestJson) {
        final NativeAuthV2ContinuationState continuationState = Mockito.mock(NativeAuthV2ContinuationState.class);
        Mockito.when(continuationState.getCorrelationId()).thenReturn(correlationId);
        Mockito.when(continuationState.scopesForTokenRequest()).thenReturn(scopes);
        Mockito.when(continuationState.claimsRequestJsonForTokenRequest()).thenReturn(claimsRequestJson);
        return continuationState;
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
