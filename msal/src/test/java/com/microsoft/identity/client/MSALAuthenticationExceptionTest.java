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

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link MSALAuthenticationException}.
 */
public final class MSALAuthenticationExceptionTest {
    private static final String TEST_ERROR_DESCRIPTION = "test error description";

    @Test
    public void testEmptyConstructor() {
        final MSALAuthenticationException msalAuthenticationException = new MSALAuthenticationException();
        Assert.assertNull(msalAuthenticationException.getErrorCode());
        Assert.assertNull(msalAuthenticationException.getMessage());
        Assert.assertNull(msalAuthenticationException.getCause());
    }

    @Test
    public void testWithErrorCode() {
        final MSALAuthenticationException msalAuthenticationException = new MSALAuthenticationException(
                MSALError.RETRY_FAILED_WITH_SERVER_ERROR);
        Assert.assertTrue(msalAuthenticationException.getErrorCode().equals(MSALError.RETRY_FAILED_WITH_SERVER_ERROR));
        Assert.assertNotNull(msalAuthenticationException.getMessage());
        Assert.assertTrue(msalAuthenticationException.getMessage().equals(
                MSALError.RETRY_FAILED_WITH_SERVER_ERROR.getDescription()));
        Assert.assertNull(msalAuthenticationException.getCause());
    }

    @Test
    public void testWithErrorCodeAndDescription() {
        final MSALAuthenticationException msalAuthenticationException = new MSALAuthenticationException(
                MSALError.RETRY_FAILED_WITH_SERVER_ERROR, TEST_ERROR_DESCRIPTION);
        Assert.assertTrue(msalAuthenticationException.getErrorCode().equals(MSALError.RETRY_FAILED_WITH_SERVER_ERROR));
        Assert.assertTrue(msalAuthenticationException.getMessage().equals(TEST_ERROR_DESCRIPTION));
        Assert.assertNull(msalAuthenticationException.getCause());
    }

    @Test
    public void testWithErrorCodeAndDescriptAndCause() {
        final Throwable throwable = new Throwable(TEST_ERROR_DESCRIPTION);
        final MSALAuthenticationException msalAuthenticationException = new MSALAuthenticationException(
                MSALError.RETRY_FAILED_WITH_SERVER_ERROR, TEST_ERROR_DESCRIPTION, throwable);
        Assert.assertTrue(msalAuthenticationException.getErrorCode().equals(MSALError.RETRY_FAILED_WITH_SERVER_ERROR));
        Assert.assertTrue(msalAuthenticationException.getMessage().equals(TEST_ERROR_DESCRIPTION));
        Assert.assertNotNull(msalAuthenticationException.getCause());
        Assert.assertTrue(msalAuthenticationException.getCause().getMessage().equals(TEST_ERROR_DESCRIPTION));
    }
}
