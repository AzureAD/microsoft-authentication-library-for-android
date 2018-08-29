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

package com.microsoft.identity.client;

import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.UUID;

/**
 * Unit test for {@link AuthorityMetadata}.
 */
@RunWith(AndroidJUnit4.class)
public final class AuthorityMetadataTest {
    static final String AUTHORIZE_ENDPOINT = "some_authorization_endpoint";
    static final String TOKEN_ENDPOINT = "some_token_endpoint";
    static final String TENANT_DISCOVERY_ENDPOINT = "https://some_tenant_discovery/endpoint";
    static final String TEST_AUTHORITY = "https://some.authority/common";
    static final String TEST_B2C_AUTHORITY = "https://login.microsoftonline.com/tfp/tenant/policy";

    @After
    public void tearDown() {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAuthorityStartWithHttp() {
        AuthorityMetadata.createAuthority("http://test.com", false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAuthorityStartWithNonHttpProtocol() {
        AuthorityMetadata.createAuthority("file://test.com", false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAuthorityNotContainPath() {
        AuthorityMetadata.createAuthority("https://test.com", false);
    }

    @Test
    public void testDeprecateAuthority() {
        final AuthorityMetadata authority = AuthorityMetadata.createAuthority("https://login.windows.net/common", true);
        Assert.assertTrue(authority.getAuthorityHost().equals(AadAuthorityMetadata.AAD_AUTHORITY_HOST));
        Assert.assertTrue(authority.getAuthority().contains(AadAuthorityMetadata.AAD_AUTHORITY_HOST));

        final AuthorityMetadata authorityWithPort = AuthorityMetadata.createAuthority("https://login.windows.net:1010/sometenant", true);
        Assert.assertTrue(authorityWithPort.getAuthority().equals("https://login.microsoftonline.com:1010/sometenant"));
    }

    @Test
    public void testAuthorityContainFragment() {
        final AuthorityMetadata authority = AuthorityMetadata.createAuthority("https://test.com/abc#token=123", false);
        authority.equals("https://test.com/abc");
    }

    @Test
    public void testAuthorityContainQP() {
        final AuthorityMetadata authority = AuthorityMetadata.createAuthority(
                "https://login.microsoftonline.com/common?resource=2343&client_id=234", false);
        authority.equals("https://login.microsoftonline.com/common");
        Assert.assertTrue(authority.getIsTenantless());
    }

    /**
     * We don't support adfs as authority for BUILD.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAdfsAuthorityValidationEnabled() {
        AuthorityMetadata.createAuthority("https://somehost/adfs", true);
    }

    // TODO: when adfs authority validation is enabled back, we should add tests for adfs authority tenant less check.

    /**
     * We don't support adfs as authority for BUILD.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAdfsAuthorityValidationDisabled() {
        AuthorityMetadata.createAuthority("https://somehost/adfs", false);
    }

    @Test
    public void testAuthorityWithPortNumber() {
        final String authorityString = "https://somehost:100/sometenant";
        final AuthorityMetadata authority = AuthorityMetadata.createAuthority(authorityString, true);
        Assert.assertTrue(authority.getAuthority().equals(authorityString));

        final String b2cAuthorityString = "https://somehost:101/tfp/sometenant/signin";
        final AuthorityMetadata b2cAuthority = AuthorityMetadata.createAuthority(b2cAuthorityString, true);
        Assert.assertTrue(b2cAuthority.getAuthority().equals(b2cAuthorityString));

        final String authorityWithIpAddressAsHostString = "https://192.168.0.1:123/sometenant";
        final AuthorityMetadata authorityWithIpAddressAsHost = AuthorityMetadata.createAuthority(authorityWithIpAddressAsHostString, true);
        Assert.assertTrue(authorityWithIpAddressAsHost.getAuthority().equals(authorityWithIpAddressAsHostString));

        final String authorityWithFormedUrlString = "https://login.microsoftonline.com:300/sometenant";
        final AuthorityMetadata authorityWithFormedUrl = AuthorityMetadata.createAuthority(authorityWithFormedUrlString, true);
        Assert.assertTrue(authorityWithFormedUrl.getAuthority().equals(authorityWithFormedUrlString));
    }

    // adfs authoirty is not supported for build, should be enabled back post-build.
    @Ignore
    @Test
    public void testAdfsAuthorityWithPortNumber() {
        final String adfsAuthorityString = "https://adfshost:300/adfs";
        final AuthorityMetadata adfsAuthority = AuthorityMetadata.createAuthority(adfsAuthorityString, false);
        Assert.assertTrue(adfsAuthority.getAuthority().equals(adfsAuthorityString));
    }

    @Test
    public void testAuthorityWithTrailingSlash() throws IOException {
        final String authorityWithTrailingSlash = TEST_AUTHORITY + "/";
        final AuthorityMetadata authority = AuthorityMetadata.createAuthority(authorityWithTrailingSlash, false);

        Assert.assertFalse(authority.getAuthority().toString().endsWith("/"));

        // mock tenant discovery response
        AndroidTestMockUtil.mockSuccessTenantDiscovery(AUTHORIZE_ENDPOINT, TOKEN_ENDPOINT);

        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId()), null);
        } catch (MsalException e) {
            Assert.fail();
        }

        Assert.assertTrue(authority.getAuthorizeEndpoint().equals(AUTHORIZE_ENDPOINT));
        Assert.assertTrue(authority.getTokenEndpoint().equals(TOKEN_ENDPOINT));
    }

    @Test
    public void testAuthorityWithoutTrailingSlash() throws IOException {
        final AuthorityMetadata authority = AuthorityMetadata.createAuthority(TEST_AUTHORITY, false);

        // mock tenant discovery response
        AndroidTestMockUtil.mockSuccessTenantDiscovery(AUTHORIZE_ENDPOINT, TOKEN_ENDPOINT);
        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId()), null);
        } catch (MsalException e) {
            Assert.fail();
        }

        Assert.assertTrue(authority.getAuthorizeEndpoint().equals(AUTHORIZE_ENDPOINT));
        Assert.assertTrue(authority.getTokenEndpoint().equals(TOKEN_ENDPOINT));
    }

    @Test
    public void testAuthorityValidationWithTrustedHost() {
        // make sure no mocked connection in the queue
        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);

        final AuthorityMetadata authorityAzure2 = AuthorityMetadata.createAuthority("https://login.microsoftonline.com/sometenant", true);
        performAuthorityValidationAndVerify(authorityAzure2);

        final AuthorityMetadata authorityChina = AuthorityMetadata.createAuthority("https://login.chinacloudapi.cn/sometenant", true);
        performAuthorityValidationAndVerify(authorityChina);

        final AuthorityMetadata authorityGermany = AuthorityMetadata.createAuthority("https://login.microsoftonline.de/sometenant", true);
        performAuthorityValidationAndVerify(authorityGermany);

        final AuthorityMetadata authorityUSGovernment = AuthorityMetadata.createAuthority("https://login-us.microsoftonline.com/sometenant", true);
        performAuthorityValidationAndVerify(authorityUSGovernment);
    }

    @Test
    public void testB2cAuthorityValidationWithTrustedHost() {
        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);

        final AuthorityMetadata authorityAzure2 = AuthorityMetadata.createAuthority("https://login.microsoftonline.com/tfp/sometenant/policy", true);
        performAuthorityValidationAndVerify(authorityAzure2);

        final AuthorityMetadata authorityChina = AuthorityMetadata.createAuthority("https://login.chinacloudapi.cn/tfp/sometenant/policy", true);
        performAuthorityValidationAndVerify(authorityChina);

        final AuthorityMetadata authorityGermany = AuthorityMetadata.createAuthority("https://login.microsoftonline.de/tfp/sometenant/policy", true);
        performAuthorityValidationAndVerify(authorityGermany);

        final AuthorityMetadata authorityUSGovernment = AuthorityMetadata.createAuthority("https://login-us.microsoftonline.com/tfp/sometenant/policy", true);
        performAuthorityValidationAndVerify(authorityUSGovernment);
    }

    private void performAuthorityValidationAndVerify(final AuthorityMetadata authority) {
        try {
            final String openIdConfigEndpoint = authority.performInstanceDiscovery(new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId()), null);
            Assert.assertTrue(openIdConfigEndpoint.equals(authority.getDefaultOpenIdConfigurationEndpoint()));
        } catch (MsalException e) {
            Assert.fail();
        }
    }

    @Test
    public void testAuthorityValidation() throws IOException {
        final AuthorityMetadata authority = AuthorityMetadata.createAuthority(TEST_AUTHORITY, true);

        // mock success instance discovery
        AndroidTestMockUtil.mockSuccessInstanceDiscovery(TENANT_DISCOVERY_ENDPOINT);
        AndroidTestMockUtil.mockSuccessTenantDiscovery(AUTHORIZE_ENDPOINT, TOKEN_ENDPOINT);

        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId()), null);
        } catch (MsalException e) {
            Assert.fail();
        }

        Assert.assertTrue(authority.getAuthorizeEndpoint().equals(AUTHORIZE_ENDPOINT));
        Assert.assertTrue(authority.getTokenEndpoint().equals(TOKEN_ENDPOINT));

        // make sure no more mocked httpurlconnection is queued
        HttpUrlConnectionFactory.clearMockedConnectionQueue();

        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId()), null);
        } catch (MsalException e) {
            Assert.fail();
        }

        Assert.assertTrue(authority.getAuthorizeEndpoint().equals(AUTHORIZE_ENDPOINT));
        Assert.assertTrue(authority.getTokenEndpoint().equals(TOKEN_ENDPOINT));

        // create new authority with the same authority url again
        final AuthorityMetadata authority2 = AuthorityMetadata.createAuthority(TEST_AUTHORITY, true);
        try {
            authority2.resolveEndpoints(new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId()), null);
        } catch (MsalException e) {
            Assert.fail();
        }

        Assert.assertTrue(authority2.getAuthorizeEndpoint().equals(AUTHORIZE_ENDPOINT));
        Assert.assertTrue(authority2.getTokenEndpoint().equals(TOKEN_ENDPOINT));
    }

    /**
     * Verify that if instance discovery fails(server returns error and error_description), we return the error error back.
     */
    @Test
    public void testInstanceDiscoveryFailed() throws IOException, MsalClientException {
        final String authorityString = "https://some.authority/endpoint";
        final AuthorityMetadata authority = AuthorityMetadata.createAuthority(authorityString, true);

        AndroidTestMockUtil.mockFailedGetRequest(HttpURLConnection.HTTP_BAD_REQUEST,
                AndroidTestUtil.getErrorResponseMessage("invalid_request"));
        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId()), null);
            Assert.fail();
        } catch (final MsalServiceException e) {
            Assert.assertTrue(e.getErrorCode().equals(MsalServiceException.INVALID_REQUEST));
        }
    }

    /**
     * Verify if server returns an invalid JSON response for instance discovery, correct error are returned back.
     */
    @Test
    public void testInstanceDiscoveryNonJsonResponse() throws IOException {
        final String authorityString = "https://some.authority/endpoint";
        final AuthorityMetadata authority = AuthorityMetadata.createAuthority(authorityString, true);

        AndroidTestMockUtil.mockFailedGetRequest(HttpURLConnection.HTTP_BAD_REQUEST, "some_failure_response");
        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId()), null);
            Assert.fail();
        } catch (final MsalException e) {
            Assert.assertTrue(e.getErrorCode().equals(MsalClientException.JSON_PARSE_FAILURE));
        }
    }

    @Test
    public void testInstanceDiscoveryFailedWithTimeout() throws IOException {
        final String authorityString = "https://some.authority/endpoint";
        final AuthorityMetadata authority = AuthorityMetadata.createAuthority(authorityString, true);

        // mock that instance discovery failed with timeout
        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSocketTimeout();
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId()), null);
            Assert.fail();
        } catch (final MsalException e) {
            Assert.assertTrue(e.getErrorCode().equals(MsalServiceException.REQUEST_TIMEOUT));
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof SocketTimeoutException);
        }
    }

    @Test
    public void testTenantDiscoveryFailed() throws IOException, MsalClientException {
        final String authorityString = "https://some.authority/endpoint";
        final AuthorityMetadata authority = AuthorityMetadata.createAuthority(authorityString, true);

        // mock instance discovery succeed
        AndroidTestMockUtil.mockSuccessInstanceDiscovery(TENANT_DISCOVERY_ENDPOINT);

        // mock tenant discovery failed
        AndroidTestMockUtil.mockFailedGetRequest(HttpURLConnection.HTTP_BAD_REQUEST, AndroidTestUtil.getErrorResponseMessage("invalid_instance"));
        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId()), null);
            Assert.fail();
        } catch (final MsalServiceException e) {
            Assert.assertTrue(e.getErrorCode().equals(MsalServiceException.INVALID_INSTANCE));
        }
    }

    @Test
    public void testTenantDiscoveryFailedWithInvalidJsonResponse() throws IOException {
        final String authorityString = "https://some.authority/endpoint";
        final AuthorityMetadata authority = AuthorityMetadata.createAuthority(authorityString, true);

        // mock instance discovery succeed
        AndroidTestMockUtil.mockSuccessInstanceDiscovery(TENANT_DISCOVERY_ENDPOINT);

        // mock tenant discovery failed
        AndroidTestMockUtil.mockFailedGetRequest(HttpURLConnection.HTTP_BAD_REQUEST, "some error");
        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId()), null);
            Assert.fail();
        } catch (final MsalException e) {
            Assert.assertTrue(e.getErrorCode().equals(MsalClientException.JSON_PARSE_FAILURE));
        }
    }

    @Test
    public void testTenantDiscoveryFailedWithTimeout() throws IOException {
        final String authorityString = "https://some.authority/endpoint";
        final AuthorityMetadata authority = AuthorityMetadata.createAuthority(authorityString, true);

        // mock instance discovery succeed
        AndroidTestMockUtil.mockSuccessInstanceDiscovery(TENANT_DISCOVERY_ENDPOINT);

        // mock tenant discovery failed(failed twice for retry)
        final HttpURLConnection mockedConnectionWithTimeout = AndroidTestMockUtil.getMockedConnectionWithSocketTimeout();
        HttpUrlConnectionFactory.addMockedConnection(mockedConnectionWithTimeout);
        HttpUrlConnectionFactory.addMockedConnection(mockedConnectionWithTimeout);
        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId()), null);
            Assert.fail();
        } catch (final MsalException e) {
            Assert.assertTrue(e.getErrorCode().equals(MsalServiceException.REQUEST_TIMEOUT));
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof SocketTimeoutException);
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);
    }

    @Test
    public void testB2cAuthorityWithTenantDiscovery() throws IOException {
        final AuthorityMetadata authority = AuthorityMetadata.createAuthority(TEST_B2C_AUTHORITY, true);

        // mock tenant discovery succeed.
        AndroidTestMockUtil.mockSuccessTenantDiscovery(AUTHORIZE_ENDPOINT, TOKEN_ENDPOINT);
        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), "test", Telemetry.generateNewRequestId()), null);
        } catch (final MsalException e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertTrue(authority.getAuthorizeEndpoint().equals(AUTHORIZE_ENDPOINT));
        Assert.assertTrue(authority.getTokenEndpoint().equals(TOKEN_ENDPOINT));
        Assert.assertFalse(authority.getIsTenantless());
    }
}
