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

/**
 * AccountId implementation for Accounts retrieved from AzureActiveDirectory.
 */
public class AzureActiveDirectoryAccountId extends AccountId {

    /**
     * The object_id for the associated Account in AAD.
     */
    private String mObjectId;

    /**
     * The tenant identifier for the associated Account.
     */
    private String mTenantId;

    /**
     * Sets the objectId.
     *
     * @param objectId The objectId to set.
     */
    void setObjectId(final String objectId) {
        mObjectId = objectId;
    }

    /**
     * Gets the objectId.
     *
     * @return The objectId to get.
     */
    public String getObjectId() {
        return mObjectId;
    }

    /**
     * Sets the tenantId.
     *
     * @param tenantId The tenantId to set.
     */
    void setTenantId(final String tenantId) {
        mTenantId = tenantId;
    }

    /**
     * Gets the tenantId.
     *
     * @return The tenantId to get.
     */
    public String getTenantId() {
        return mTenantId;
    }
}
