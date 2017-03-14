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
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.UUID;

/**
 * Unit test for {@link Authority}.
 */
@RunWith(AndroidJUnit4.class)
public final class AuthorityTest {
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
        Authority.createAuthority("http://test.com", false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAuthorityStartWithNonHttpProtocol() {
        Authority.createAuthority("file://test.com", false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAuthorityNotContainPath() {
        Authority.createAuthority("https://test.com", false);
    }

    @Test
    public void testAuthorityContainFragment() {
        final Authority authority = Authority.createAuthority("https://test.com/abc#token=123", false);
        authority.equals("https://test.com/abc");
    }

    @Test
    public void testAuthorityContainQP() {
        final Authority authority = Authority.createAuthority(
                "https://login.windows.net/common?resource=2343&client_id=234", false);
        authority.equals("https://login.windows.net/common");
    }

    /**
     * We don't support adfs as authority for BUILD.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAdfsAuthorityValidationEnabled() {
        Authority.createAuthority("https://somehost/adfs", true);
    }

    /**
     * We don't support adfs as authority for BUILD.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAdfsAuthorityValidationDisabled() {
        Authority.createAuthority("https://somehost/adfs", false);
    }

    @Test
    public void testAuthorityWithTrailingSlash() throws IOException {
        final String authorityWithTrailingSlash = TEST_AUTHORITY + "/";
        final Authority authority = Authority.createAuthority(authorityWithTrailingSlash, false);

        Assert.assertFalse(authority.getAuthority().toString().endsWith("/"));

        // mock tenant discovery response
        AndroidTestMockUtil.mockSuccessTenantDiscovery(AUTHORIZE_ENDPOINT, TOKEN_ENDPOINT);

        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), ""), null);
        } catch (AuthenticationException e) {
            Assert.fail();
        }

        Assert.assertTrue(authority.getAuthorizeEndpoint().equals(AUTHORIZE_ENDPOINT));
        Assert.assertTrue(authority.getTokenEndpoint().equals(TOKEN_ENDPOINT));
    }

    @Test
    public void testAuthorityWithoutTrailingSlash() throws IOException {
        final Authority authority = Authority.createAuthority(TEST_AUTHORITY, false);

        // mock tenant discovery response
        AndroidTestMockUtil.mockSuccessTenantDiscovery(AUTHORIZE_ENDPOINT, TOKEN_ENDPOINT);
        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), ""), null);
        } catch (AuthenticationException e) {
            Assert.fail();
        }

        Assert.assertTrue(authority.getAuthorizeEndpoint().equals(AUTHORIZE_ENDPOINT));
        Assert.assertTrue(authority.getTokenEndpoint().equals(TOKEN_ENDPOINT));
    }

    @Test
    public void testAuthorityValidationWithTrustedHost() {
        // make sure no mocked connection in the queue
        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);
        final Authority authorityAzure1 = Authority.createAuthority("https://login.windows.net/sometenant", true);
        performAuthorityValidationAndVerify(authorityAzure1);

        final Authority authorityAzure2 = Authority.createAuthority("https://login.microsoftonline.com/sometenant", true);
        performAuthorityValidationAndVerify(authorityAzure2);

        final Authority authorityChina = Authority.createAuthority("https://login.chinacloudapi.cn/sometenant", true);
        performAuthorityValidationAndVerify(authorityChina);

        final Authority authorityGermany = Authority.createAuthority("https://login.microsoftonline.de/sometenant", true);
        performAuthorityValidationAndVerify(authorityGermany);

        final Authority authorityUSGovernment = Authority.createAuthority("https://login-us.microsoftonline.com/sometenant", true);
        performAuthorityValidationAndVerify(authorityUSGovernment);
    }

    @Test
    public void testB2cAuthorityValidationWithTrustedHost() {
        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);
        final Authority authorityAzure1 = Authority.createAuthority("https://login.windows.net/tfp/sometenant/policy", true);
        performAuthorityValidationAndVerify(authorityAzure1);

        final Authority authorityAzure2 = Authority.createAuthority("https://login.microsoftonline.com/tfp/sometenant/policy", true);
        performAuthorityValidationAndVerify(authorityAzure2);

        final Authority authorityChina = Authority.createAuthority("https://login.chinacloudapi.cn/tfp/sometenant/policy", true);
        performAuthorityValidationAndVerify(authorityChina);

        final Authority authorityGermany = Authority.createAuthority("https://login.microsoftonline.de/tfp/sometenant/policy", true);
        performAuthorityValidationAndVerify(authorityGermany);

        final Authority authorityUSGovernment = Authority.createAuthority("https://login-us.microsoftonline.com/tfp/sometenant/policy", true);
        performAuthorityValidationAndVerify(authorityUSGovernment);
    }

    private void performAuthorityValidationAndVerify(final Authority authority) {
        try {
            final String openIdConfigEndpoint = authority.performInstanceDiscovery(new RequestContext(UUID.randomUUID(), ""), null);
            Assert.assertTrue(openIdConfigEndpoint.equals(authority.getDefaultOpenIdConfigurationEndpoint()));
        } catch (AuthenticationException e) {
            Assert.fail();
        }
    }

    @Test
    public void testAuthorityValidation() throws IOException {
        final Authority authority = Authority.createAuthority(TEST_AUTHORITY, true);

        // mock success instance discovery
        AndroidTestMockUtil.mockSuccessInstanceDiscovery(TENANT_DISCOVERY_ENDPOINT);
        AndroidTestMockUtil.mockSuccessTenantDiscovery(AUTHORIZE_ENDPOINT, TOKEN_ENDPOINT);

        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), ""), null);
        } catch (AuthenticationException e) {
            Assert.fail();
        }

        Assert.assertTrue(authority.getAuthorizeEndpoint().equals(AUTHORIZE_ENDPOINT));
        Assert.assertTrue(authority.getTokenEndpoint().equals(TOKEN_ENDPOINT));

        // make sure no more mocked httpurlconnection is queued
        HttpUrlConnectionFactory.clearMockedConnectionQueue();

        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), ""), null);
        } catch (AuthenticationException e) {
            Assert.fail();
        }

        Assert.assertTrue(authority.getAuthorizeEndpoint().equals(AUTHORIZE_ENDPOINT));
        Assert.assertTrue(authority.getTokenEndpoint().equals(TOKEN_ENDPOINT));

        // create new authority with the same authority url again
        final Authority authority2 = Authority.createAuthority(TEST_AUTHORITY, true);
        try {
            authority2.resolveEndpoints(new RequestContext(UUID.randomUUID(), ""), null);
        } catch (AuthenticationException e) {
            Assert.fail();
        }

        Assert.assertTrue(authority2.getAuthorizeEndpoint().equals(AUTHORIZE_ENDPOINT));
        Assert.assertTrue(authority2.getTokenEndpoint().equals(TOKEN_ENDPOINT));
    }

    /**
     * Verify that if instance discovery fails(server returns error and error_description), we return the error error back.
     */
    @Test
    public void testInstanceDiscoveryFailed() throws IOException {
        final String authorityString = "https://some.authority/endpoint";
        final Authority authority = Authority.createAuthority(authorityString, true);

        AndroidTestMockUtil.mockFailedGetRequest(HttpURLConnection.HTTP_BAD_REQUEST,
                AndroidTestUtil.getErrorResponseMessage("invalid_request"));
        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), ""), null);
            Assert.fail();
        } catch (final AuthenticationException e) {
            Assert.assertTrue(e.getErrorCode().equals(MSALError.AUTHORITY_VALIDATION_FAILED));
        }
    }

    /**
     * Verify if server returns an invalid JSON response for instance discovery, correct error are returned back.
     */
    @Test
    public void testInstanceDiscoveryNonJsonResponse() throws IOException {
        final String authorityString = "https://some.authority/endpoint";
        final Authority authority = Authority.createAuthority(authorityString, true);

        AndroidTestMockUtil.mockFailedGetRequest(HttpURLConnection.HTTP_BAD_REQUEST, "some_failure_response");
        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), ""), null);
            Assert.fail();
        } catch (final AuthenticationException e) {
            Assert.assertTrue(e.getErrorCode().equals(MSALError.JSON_PARSE_FAILURE));
        }
    }

    @Test
    public void testInstanceDiscoveryFailedWithTimeout() throws IOException {
        final String authorityString = "https://some.authority/endpoint";
        final Authority authority = Authority.createAuthority(authorityString, true);

        // mock that instance discovery failed with timeout
        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSocketTimeout();
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), ""), null);
            Assert.fail();
        } catch (final AuthenticationException e) {
            Assert.assertTrue(e.getErrorCode().equals(MSALError.SERVER_ERROR));
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof SocketTimeoutException);
        }
    }

    @Test
    public void testTenantDiscoveryFailed() throws IOException {
        final String authorityString = "https://some.authority/endpoint";
        final Authority authority = Authority.createAuthority(authorityString, true);

        // mock instance discovery succeed
        AndroidTestMockUtil.mockSuccessInstanceDiscovery(TENANT_DISCOVERY_ENDPOINT);

        // mock tenant discovery failed
        AndroidTestMockUtil.mockFailedGetRequest(HttpURLConnection.HTTP_BAD_REQUEST, AndroidTestUtil.getErrorResponseMessage("invalid_instance"));
        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), ""), null);
            Assert.fail();
        } catch (final AuthenticationException e) {
            Assert.assertTrue(e.getErrorCode().equals(MSALError.TENANT_DISCOVERY_FAILED));
        }
    }

    @Test
    public void testTenantDiscoveryFailedWithInvalidJsonResponse() throws IOException {
        final String authorityString = "https://some.authority/endpoint";
        final Authority authority = Authority.createAuthority(authorityString, true);

        // mock instance discovery succeed
        AndroidTestMockUtil.mockSuccessInstanceDiscovery(TENANT_DISCOVERY_ENDPOINT);

        // mock tenant discovery failed
        AndroidTestMockUtil.mockFailedGetRequest(HttpURLConnection.HTTP_BAD_REQUEST, "some error");
        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), ""), null);
            Assert.fail();
        } catch (final AuthenticationException e) {
            Assert.assertTrue(e.getErrorCode().equals(MSALError.JSON_PARSE_FAILURE));
        }
    }

    @Test
    public void testTenantDiscoveryFailedWithTimeout() throws IOException {
        final String authorityString = "https://some.authority/endpoint";
        final Authority authority = Authority.createAuthority(authorityString, true);

        // mock instance discovery succeed
        AndroidTestMockUtil.mockSuccessInstanceDiscovery(TENANT_DISCOVERY_ENDPOINT);

        // mock tenant discovery failed(failed twice for retry)
        final HttpURLConnection mockedConnectionWithTimeout = AndroidTestMockUtil.getMockedConnectionWithSocketTimeout();
        HttpUrlConnectionFactory.addMockedConnection(mockedConnectionWithTimeout);
        HttpUrlConnectionFactory.addMockedConnection(mockedConnectionWithTimeout);
        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), ""), null);
            Assert.fail();
        } catch (final AuthenticationException e) {
            Assert.assertTrue(e.getErrorCode().equals(MSALError.SERVER_ERROR));
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof SocketTimeoutException);
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);
    }

    @Test
    public void testB2cAuthorityWithTenantDiscovery() throws IOException {
        final Authority authority = Authority.createAuthority(TEST_B2C_AUTHORITY, true);

        // mock tenant discovery succeed.
        AndroidTestMockUtil.mockSuccessTenantDiscovery(AUTHORIZE_ENDPOINT, TOKEN_ENDPOINT);
        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), "test"), null);
        } catch (final AuthenticationException e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertTrue(authority.getAuthorizeEndpoint().equals(AUTHORIZE_ENDPOINT));
        Assert.assertTrue(authority.getTokenEndpoint().equals(TOKEN_ENDPOINT));
    }
}
