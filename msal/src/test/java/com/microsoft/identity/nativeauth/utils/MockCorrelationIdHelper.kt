package com.microsoft.identity.nativeauth.utils

import com.microsoft.identity.nativeauth.statemachine.errors.Error
import com.microsoft.identity.nativeauth.statemachine.states.BaseState
import org.mockito.kotlin.whenever

/**
 * Helper methods for mocking correlation ID in States and Errors
 */
fun Error.mockCorrelationId(correlationId: String) {
    whenever(this.correlationId).thenReturn(correlationId)
}

fun BaseState.mockCorrelationId(correlationId: String) {
    whenever(this.correlationId).thenReturn(correlationId)
}
