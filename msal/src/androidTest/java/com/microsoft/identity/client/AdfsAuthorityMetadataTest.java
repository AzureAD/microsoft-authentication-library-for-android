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

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class AdfsAuthorityMetadataTest {

    private static final RequestContext REQUEST_CONTEXT = new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId());

    private final String mTestUPN = "user.name@foo.com";

    private AdfsAuthorityMetadata mAdfsAuthority;

    @Before
    public void setUp() {
        try {
            initializeAuthority();
            final HttpURLConnection mockDrsConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(DrsMetadataRequestorTest.RESPONSE);
            final HttpURLConnection mockWebFinger = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(WebFingerMetadataRequestorTest.RESPONSE);
            HttpUrlConnectionFactory.addMockedConnection(mockDrsConnection);
            HttpUrlConnectionFactory.addMockedConnection(mockWebFinger);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    private void initializeAuthority() throws MalformedURLException {
        mAdfsAuthority = new AdfsAuthorityMetadata(
                new URL("https://fs.ngctest.nttest.microsoft.com/adfs/ls/"),
                true
        );
        mAdfsAuthority.mAuthorizationEndpoint = "https://fs.ngctest.nttest.microsoft.com/adfs/oauth2/authorize";
    }

    // ADFS is not a supported instance for BUILD.
    @Ignore
    @Test
    public void testExistsInValidatedAuthorityCache() {
        mAdfsAuthority.addToResolvedAuthorityCache(mTestUPN);
        Assert.assertTrue(mAdfsAuthority.existsInResolvedAuthorityCache(mTestUPN));
    }

    // ADFS is not a supported instance for BUILD.
    @Ignore
    @Test
    public void testDoesntExistInValidatedAuthorityCache() {
        Assert.assertFalse(mAdfsAuthority.existsInResolvedAuthorityCache(mTestUPN));
    }

    // ADFS is not a supported instance for BUILD.
    @Ignore
    @Test
    public void testPerformInstanceDiscovery() {
        try {
            final AdfsAuthorityMetadata authority = new AdfsAuthorityMetadata(
                    new URL("https://fs.lindft6.com/adfs/ls/"),
                    true
            );
            Assert.assertEquals(
                    "https://fs.lindft6.com/adfs/.well-known/openid-configuration",
                    authority.performInstanceDiscovery(REQUEST_CONTEXT, mTestUPN)
            );
        } catch (final MsalServiceException | MsalClientException | MalformedURLException e) {
            Assert.fail();
        }
    }

    // ADFS is not a supported instance for BUILD.
    @Ignore
    @Test
    public void testPerformInstanceDiscoveryThrowsWhenURLnvalid() {
        try {
            mAdfsAuthority = new AdfsAuthorityMetadata(
                    new URL("file:/Users/RFC2396 noncompliant"),
                    true
            );
            mAdfsAuthority.performInstanceDiscovery(REQUEST_CONTEXT, mTestUPN);
        } catch (MalformedURLException e) {
            Assert.fail();
        } catch (final MsalClientException | MsalServiceException e) {
            // NOOP: expected
            return;
        }
        Assert.fail();
    }

    // ADFS is not a supported instance for BUILD.
    @Ignore
    @Test
    public void testGetDomainFromUPN() {
        Assert.assertEquals(
                "foo.com",
                AdfsAuthorityMetadata.getDomainFromUPN(mTestUPN)
        );
    }

    // ADFS is not a supported instance for BUILD.
    @Ignore
    @Test
    public void testGetDomainFromUPNReturnsNullWhenInvalid() {
        Assert.assertNull(
                AdfsAuthorityMetadata.getDomainFromUPN("user_no_domain")
        );
    }

    // ADFS is not a supported instance for BUILD.
    @Ignore
    @Test
    public void testGetDefaultOpenIdConfigurationEndpoint() {
        Assert.assertEquals(
                "https://fs.ngctest.nttest.microsoft.com/adfs/.well-known/openid-configuration",
                mAdfsAuthority.getDefaultOpenIdConfigurationEndpoint()
        );
    }
}
