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
package com.microsoft.identity.client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MultiTenantAccount extends Account implements IMultiTenantAccount {

    private Map<String, ITenantProfile> mTenantProfiles = new HashMap<>();

    MultiTenantAccount(
            @Nullable final String clientInfo,
            @Nullable final IDToken homeTenantIdToken) {
        super(clientInfo, homeTenantIdToken);
    }

    void setTenantProfiles(@NonNull final Map<String, ITenantProfile> profiles) {
        mTenantProfiles = profiles;
    }

    @NonNull
    @Override
    public Map<String, ITenantProfile> getTenantProfiles() {
        return Collections.unmodifiableMap(mTenantProfiles);
    }
}
