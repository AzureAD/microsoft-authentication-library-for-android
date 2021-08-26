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

import com.microsoft.identity.common.java.cache.CacheRecord;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.internal.commands.DeviceCodeFlowCommand;
import com.microsoft.identity.common.internal.commands.DeviceCodeFlowCommandCallback;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.java.result.LocalAuthenticationResult;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Shadow class that simulates Device Code Flow successfully returning an acquire token result object.
 */
@Implements(DeviceCodeFlowCommand.class)
public class ShadowDeviceCodeFlowCommandSuccessful {

    @RealObject
    private DeviceCodeFlowCommand mDeviceCodeFlowCommand;

    @Implementation
    public AcquireTokenResult execute() {
        // 15 minutes is the default timeout.
        final Date expiryDate = new Date();
        expiryDate.setTime(expiryDate.getTime() + TimeUnit.MINUTES.toMillis(15));

        final DeviceCodeFlowCommandCallback callback = (DeviceCodeFlowCommandCallback) mDeviceCodeFlowCommand.getCallback();
        callback.onUserCodeReceived(
                "https://login.microsoftonline.com/common/oauth2/deviceauth",
                "ABCDEFGH",
                "Follow these instructions to authenticate.",
                expiryDate);

        // Create parameters for dummy authentication result
        final CacheRecord.CacheRecordBuilder recordBuilder = CacheRecord.builder();
        final AccountRecord accountRecord = new AccountRecord();
        recordBuilder.account(accountRecord);
        accountRecord.setHomeAccountId("abcd");
        accountRecord.setLocalAccountId("abcd");
        final List<ICacheRecord> cacheRecordList = new ArrayList<>();
        final ICacheRecord cacheRecord = recordBuilder.build();
        cacheRecordList.add(cacheRecord);

        // Create dummy authentication result
        final ILocalAuthenticationResult localAuthenticationResult = new LocalAuthenticationResult(
                cacheRecord,
                cacheRecordList,
                SdkType.MSAL,
                false
        );

        // Create dummy token result
        final AcquireTokenResult tokenResult = new AcquireTokenResult();
        tokenResult.setLocalAuthenticationResult(localAuthenticationResult);

        return tokenResult;
    }
}
