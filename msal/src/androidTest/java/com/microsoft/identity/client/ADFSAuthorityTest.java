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

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class ADFSAuthorityTest {

    final String testUPN = "user.name@foo.com";

    private ADFSAuthority adfsAuthority;

    @Before
    public void setUp() {
        try {
            adfsAuthority = new ADFSAuthority(
                    new URL("https://fs.ngctest.nttest.microsoft.com/adfs/ls/"),
                    true
            );
            adfsAuthority.mAuthorizationEndpoint = "https://fs.lindft6.com/adfs/oauth2/authorize";
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testExistsInValidatedAuthorityCache() {
        adfsAuthority.addToValidatedAuthorityCache(testUPN);
        Assert.assertTrue(adfsAuthority.existsInValidatedAuthorityCache(testUPN));
    }

    @Test
    public void testDoesntExistInValidatedAuthorityCache() {
        Assert.assertFalse(adfsAuthority.existsInValidatedAuthorityCache(testUPN));
    }

    @Test
    public void testPerformInstanceDiscovery() {
        // TODO
    }

    @Test
    public void testPerformInstanceDiscoveryThrowsWhenURLnvalid() {
        try {
            adfsAuthority = new ADFSAuthority(
                    new URL("Not a valid URL"),
                    false
            );
            adfsAuthority.performInstanceDiscovery(UUID.randomUUID(), testUPN);
        } catch (MalformedURLException e) {
            Assert.fail();
        } catch (AuthenticationException e) {
            // NOOP: expected
        }
    }

    @Test
    public void testGetDomainFromUPN() {
        // TODO
    }

    @Test
    public void testGetDomainFromUPNReturnsNullWhenInvalid() {
        // TODO
    }

    @Test
    public void testGetDefaultOpenIdConfigurationEndpoint() {
        // TODO
    }
}
