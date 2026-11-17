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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Unit tests for {@link PoPAuthenticationScheme} and its {@link PoPAuthenticationScheme.Builder}.
 */
public class PoPAuthenticationSchemeTest {

    private static URL testUrl() throws MalformedURLException {
        return new URL("https://contoso.com/path?query=1");
    }

    @Test
    public void build_withAllParams_populatesAllFields() throws MalformedURLException {
        final URL url = testUrl();
        final PoPAuthenticationScheme scheme = PoPAuthenticationScheme.builder()
                .withUrl(url)
                .withHttpMethod(HttpMethod.POST)
                .withNonce("nonce-123")
                .withClientClaims("{\"claim\":\"value\"}")
                .build();

        assertSame(url, scheme.getUrl());
        assertEquals(HttpMethod.POST.name(), scheme.getHttpMethod());
        assertEquals("nonce-123", scheme.getNonce());
        assertEquals("{\"claim\":\"value\"}", scheme.getClientClaims());
        assertEquals(PopAuthenticationSchemeInternal.SCHEME_POP, scheme.getName());
    }

    @Test
    public void build_withoutHttpMethod_returnsNullHttpMethod() throws MalformedURLException {
        final PoPAuthenticationScheme scheme = PoPAuthenticationScheme.builder()
                .withUrl(testUrl())
                .build();

        assertNull(scheme.getHttpMethod());
        assertNull(scheme.getNonce());
        assertNull(scheme.getClientClaims());
    }

    @Test
    public void build_withoutUrl_throwsIllegalArgument() {
        try {
            PoPAuthenticationScheme.builder()
                    .withHttpMethod(HttpMethod.GET)
                    .build();
            fail("Expected IllegalArgumentException when URL is missing.");
        } catch (final IllegalArgumentException e) {
            // Expected: URL is a required parameter.
        }
    }

    @Test
    public void httpMethod_valueOf_roundTripsAllValues() {
        for (final HttpMethod method : HttpMethod.values()) {
            assertEquals(method, HttpMethod.valueOf(method.name()));
        }
    }
}
