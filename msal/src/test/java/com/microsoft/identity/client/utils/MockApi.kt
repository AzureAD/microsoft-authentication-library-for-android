package com.microsoft.identity.client.utils

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.net.HttpConstants
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.util.ObjectMapper
import org.junit.Assert.assertTrue
import java.net.URL
import java.util.TreeMap

class MockApi private constructor(
    private val httpClient: UrlConnectionHttpClient = UrlConnectionHttpClient.getDefaultInstance()
) {
    companion object {
        private const val CONFIG_BASE_URL = "https://native-ux-mock-api.azurewebsites.net/config"
        private const val RESPONSE_URL = "$CONFIG_BASE_URL/response"

        private val headers = TreeMap<String, String?>().also {
            it[HttpConstants.HeaderField.CONTENT_TYPE] = "application/json"
        }

        lateinit var instance: MockApi

        fun create() {
            if (this::instance.isInitialized) {
                throw IllegalStateException("MockApi already initialised")
            } else {
                instance = MockApi()
            }
        }

        private fun getEncodedRequest(request: Request): String {
            return ObjectMapper.serializeObjectToJsonString(request)
        }
    }

    fun addErrorToStack(endpointType: MockApiEndpointType, responseType: MockApiResponseType, correlationId: String) {
        val requestUrl = URL(RESPONSE_URL)
        val request = Request(
            correlationId = correlationId,
            endpoint = endpointType.stringValue,
            responseList = listOf(responseType.stringValue)
        )
        val encodedRequest = getEncodedRequest(request)

        val result = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        assertTrue(result.statusCode == 200)
    }
}

data class Request(
    @SerializedName("correlationId") val correlationId: String,
    @SerializedName("endpoint") val endpoint: String,
    @SerializedName("responseList") val responseList: List<String>
)
