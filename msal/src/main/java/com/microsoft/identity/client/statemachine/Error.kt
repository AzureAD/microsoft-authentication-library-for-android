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

package com.microsoft.identity.client.statemachine

sealed class Error(
    open val error: String? = null,
    open val errorMessage: String?,
    open val correlationId: String,
    open var exception: Exception? = null,
    open val errorCodes: List<Int>? = null
)

class GeneralError(
    override var error: String? = null,
    override val errorMessage: String? = "An unexpected error happened",
    override val correlationId: String,
    val details: List<Map<String, String>>? = null,
    override val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId, exception = exception)

class BrowserRequiredError(
    override var error: String? = null,
    override val errorMessage: String = "The client's authentication capabilities are insufficient. Please redirect to the browser to complete authentication",
    override val correlationId: String
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId)

class IncorrectCodeError(
    override var error: String? = null,
    override val errorMessage: String,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId, errorCodes = errorCodes)

class UserNotFoundError(
    override var error: String? = null,
    override val errorMessage: String,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId, errorCodes = errorCodes)

class PasswordIncorrectError(
    override var error: String? = null,
    override val errorMessage: String,
    override val correlationId: String,
    override val errorCodes: List<Int>
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId, errorCodes = errorCodes)

class UserAlreadyExistsError(
    override var error: String? = null,
    override val errorMessage: String,
    override val correlationId: String
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId)

class InvalidPasswordError(
    override var error: String? = null,
    override val errorMessage: String,
    override val correlationId: String
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId)

class InvalidEmailError(
    override var error: String? = null,
    override val errorMessage: String,
    override val correlationId: String
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId)

class InvalidAttributesError(
    override var error: String? = null,
    override val errorMessage: String,
    override val correlationId: String
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId)

class InvalidAuthenticationTypeError(
    override var error: String? = null,
    override val errorMessage: String = "This user cannot use the current authentication type",
    override val correlationId: String,
    override val errorCodes: List<Int>
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId, errorCodes = errorCodes)
