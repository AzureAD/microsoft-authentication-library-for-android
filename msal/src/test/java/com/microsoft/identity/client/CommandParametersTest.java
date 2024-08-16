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
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.ui.PreferredAuthMethod;

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
