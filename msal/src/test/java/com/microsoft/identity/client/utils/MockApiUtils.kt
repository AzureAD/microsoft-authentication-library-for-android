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

import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.logging.IRequestContext
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.reflect.Whitebox

class MockApiUtils {
    companion object {
        init {
            MockApi.create()
        }

        private fun setCorrelationIdHeader(correlationId: String) {
            val mockDiagnosticContext = mock<DiagnosticContext>()
            Whitebox.setInternalState(
                DiagnosticContext::class.java,
                "INSTANCE",
                mockDiagnosticContext
            )

            val mockRequestContext = mock<IRequestContext>()
            whenever(mockRequestContext[DiagnosticContext.CORRELATION_ID]).thenReturn(correlationId)
            whenever(mockDiagnosticContext.requestContext).thenReturn(mockRequestContext)
        }

        private fun configureMockApiResponse(endpointType: MockApiEndpointType, responseType: MockApiResponseType, correlationId: String) {
            MockApi.instance.addErrorToStack(
                endpointType = endpointType,
                responseType = responseType,
                correlationId = correlationId
            )
        }

        /**
         * The mock API can be configured to return certain responses based on the correlation ID.
         * This method sets the correlation ID header (through mocking DiagnosticContext) of the signup
         * requests, and configures the mock API to return the desired response.
         * Note: MockApiUtils.configureMockApiResponse() will fail if the mock API configuration endpoint
         * didn't return a success state.
         *
         * @param correlationId The correlation ID to set in the request header of the sign up request,
         * and used to set the mock API response.
         * @param responseType The type of response to return from the mock API.
         */
        @JvmStatic
        fun configureMockApi(
            endpointType: MockApiEndpointType,
            correlationId: String,
            responseType: MockApiResponseType
        ) {
            configureMockApiResponse(
                endpointType = endpointType,
                responseType = responseType,
                correlationId = correlationId
            )
            setCorrelationIdHeader(
                correlationId = correlationId
            )
        }
    }
}
