//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.
//
//   Permission is hereby granted, free of charge, to any person obtaining a copy
//   of this software and associated documentation files(the "Software"), to deal
//   in the Software without restriction, including without limitation the rights
//   to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//   copies of the Software, and to permit persons to whom the Software is
//   furnished to do so, subject to the following conditions :
//
//   The above copyright notice and this permission notice shall be included in
//   all copies or substantial portions of the Software.
//
//   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//   THE SOFTWARE.

package com.microsoft.identity.client;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

/**
 * Tests for {@link B2CAuthority}.
 */
@RunWith(AndroidJUnit4.class)
public final class B2cAuthorityTest {

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPathSegments() {
        final String b2cAuthorityUrl = "https://someauhority/tfp/sometenant/";
        Authority.createAuthority(b2cAuthorityUrl, false);
    }

    @Test
    public void testValidationEnabledButNotSupported() {
        final String b2cAuthority = "https://someauthority/tfp/sometenant/somepolicy";
        final Authority authority = Authority.createAuthority(b2cAuthority, true);

        try {
            authority.resolveEndpoints(new RequestContext(UUID.randomUUID(), "test"), null);
            Assert.fail("Should reach exception");
        } catch (final AuthenticationException e) {
            Assert.assertTrue(e.getErrorCode().equals(MSALError.UNSUPPORTED_AUTHORITY_VALIDATION));
        }
    }
}
