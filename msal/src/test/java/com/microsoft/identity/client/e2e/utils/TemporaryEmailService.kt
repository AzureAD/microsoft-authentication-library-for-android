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

package com.microsoft.identity.client.e2e.utils

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.nativeauth.MockApiEndpoint
import com.microsoft.identity.common.nativeauth.MockApiResponseType
import java.net.URL

const val CORRELATION_ID =  "correlationId"
const val ENDPOINT = "endpoint"
const val RESPONSE_LIST = "responseList"

/**
 * Class for interacting with temporary mail service provider 1SecMail.
 */
class TemporaryEmailService private constructor(
    private val httpClient: UrlConnectionHttpClient = UrlConnectionHttpClient.getDefaultInstance()
) {
    companion object {
        private const val BASE_URL = "https://www.1secmail.com/api/v1/"

        lateinit var instance: TemporaryEmailService

        fun create() {
            if (this::instance.isInitialized) {
                throw IllegalStateException("TemporaryEmailService already initialised")
            } else {
                instance = TemporaryEmailService()
            }
        }
    }

//    fun generateEmailAddress(): String {
//
//    }


    fun performRequest(endpointType: MockApiEndpoint, responseType: MockApiResponseType, correlationId: String) {
//        val addResponseUrl = URL(BASE_URL)
//        val request = Request(
//            correlationId = correlationId,
//            endpoint = endpointType.stringValue,
//            responseList = listOf(responseType.stringValue)
//        )
//        val encodedRequest = getEncodedRequest(request)
//
//        val result = httpClient.post(
//            addResponseUrl,
//            headers,
//            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
//        )
//        assertTrue(result.statusCode == 200)
    }
}

/**
 * Data class to represent the request object send to the MockAPI for Native Auth
 */
data class Request(
    @SerializedName(CORRELATION_ID) val correlationId: String,
    @SerializedName(ENDPOINT) val endpoint: String,
    @SerializedName(RESPONSE_LIST) val responseList: List<String>
)
