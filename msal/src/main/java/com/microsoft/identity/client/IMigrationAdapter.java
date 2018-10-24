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

import android.util.Pair;

import com.microsoft.identity.common.BaseAccount;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;

import java.util.List;
import java.util.Map;

/**
 * Describes an object which adapts a Map of credentials (keys/values) to a List of Account/RT Pairs.
 *
 * @param <T> The account type.
 * @param <U> The refresh token type.
 */
public interface IMigrationAdapter<T extends BaseAccount, U extends RefreshToken> {

    /**
     * Adapts a Map of credentials (keys/values) to a List of Account/RT Pairs.
     *
     * @param cacheItems The cache items to adapt.
     * @return The adapter cache items in the format specified by T/U generic types. Paired as
     * Account/RefreshToken.
     */
    List<Pair<T, U>> adapt(Map<String, String> cacheItems);
}
