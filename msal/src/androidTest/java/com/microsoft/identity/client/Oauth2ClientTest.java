package com.microsoft.identity.client;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tests for {@link Oauth2Client}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class Oauth2ClientTest {
    static final UUID CORRELATION_ID = UUID.randomUUID();
    static final String CLIENT_ID = "clientId";
    static final String REFRESH_TOKEN = "test refresh token";
    static final String AUTH_CODE = "testing code";

    @After
    public void tearDown() {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    /**
     * Make sure if body params are sent and we get success response, result is correctly handled.
     */
    @Test
    public void testOauth2ClientRTRequestWithSuccessResponse() throws IOException {
        final Oauth2Client oauth2Client = getOauth2ClientWithCorrelationIdInTheHeader();
        addCommonBodyParameters(oauth2Client);
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.GRANT_TYPE, OauthConstants.Oauth2GrantType.REFRESH_TOKEN);
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.REFRESH_TOKEN, REFRESH_TOKEN);

        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponseWithNoRefreshToken(AndroidTestUtil.TEST_IDTOKEN));
        final OutputStream outputStream = Mockito.mock(OutputStream.class);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(outputStream);
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        final Set<String> expectedRequestMessageSet = new HashSet<>();
        expectedRequestMessageSet.add(OauthConstants.Oauth2Parameters.GRANT_TYPE + "=" + OauthConstants.Oauth2GrantType.REFRESH_TOKEN);
        expectedRequestMessageSet.add(OauthConstants.Oauth2Parameters.REFRESH_TOKEN + "=" + MSALUtils.urlEncode(REFRESH_TOKEN));
        expectedRequestMessageSet.add(OauthConstants.Oauth2Parameters.CLIENT_ID + "=" + MSALUtils.urlEncode(CLIENT_ID));

        final String expectedRequestMessage = MSALUtils.convertSetToString(expectedRequestMessageSet, "&");

        try {
            final TokenResponse response = oauth2Client.getToken(getAuthority(AndroidTestUtil.DEFAULT_AUTHORITY));
            // Verify common headers
            verifyMockConnectionHasCommonHeaders(mockedConnection);

            // Verify body parameters
            Mockito.verify(outputStream).write(AdditionalMatchers.aryEq(expectedRequestMessage.getBytes(MSALUtils.ENCODING_UTF8)));

            // verify response
            Assert.assertNotNull(response);
            Assert.assertTrue(response.getAccessToken().equals(AndroidTestUtil.ACCESS_TOKEN));
        } catch (final RetryableException | IOException  | AuthenticationException e) {
            Assert.fail("Unexpected Exception.");
        }
    }

    /**
     * Make sure if body parameters are sent, we correctly construct the request message and the failure response is correctly
     * handled.
     */
    @Test
    public void testOauth2ClientAuthCodeGrantWithFailureResponse() throws IOException {
        final Oauth2Client oauth2Client = getOauth2ClientWithCorrelationIdInTheHeader();
        addCommonBodyParameters(oauth2Client);
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.GRANT_TYPE, OauthConstants.Oauth2GrantType.AUTHORIZATION_CODE);
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.CODE, AUTH_CODE);

        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithFailureResponse(HttpURLConnection.HTTP_BAD_REQUEST,
                AndroidTestUtil.getErrorResponseMessage("invalid_request"));

        // prepare output stream for mocking
        final OutputStream outputStream = Mockito.mock(OutputStream.class);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(outputStream);
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        try {
            final TokenResponse response = oauth2Client.getToken(getAuthority(AndroidTestUtil.DEFAULT_AUTHORITY));
            // Verify common headers
            verifyMockConnectionHasCommonHeaders(mockedConnection);

            // Verify body parameters
            Mockito.verify(outputStream).write(Mockito.argThat(new ArgumentMatcher<byte[]>() {
                @Override
                public boolean matches(Object argument) {
                    final String message = new String((byte[]) argument);
                    if (message == null) {
                        return false;
                    }

                    final Map<String, String> decodeUrlMap = MSALUtils.decodeUrlToMap(message, "&");
                    if (decodeUrlMap.get(OauthConstants.Oauth2Parameters.GRANT_TYPE).equalsIgnoreCase(
                            OauthConstants.Oauth2GrantType.AUTHORIZATION_CODE)
                            && decodeUrlMap.get(OauthConstants.Oauth2Parameters.CODE).equalsIgnoreCase(AUTH_CODE)
                            && decodeUrlMap.get(OauthConstants.Oauth2Parameters.CLIENT_ID).equalsIgnoreCase(CLIENT_ID)) {
                        return true;
                    }

                    return false;
                }
            }));

            // verify response
            Assert.assertNotNull(response);
            Assert.assertTrue(response.getError().equals("invalid_request"));
            Assert.assertFalse(response.getErrorDescription().isEmpty());
            Assert.assertNotNull(response.getErrorCodes());
            Assert.assertTrue(response.getErrorCodes().length == 2);
        } catch (final RetryableException | IOException | AuthenticationException e) {
            Assert.fail("Unexpected Exception.");
        }
    }

    /**
     * Make sure the right exception is thrown if the returned raw response is not in the JSON format.
     */
    @Test
    public void testOauth2ClientResponseNotInJsonFormat() throws IOException, RetryableException {
        final Oauth2Client oauth2Client = getOauth2ClientWithCorrelationIdInTheHeader();

        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse("some response");
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        try {
            oauth2Client.getToken(getAuthority(AndroidTestUtil.DEFAULT_AUTHORITY));
            Assert.fail();
        } catch (final AuthenticationException e) {
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof JSONException);
        }
    }

    /**
     * Make sure we correctly bubble up the exception if retry fails with timeout.
     */
    @Test
    public void testOauth2ClientPassBackRetryableExceptionRetryFailsWithTimeout() throws IOException,
            AuthenticationException {
        final Oauth2Client oauth2Client = getOauth2ClientWithCorrelationIdInTheHeader();

        // Add two connections with timeout
        HttpUrlConnectionFactory.addMockedConnection(getMockedConnectionWithSocketTimeout());
        HttpUrlConnectionFactory.addMockedConnection(getMockedConnectionWithSocketTimeout());

        try {
            oauth2Client.getToken(getAuthority(AndroidTestUtil.DEFAULT_AUTHORITY));
            Assert.fail();
        } catch (final RetryableException e) {
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof SocketTimeoutException);
        }
    }

    /**
     * Make sure we correctly bubble up the exception and cause if retry fails with 500/503/504.
     */
    @Test
    public void testOauth2ClientPassBackRetryableExceptionRetryFailsWithServerError() throws IOException,
            AuthenticationException {
        final Oauth2Client oauth2Client = getOauth2ClientWithCorrelationIdInTheHeader();

        // Add two connections, one with timeout, one with 500/503/504
        HttpUrlConnectionFactory.addMockedConnection(getMockedConnectionWithSocketTimeout());

        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_INTERNAL_ERROR, "failed response");
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        try {
            oauth2Client.getToken(getAuthority(AndroidTestUtil.DEFAULT_AUTHORITY));
            Assert.fail();
        } catch (final RetryableException e) {
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof AuthenticationException);

            final AuthenticationException exception = (AuthenticationException) e.getCause();
            Assert.assertTrue(exception.getErrorCode().equals(MSALError.RETRY_FAILED_WITH_SERVER_ERROR));
        }
    }

    // TODO: add test for correlation id is not the same as what's sent in the header.

    private Authority getAuthority(final String authorityUrl) {
        return Authority.createAuthority(authorityUrl, false, null);
    }

    private void addCommonBodyParameters(final Oauth2Client oauth2Client) {
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.CLIENT_ID, CLIENT_ID);
    }

    private Oauth2Client getOauth2ClientWithCorrelationIdInTheHeader() {
        final Oauth2Client oauth2Client = new Oauth2Client();
        oauth2Client.addHeader(OauthConstants.OauthHeader.CORRELATION_ID, CORRELATION_ID.toString());

        return oauth2Client;
    }

    private void verifyMockConnectionHasCommonHeaders(final HttpURLConnection mockedConnection) {
        Mockito.verify(mockedConnection).setRequestProperty(Matchers.refEq(OauthConstants.OauthHeader.CORRELATION_ID),
                Matchers.refEq(CORRELATION_ID.toString()));
    }

    private HttpURLConnection getMockedConnectionWithSocketTimeout() throws IOException {
        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSocketTimeout();
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));

        return mockedConnection;
    }
}
