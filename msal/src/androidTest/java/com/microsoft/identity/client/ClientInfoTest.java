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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link ClientInfo}.
 */
@RunWith(AndroidJUnit4.class)
public final class ClientInfoTest {

    @Test
    public void testEmptyClientInfo() throws MsalException {
        final ClientInfo clientInfo = new ClientInfo("");
        Assert.assertTrue(clientInfo.getUniqueIdentifier().equals(""));
        Assert.assertTrue(clientInfo.getUniqueTenantIdentifier().equals(""));
    }

    @Test
    public void testInvalidRawClientInfo() {
        try {
            new ClientInfo("some_rawClientInfo");
        } catch (final MsalClientException e) {
            Assert.assertTrue(e.getErrorCode().equals(MsalClientException.JSON_PARSE_FAILURE));
        }
    }

    @Test
    public void testHappyPath() {
        final String uid = "some-uid";
        final String utid = "some-utid";

        try {
            final ClientInfo clientInfo = new ClientInfo(AndroidTestUtil.createRawClientInfo(uid, utid));
            Assert.assertTrue(clientInfo.getUniqueIdentifier().equals(uid));
            Assert.assertTrue(clientInfo.getUniqueTenantIdentifier().equals(utid));
        } catch (final MsalClientException e) {
            Assert.fail("unexpected exception.");
        }
    }
}
