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
