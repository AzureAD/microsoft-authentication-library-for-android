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
package com.microsoft.identity.client.e2e.shadows;

import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.internal.commands.DeviceCodeFlowCommand;
import com.microsoft.identity.common.internal.commands.DeviceCodeFlowCommandCallback;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;

import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Shadow class that simulates Device Code Flow failing due to an error in the token polling phase.
 */
@Implements(DeviceCodeFlowCommand.class)
public class ShadowDeviceCodeFlowCommandTokenError {

    @RealObject
    private DeviceCodeFlowCommand mDeviceCodeFlowCommand;

    public AcquireTokenResult execute() throws Exception {
        final DeviceCodeFlowCommandCallback callback = (DeviceCodeFlowCommandCallback) mDeviceCodeFlowCommand.getCallback();

        // 15 minutes is the default timeout.
        final Date expiryDate = new Date();
        expiryDate.setTime(expiryDate.getTime() + TimeUnit.MINUTES.toMillis(15));

        callback.onUserCodeReceived(
                "https://login.microsoftonline.com/common/oauth2/deviceauth",
                "ABCDEFGH",
                "Follow these instructions to authenticate.",
                expiryDate);

        throw new ServiceException(ErrorStrings.DEVICE_CODE_FLOW_EXPIRED_TOKEN_ERROR_CODE, "The device_code expired. No need to continue polling for the token (expired token).", null);
    }
}
