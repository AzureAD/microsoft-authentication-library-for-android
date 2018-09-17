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
package com.microsoft.identity.client.internal.authorities;

import android.net.Uri;

import com.google.gson.annotations.SerializedName;

public abstract class AzureActiveDirectoryAudience {

    private String cloudUrl;
    @SerializedName("tenant_id")
    private String tenantId;

    private static final String ORGANIZATIONS = "organizations";
    private static final String CONSUMERS = "consumers";
    private static final String ALL = "common";

    public String getCloudUrl() {
        return this.cloudUrl;
    }

    public void setCloudUrl(String cloudUrl) {
        this.cloudUrl = cloudUrl;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public static AzureActiveDirectoryAudience getAzureActiveDirectoryAudience(final String cloudUrl,
                                                                               final String tenantId) {
        AzureActiveDirectoryAudience audience = null;

        switch (tenantId.toLowerCase()) {
            case ORGANIZATIONS:
                audience = new AnyOrganizationalAccount(cloudUrl);
                break;
            case CONSUMERS:
                audience = new AnyPersonalAccount();
                break;
            case ALL:
                audience = new AllAccounts();
                break;
            default:
                audience = new AccountsInOneOrganization(cloudUrl, tenantId);
        }

        return audience;
    }

}
