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

import java.util.Date;
import java.util.UUID;

public interface IAuthenticationResult {

    /**
     * @return The access token requested.
     */
    @NonNull
    String getAccessToken();

    /**
     * Gets the fully-formed Authorization header value. Includes the Authentication scheme.
     * <p>
     * Example: Bearer eyJ1aWQiOiJj.......
     *
     * @return The Authorization header value.
     */
    @NonNull
    String getAuthorizationHeader();

    /**
     * Gets the authentication scheme (Bearer, PoP, etc)....
     *
     * @return The authentication scheme name.
     */
    @NonNull
    String getAuthenticationScheme();

    /**
     * @return The expiration time of the access token returned in the Token property.
     * This value is calculated based on current UTC time measured locally and the value expiresIn returned from the
     * service. Please note that if the authentication scheme is 'pop', this value reflects the expiry of the
     * 'inner' token returned by AAD and does not indicate the expiry of the signed pop JWT ('outer' token).
     */
    @NonNull
    Date getExpiresOn();

    /**
     * @return A unique tenant identifier that was used in token acquisition. Could be null if tenant information is not
     * returned by the service.
     */
    @Nullable
    String getTenantId();

    /**
     * Gets the Account.
     *
     * @return The Account to get.
     */
    @NonNull
    IAccount getAccount();

    /**
     * @return The scopes returned from the service.
     */
    @NonNull
    String[] getScope();

    /**
     * Gets the correlation id used during the acquire token request. Could be null if an error
     * occurs when parsing from String or if not set.
     *
     * @return a UUID representing a correlation id
     */
    @Nullable
    UUID getCorrelationId(); // this should never actually be null for MSAL
}
