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

import com.microsoft.identity.common.internal.cache.CacheRecord;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.commands.DeviceCodeFlowCommand;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.internal.result.LocalAuthenticationResult;

import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.List;

@Implements(DeviceCodeFlowCommand.class)
public class ShadowDCFCommandSuccessful {
    public AcquireTokenResult execute() {
        DeviceCodeFlowCommand.dcfCallback.getUserCode(
                "https://login.microsoftonline.com/common/oauth2/deviceauth",
                "ABCDEFGH",
                "Follow these instructions to authenticate.");

        // Create parameters for dummy authentication result
        CacheRecord cRecord = new CacheRecord();
        cRecord.setAccount(new AccountRecord());
        cRecord.getAccount().setHomeAccountId("abcd");
        cRecord.getAccount().setLocalAccountId("abcd");
        List<ICacheRecord> list = new ArrayList<>();
        list.add(cRecord);

        // Create dummy authentication result
        ILocalAuthenticationResult localAuth = new LocalAuthenticationResult(
                cRecord,
                list,
                SdkType.MSAL,
                false
        );

        // Create dummy token result
        AcquireTokenResult acqResult = new AcquireTokenResult();
        acqResult.setLocalAuthenticationResult(localAuth);

        return acqResult;
    }
}
