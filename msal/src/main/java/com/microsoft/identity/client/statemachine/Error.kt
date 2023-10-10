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

/**
 * GeneralError is a base class for all errors present in the Native Auth.
 */
class GeneralError(
    override var error: String? = null,
    override val errorMessage: String? = "An unexpected error happened",
    override val correlationId: String,
    val details: List<Map<String, String>>? = null,
    override val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId, exception = exception)

/**
 * BrowserRequiredError occurs when authentication cannot be performed via means of Native Auth and
 * a redirect via browser is required.
 */
class BrowserRequiredError(
    override var error: String? = null,
    override val errorMessage: String = "The client's authentication capabilities are insufficient. Please redirect to the browser to complete authentication",
    override val correlationId: String
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId)

/**
 * IncorrectCodeError occurs when the user has provided incorrect code for out of band authentication.
 */
class IncorrectCodeError(
    override var error: String? = null,
    override val errorMessage: String,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId, errorCodes = errorCodes)

/**
 * UserNotFoundError occurs when the user could not be located in the given the username. The authentication
 * using signin and password reset APIs will throw this error.
 */
class UserNotFoundError(
    override var error: String? = null,
    override val errorMessage: String,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId, errorCodes = errorCodes)

/**
 * PasswordIncorrectError occurs when the user has provided incorrect password for signin.
 */
class PasswordIncorrectError(
    override var error: String? = null,
    override val errorMessage: String,
    override val correlationId: String,
    override val errorCodes: List<Int>
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId, errorCodes = errorCodes)

/**
 * UserAlreadyExistsError has used a username to create an exists for which there is a pre-existing
 * account.
 */
class UserAlreadyExistsError(
    override var error: String? = null,
    override val errorMessage: String,
    override val correlationId: String
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId)

/**
 * InvalidPasswordError is seen in Signup process when a user provides a password that does not
 * match policies set by the server.
 */
class InvalidPasswordError(
    override var error: String? = null,
    override val errorMessage: String,
    override val correlationId: String
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId)

/**
 * InvalidPasswordError is seen in Signup process when a user provides a email address that is not
 * acceptable by the server.
 */
class InvalidEmailError(
    override var error: String? = null,
    override val errorMessage: String,
    override val correlationId: String
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId)

/**
 * InvalidAttributesError is seen in Signup process when a user provides attributes that are not
 * acceptable by the server.
 */
class InvalidAttributesError(
    override var error: String? = null,
    override val errorMessage: String,
    override val correlationId: String
) : Error(errorMessage = errorMessage, error = error, correlationId = correlationId)
