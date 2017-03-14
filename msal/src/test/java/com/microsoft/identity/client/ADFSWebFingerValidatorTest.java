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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ADFSWebFingerValidatorTest {

    @Before
    public void setUp() {
        Logger.getInstance().setEnableLogcatLog(false);
    }

    @Test
    public void testTrustedRealmFieldInitialized()
            throws NoSuchFieldException, IllegalAccessException {
        Field trustedRealmURI = ADFSWebFingerValidator.class.getDeclaredField("TRUSTED_REALM_REL");
        trustedRealmURI.setAccessible(true);
        Assert.assertEquals(
                trustedRealmURI.get(null).toString(),
                "http://schemas.microsoft.com/rel/trusted-realm"
        );
    }

    @Test
    public void testRealmIsTrustedEmptyMetadata() throws URISyntaxException {
        final URI testAuthority = new URI("https://fs.ngctest.nttest.microsoft.com/adfs/ls/");
        WebFingerMetadata metadata = new WebFingerMetadata();
        Assert.assertEquals(
                false,
                ADFSWebFingerValidator.realmIsTrusted(
                        new RequestContext(UUID.randomUUID(), ""),
                        testAuthority,
                        metadata
                )
        );
    }

    @Test
    public void testRealmIsTrusted() throws URISyntaxException {
        final URI testAuthority = new URI("https://fs.ngctest.nttest.microsoft.com/adfs/ls/");
        WebFingerMetadata metadata = new WebFingerMetadata();
        final Link link = new Link();
        link.setHref("https://fs.ngctest.nttest.microsoft.com");
        link.setRel("http://schemas.microsoft.com/rel/trusted-realm");
        List<Link> links = new ArrayList<>();
        links.add(link);
        metadata.setLinks(links);
        Assert.assertEquals(
                true,
                ADFSWebFingerValidator.realmIsTrusted(
                        new RequestContext(UUID.randomUUID(), ""),
                        testAuthority,
                        metadata
                )
        );
    }

    @Test
    public void testRealmIsNotTrusted() throws URISyntaxException {
        final URI testAuthority = new URI("https://fs.ngctest.nttest.microsoft.com/adfs/ls/");
        WebFingerMetadata metadata = new WebFingerMetadata();
        List<Link> links = new ArrayList<>();
        metadata.setLinks(links);
        Assert.assertEquals(
                false,
                ADFSWebFingerValidator.realmIsTrusted(
                        new RequestContext(UUID.randomUUID(), ""),
                        testAuthority,
                        metadata
                )
        );
    }
}
