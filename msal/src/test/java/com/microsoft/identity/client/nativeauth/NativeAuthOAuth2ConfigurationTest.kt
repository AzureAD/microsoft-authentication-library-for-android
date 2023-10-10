package com.microsoft.identity.client.nativeauth

import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Configuration
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URI
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class NativeAuthOAuth2ConfigurationTest {

    @Test
    fun testNoTrailingSlash() {
        val authorityUrlString = "https://login.microsoftonline.com/samtoso.onmicrosoft.com"
        val authorityUrl = URI(authorityUrlString).toURL()

        val configuration = NativeAuthOAuth2Configuration(
            authorityUrl = authorityUrl,
            clientId = "1234",
            challengeType = "oob password redirect",
            useMockApiForNativeAuth = false
        )

        val signUpEndpoint = configuration.getSignUpStartEndpoint()

        assertEquals(URL("https://login.microsoftonline.com/samtoso.onmicrosoft.com/signup/v1.0/start"), signUpEndpoint)
    }

    @Test
    fun testTrailingSlash() {
        val authorityUrlString = "https://login.microsoftonline.com/samtoso.onmicrosoft.com/"
        val authorityUrl = URI(authorityUrlString).toURL()

        val configuration = NativeAuthOAuth2Configuration(
            authorityUrl = authorityUrl,
            clientId = "1234",
            challengeType = "oob password redirect",
            useMockApiForNativeAuth = false
        )

        val signUpEndpoint = configuration.getSignUpStartEndpoint()

        assertEquals(URL("https://login.microsoftonline.com/samtoso.onmicrosoft.com/signup/v1.0/start"), signUpEndpoint)
    }
}
