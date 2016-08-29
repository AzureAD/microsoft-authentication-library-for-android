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

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Tests for {@link HttpUrlConnectionFactory}.
 */
public final class HttpUrlConnectionFactoryTest {

    @After
    public void tearDown() {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    @Test
    public void testSetMockedConnection() throws IOException {
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        Assert.assertTrue(HttpUrlConnectionFactory.createHttpURLConnection(
                Util.getValidRequestUrl()).equals(mockedConnection));
        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);
    }

    @Test
    public void testMultipleMockedConnection() throws IOException {
        final HttpURLConnection firstMockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.addMockedConnection(firstMockedConnection);

        final HttpURLConnection secondMockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.addMockedConnection(secondMockedConnection);

        final HttpURLConnection thirdMockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.addMockedConnection(thirdMockedConnection);

        final int connectionQueueCount = 3;
        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == connectionQueueCount);
        Assert.assertTrue(HttpUrlConnectionFactory.createHttpURLConnection(
                Util.getValidRequestUrl()).equals(firstMockedConnection));
        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == connectionQueueCount - 1);

        Assert.assertTrue(HttpUrlConnectionFactory.createHttpURLConnection(
                Util.getValidRequestUrl()).equals(secondMockedConnection));
        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == connectionQueueCount - 2);
    }

    @Test
    public void testMockedConnectionNotSet() throws IOException {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);
        Assert.assertNotNull(HttpUrlConnectionFactory.createHttpURLConnection(Util.getValidRequestUrl()));
    }
}
