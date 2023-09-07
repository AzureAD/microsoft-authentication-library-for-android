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
