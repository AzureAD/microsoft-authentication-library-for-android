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

import java.util.Map;

public interface IClaimable {

    /**
     * Gets the JWT format id_token corresponding to this IClaimable. This value conforms to
     * <a href="https://tools.ietf.org/html/rfc7519">RFC-7519</a> and is further specified according
     * to <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken">OpenID Connect Core</a>.
     * <p>
     * Note: MSAL does not validate the JWT token.
     *
     * @return The raw id_token.
     */
    @Nullable
    String getIdToken();

    /**
     * Gets the claims associated with this IClaimable's IdToken.
     *
     * @return A Map of claims.
     */
    @Nullable
    Map<String, ?> getClaims();

    /**
     * Gets the preferred_username claim associated with this IClaimable.
     * <p>
     * Note: On the Microsoft B2C Identity Platform, this claim may be unavailable when external
     * identity providers are used.
     *
     * @return The preferred_username claim or "" (empty string) if not available.
     */
    @NonNull
    String getUsername();

    /**
     * Gets the tid claim associated with this IClaimable.
     *
     * @return The tid claim or "" (empty string) if not available.
     */
    @NonNull
    String getTenantId();
}
