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
 * MSAL internal representation for token cache. MS first party apps can use the internal
 * {@link TokenCache#serialize(User)} and {@link TokenCache#deserialize(String)} to import and export family tokens
 * to implement SSO. To prevent confusions among external apps, we don't expose these two methods.
 */
class TokenCache {
    /**
     * Internal API for the SDK to serialize the family token cache item for the given user.
     *
     * The sdk will look up family token cache item with the given user id, and serialize the token cache item and
     * return it as a serialized blob.
     * @param user
     * @return
     */
    String serialize(final User user) {
        return "";
    }

    /**
     * Internal API for the sdk to take in the serialized blob and save it into the cache.
     *
     * The sdk will deserialize the input blob into the token cache item and save it into cache.
     * @param serializedBlob
     */
    void deserialize(final String serializedBlob) {

    }
}