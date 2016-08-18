package com.microsoft.identity.client;

import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * Tests for {@link Oauth2Client}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class Oauth2ClientTest {
    static final UUID CORRELATION_ID = UUID.randomUUID();
    static final String CLIENT_ID = "clientId";

    @After
    public void tearDown() {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    @Test
    public void testOauthClientWithQueryParam() throws UnsupportedEncodingException {
        final Oauth2Client oauth2Client = getOauth2ClientWithCorrelationIdInTheHeader();
        oauth2Client.addQueryParameter(OauthConstants.Oauth2Parameters.POLICY, "p1 p2");

        final URL tokenEndpoint = oauth2Client.getTokenEndpoint(getAuthority(AndroidTestUtil.DEFAULT_AUTHORITY).getAuthorityUrl());
        final String expectedUrl = AndroidTestUtil.DEFAULT_AUTHORITY + Oauth2Client.DEFAULT_TOKEN_ENDPOINT + "?p=p1+p2";

        Assert.assertTrue(tokenEndpoint.toString().equals(expectedUrl));
    }

    @Test
    public void testOauth2ClientRTRequestWithSuccessResponse() throws IOException {
        final Oauth2Client oauth2Client = getOauth2ClientWithCorrelationIdInTheHeader();

        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponse());
    }

    private Oauth2Client getOauth2ClientWithCorrelationIdInTheHeader() {
        final Oauth2Client oauth2Client = new Oauth2Client();
        oauth2Client.addHeader(OauthConstants.OauthHeader.CORRELATION_ID, CORRELATION_ID.toString());

        return oauth2Client;
    }

    Authority getAuthority(final String authorityUrl) {
        return new Authority(authorityUrl, false);
    }

    private void addCommonBodyParameters(final Oauth2Client oauth2Client) {
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.CLIENT_ID, CLIENT_ID);
    }
}
