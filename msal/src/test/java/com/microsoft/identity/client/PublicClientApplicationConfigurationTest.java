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

import com.microsoft.identity.client.configuration.AccountMode;
import com.microsoft.identity.common.java.authorities.Authority;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.microsoft.identity.client.PublicClientApplicationConfiguration.isBrokerRedirectUri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PublicClientApplicationConfigurationTest {

    private static void setAuthorities(final PublicClientApplicationConfiguration config,
                                       final List<Authority> authorities) throws Exception {
        final Field field = PublicClientApplicationConfiguration.class.getDeclaredField("mAuthorities");
        field.setAccessible(true);
        field.set(config, authorities);
    }

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

    @Test
    public void testGetDefaultAuthorityNullWhenNoAuthorities() {
        assertNull(new PublicClientApplicationConfiguration().getDefaultAuthority());
        assertFalse(new PublicClientApplicationConfiguration().isDefaultAuthorityConfigured());
    }

    @Test
    public void testGetDefaultAuthorityReturnsSingleAuthority() throws Exception {
        final PublicClientApplicationConfiguration config = new PublicClientApplicationConfiguration();
        final Authority authority = Mockito.mock(Authority.class);
        setAuthorities(config, Collections.singletonList(authority));

        assertSame(authority, config.getDefaultAuthority());
        assertTrue(config.isDefaultAuthorityConfigured());
    }

    @Test
    public void testGetDefaultAuthorityReturnsMarkedDefaultWhenMultiple() throws Exception {
        final PublicClientApplicationConfiguration config = new PublicClientApplicationConfiguration();
        final Authority nonDefault = Mockito.mock(Authority.class);
        final Authority markedDefault = Mockito.mock(Authority.class);
        Mockito.when(nonDefault.getDefault()).thenReturn(false);
        Mockito.when(markedDefault.getDefault()).thenReturn(true);
        setAuthorities(config, Arrays.asList(nonDefault, markedDefault));

        assertSame(markedDefault, config.getDefaultAuthority());
        assertTrue(config.isDefaultAuthorityConfigured());
    }

    @Test
    public void testGetDefaultAuthorityNullWhenMultipleAndNoneDefault() throws Exception {
        final PublicClientApplicationConfiguration config = new PublicClientApplicationConfiguration();
        final Authority first = Mockito.mock(Authority.class);
        final Authority second = Mockito.mock(Authority.class);
        Mockito.when(first.getDefault()).thenReturn(false);
        Mockito.when(second.getDefault()).thenReturn(false);
        setAuthorities(config, Arrays.asList(first, second));

        assertNull(config.getDefaultAuthority());
        assertFalse(config.isDefaultAuthorityConfigured());
    }

    @Test
    public void testMergeConfigurationOverlaysNonNullValues() {
        final PublicClientApplicationConfiguration base = new PublicClientApplicationConfiguration();
        base.setRedirectUri("msauth://base/aaa");

        final PublicClientApplicationConfiguration override = new PublicClientApplicationConfiguration();
        override.setRedirectUri("msauth://override/bbb");
        override.setAccountMode(AccountMode.SINGLE);
        override.setPowerOptCheckEnabled(Boolean.TRUE);
        override.setWebViewZoomEnabled(true);
        override.setWebViewZoomControlsEnabled(true);

        base.mergeConfiguration(override);

        assertEquals("msauth://override/bbb", base.getRedirectUri());
        assertEquals(AccountMode.SINGLE, base.getAccountMode());
        assertTrue(base.isPowerOptCheckForEnabled());
        assertTrue(base.isWebViewZoomEnabled());
        assertTrue(base.isWebViewZoomControlsEnabled());
    }

    @Test
    public void testMergeConfigurationKeepsBaseValuesWhenOverrideIsEmpty() {
        final PublicClientApplicationConfiguration base = new PublicClientApplicationConfiguration();
        base.setRedirectUri("msauth://base/aaa");
        base.setAccountMode(AccountMode.SINGLE);

        base.mergeConfiguration(new PublicClientApplicationConfiguration());

        assertEquals("msauth://base/aaa", base.getRedirectUri());
        assertEquals(AccountMode.SINGLE, base.getAccountMode());
    }
}
