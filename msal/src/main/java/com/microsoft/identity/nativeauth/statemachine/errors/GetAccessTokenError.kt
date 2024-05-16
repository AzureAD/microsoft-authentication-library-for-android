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

package com.microsoft.identity.nativeauth.statemachine.errors

import com.microsoft.identity.nativeauth.statemachine.results.GetAccessTokenResult

internal class GetAccessTokenErrorTypes {
    companion object {
        /*
        * The INVALID_SCOPES value indicates the scopes provided by the user are not valid
        * If this occurs, valid scopes should be resubmitted
        */
        const val INVALID_SCOPES = "invalid_scopes"

        /*
         * The NO_ACCOUNT_FOUND value indicates the user is not signed in.
         * If this occurs, the API should be called after successful sign in
         */
        const val NO_ACCOUNT_FOUND = "invalid_scopes"
    }
}

class GetAccessTokenError(
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String? = null,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
): GetAccessTokenResult, Error(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception) {
    fun isNoAccountFound() : Boolean = this.errorType == GetAccessTokenErrorTypes.NO_ACCOUNT_FOUND

    fun isInvalidScopes(): Boolean = this.errorType == GetAccessTokenErrorTypes.INVALID_SCOPES
}
