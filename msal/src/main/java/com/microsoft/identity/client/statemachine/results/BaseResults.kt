// ktlint-disable filename

// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.statemachine.results

import com.microsoft.identity.client.statemachine.Error
import com.microsoft.identity.client.statemachine.states.State

/**
 * Result is the base class for all Result classes used in Native Auth.
 */
interface Result {
    /**
     * SuccessResult which indicates the API call succeeded.
     */
    open class SuccessResult(open val nextState: State) : Result

    /**
     * ErrorResult, which indicates that the flow failed.
     */
    open class ErrorResult(open val error: Error) : Result

    /**
     * Complete Result, which indicates the flow is complete.
     */
    open class CompleteResult(open val resultValue: Any? = null) : Result

    /**
     * CompleteWithNextStateResult which indicates the flow is complete but the next flow
     * should be started e.g. SignIn flow is started after Signup is complete.
     */
    open class CompleteWithNextStateResult(override val resultValue: Any? = null, open val nextState: State?) : CompleteResult(resultValue = resultValue)

    /**
     * Returns true if the current API call succeeded
     */
    fun isSuccess(): Boolean = this is SuccessResult

    /**
     * Returns true if the API call failed
     */
    fun isError(): Boolean = this is ErrorResult

    /**
     * Returns true if the flow is complete
     */
    fun isComplete(): Boolean = this is CompleteResult
}

/**
 * Sign out: removes account from cache. Does not perform single sign-out.
 */
interface SignOutResult : Result {
    /**
     * CompleteResult Result, which indicates the sign out flow completed successfully.
     * i.e. the user account has been removed from persistence.
     */
    object Complete :
        Result.CompleteResult(resultValue = null),
        SignOutResult

    /**
     * UnexpectedError ErrorResult, which indicates that an unexpected error occurred during sign out.
     *
     * @param error [com.microsoft.identity.client.statemachine.Error]
     */
    class UnexpectedError(override val error: Error) :
        Result.ErrorResult(error = error),
        SignOutResult
}
