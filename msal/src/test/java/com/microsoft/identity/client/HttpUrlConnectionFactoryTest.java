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
        HttpUrlConnectionFactory.setMockedConnection(null);
    }

    @Test
    public void testSetMockedConnection() throws IOException {
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedConnection(mockedConnection);

        Assert.assertTrue(HttpUrlConnectionFactory.createHttpURLConnection(
                Util.getValidRequestUrl()).equals(mockedConnection));
    }

    @Test
    public void testMockedConnectionNotSet() throws IOException {
        HttpUrlConnectionFactory.setMockedConnection(null);
        Assert.assertNotNull(HttpUrlConnectionFactory.createHttpURLConnection(Util.getValidRequestUrl()));
    }
}
