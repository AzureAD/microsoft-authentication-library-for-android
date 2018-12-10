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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.identity.common.internal.result.IBaseAuthenticationResult;

import java.util.Date;

public interface IAuthenticationResult extends IBaseAuthenticationResult {


    /**
     * Gets the Account.
     *
     * @return The Account to get.
     */
    @NonNull
    IAccount getAccount();

    /**
     * @return The access token requested.
     */
    @NonNull
    @Override
    String getAccessToken();

    /**
     * @return The expiration time of the access token returned in the Token property.
     * This value is calculated based on current UTC time measured locally and the value expiresIn returned from the
     * service.
     */
    @NonNull
    @Override
    Date getExpiresOn();

    /**
     * @return A unique tenant identifier that was used in token acquisiton. Could be null if tenant information is not
     * returned by the service.
     */
    @Nullable
    @Override
    String getTenantId();

    /**
     * @return The unique identifier of the user.
     */
    @NonNull
    @Override
    String getUniqueId();

    /**
     * @return The id token returned by the service or null if no id token is returned.
     */
    @Nullable
    @Override
    String getIdToken();

    /**
     * @return The scopes returned from the service.
     */
    @NonNull
    @Override
    String[] getScope();
}
