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

import org.junit.Ignore;
import org.junit.Test;

import static com.microsoft.identity.client.PublicClientApplicationConfiguration.isBrokerRedirectUri;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PublicClientApplicationConfigurationTest {

    @Test
    public void testRedirectUriValidationValid() {
        assertTrue(isBrokerRedirectUri("msauth://myPackageName/foo.bar/baz", "myPackageName"));
    }

    @Test
    public void testRedirectUriValidationInvalid() {
        assertFalse(isBrokerRedirectUri("https://myPackageName/foo.bar/baz", "myPackageName"));
    }

    @Test
    public void testRedirectUriValidationWrongPackage() {
        assertFalse(isBrokerRedirectUri("msauth://myPackageName/foo.bar/baz", "notMyPackageName"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullRedirectThrows() {
        final PublicClientApplicationConfiguration config = new PublicClientApplicationConfiguration();
        config.setRedirectUri(null);
        config.validateConfiguration();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyStringRedirectThrows() {
        final PublicClientApplicationConfiguration config = new PublicClientApplicationConfiguration();
        config.setRedirectUri("");
        config.validateConfiguration();
    }

    @Test
    @Ignore // Ignore test due to mocking gaps http://g.co/androidstudio/not-mocked
    public void testValidRedirect() {
        final PublicClientApplicationConfiguration config = new PublicClientApplicationConfiguration();
        config.setRedirectUri("msauth://authority");
        config.validateConfiguration();
    }

    @Test(expected = IllegalArgumentException.class)
    @Ignore // Ignore test due to mocking gaps http://g.co/androidstudio/not-mocked
    public void testStringLiteralNullRedirectThrows() {
        final PublicClientApplicationConfiguration config = new PublicClientApplicationConfiguration();
        config.setRedirectUri("null");
        config.validateConfiguration();
    }
}
