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

package com.microsoft.identity.client.exception;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link MsalException}.
 */
public final class MsalExceptionTest {
    private static final String TEST_ERROR_DESCRIPTION = "test error description";

    @Test
    public void testEmptyConstructor() {
        final MsalException msalException = new MsalException();
        Assert.assertNull(msalException.getErrorCode());
        Assert.assertTrue(msalException.getMessage().isEmpty());
        Assert.assertNull(msalException.getCause());
    }

    @Test
    public void testWithErrorCode() {
        final MsalException msalException = new MsalException(
                MsalClientException.INVALID_JWT);
        Assert.assertTrue(msalException.getErrorCode().equals(MsalClientException.INVALID_JWT));
        Assert.assertTrue(msalException.getMessage().isEmpty());
        Assert.assertNull(msalException.getCause());
    }

    @Test
    public void testWithErrorCodeAndDescription() {
        final MsalException msalException = new MsalException(
                MsalServiceException.ACCESS_DENIED, TEST_ERROR_DESCRIPTION);
        Assert.assertTrue(msalException.getErrorCode().equals(MsalServiceException.ACCESS_DENIED));
        Assert.assertTrue(msalException.getMessage().equals(TEST_ERROR_DESCRIPTION));
        Assert.assertNull(msalException.getCause());
    }

    @Test
    public void testWithErrorCodeAndDescriptAndCause() {
        final Throwable throwable = new Throwable(TEST_ERROR_DESCRIPTION);
        final MsalException msalException = new MsalException(
                MsalClientException.AUTHORITY_VALIDATION_NOT_SUPPORTED, TEST_ERROR_DESCRIPTION, throwable);
        Assert.assertTrue(msalException.getErrorCode().equals(MsalClientException.AUTHORITY_VALIDATION_NOT_SUPPORTED));
        Assert.assertTrue(msalException.getMessage().equals(TEST_ERROR_DESCRIPTION));
        Assert.assertNotNull(msalException.getCause());
        Assert.assertTrue(msalException.getCause().getMessage().equals(TEST_ERROR_DESCRIPTION));
    }
}
