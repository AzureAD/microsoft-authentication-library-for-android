// Copyright (c) Microsoft Corporation.
// All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.client.internal.controllers

import com.microsoft.identity.client.exception.MsalArgumentException
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.common.java.exception.ArgumentException
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MsalExceptionAdapterTest {
    @Test
    fun argumentExceptionPreservesOperationAndArgumentNames() {
        val brokerException = ArgumentException(
            ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
            "mCallerPackageName",
            "Caller package is not owned by the calling app."
        )

        val converted = MsalExceptionAdapter.msalExceptionFromBaseException(brokerException)

        assertTrue(converted is MsalArgumentException)
        converted as MsalArgumentException
        assertEquals(ArgumentException.ILLEGAL_ARGUMENT_ERROR_CODE, converted.errorCode)
        assertEquals(brokerException.operationName, converted.operationName)
        assertEquals(brokerException.argumentName, converted.argumentName)
        assertSame(brokerException, converted.cause)
    }

    @Test
    fun olderBrokerClientExceptionStillMapsToMsalClientException() {
        val brokerException = ClientException(ErrorStrings.UNKNOWN_CALLER, "Unknown caller.")

        val converted = MsalExceptionAdapter.msalExceptionFromBaseException(brokerException)

        assertTrue(converted is MsalClientException)
        assertEquals(ErrorStrings.UNKNOWN_CALLER, converted.errorCode)
    }
}
