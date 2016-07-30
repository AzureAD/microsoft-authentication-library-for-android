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

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 3);
        Assert.assertTrue(HttpUrlConnectionFactory.createHttpURLConnection(
                Util.getValidRequestUrl()).equals(firstMockedConnection));
        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 2);

        Assert.assertTrue(HttpUrlConnectionFactory.createHttpURLConnection(
                Util.getValidRequestUrl()).equals(secondMockedConnection));
        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 1);
    }

    @Test
    public void testMockedConnectionNotSet() throws IOException {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);
        Assert.assertNotNull(HttpUrlConnectionFactory.createHttpURLConnection(Util.getValidRequestUrl()));
    }
}
