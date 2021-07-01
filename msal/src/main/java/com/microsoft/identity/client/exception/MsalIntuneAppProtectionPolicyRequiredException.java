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

package com.microsoft.identity.client.exception;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.exception.IntuneAppProtectionPolicyRequiredException;

public class MsalIntuneAppProtectionPolicyRequiredException extends MsalServiceException {

    private String mAccountUpn;
    private String mAccountUserId;
    private String mTenantId;
    private String mAuthorityUrl;


    public MsalIntuneAppProtectionPolicyRequiredException(
            @NonNull final IntuneAppProtectionPolicyRequiredException exception){

        super(exception.getErrorCode(), exception.getMessage(), exception);

        mAccountUpn = exception.getAccountUpn();
        mAccountUserId = exception.getAccountUserId();
        mAuthorityUrl = exception.getAuthorityUrl();
        mTenantId = exception.getTenantId();
    }

    /**
     * Account Upn of the user
     * @return String
     */
    public String getAccountUpn() {
        return mAccountUpn;
    }

    /**
     * Account OID of the user
     * @return String
     */
    public String getAccountUserId() {
        return mAccountUserId;
    }

    /**
     * Account Tenant id
     * @return String
     */
    public String getTenantId() {
        return mTenantId;
    }

    /**
     * Authority Url
     * @return String
     */
    public String getAuthorityUrl() {
        return mAuthorityUrl;
    }

}
