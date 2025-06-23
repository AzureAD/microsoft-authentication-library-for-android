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

package com.microsoft.identity.nativeauth.parameters

import com.microsoft.identity.nativeauth.statemachine.states.RegisterStrongAuthVerificationRequiredState

class NativeAuthRegisterStrongAuthVerificationRequiredResultParameter internal constructor(
    internal val nextState: RegisterStrongAuthVerificationRequiredState,
    internal val codeLength: Int,
    internal val sentTo: String,
    internal val channel: String
) {

    /**
     * The next state to use to continue the strong authentication method registration flow.
     */
    fun getNextState(): RegisterStrongAuthVerificationRequiredState {
        return nextState
    }

    /**
     * The length of the challenge required by the server.
     */
    fun getCodeLength(): Int {
        return codeLength
    }

    /**
     * The email/phone number the challenge was sent to.
     */
    fun getSentTo(): String {
        return sentTo
    }

    /**
     * the channel(email/phone) the challenge was sent through.
     */
    fun getChannel(): String {
        return channel
    }
}