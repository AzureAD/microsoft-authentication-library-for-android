//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.
//
//   Permission is hereby granted, free of charge, to any person obtaining a copy
//   of this software and associated documentation files(the "Software"), to deal
//   in the Software without restriction, including without limitation the rights
//   to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//   copies of the Software, and to permit persons to whom the Software is
//   furnished to do so, subject to the following conditions :
//
//   The above copyright notice and this permission notice shall be included in
//   all copies or substantial portions of the Software.
//
//   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//   THE SOFTWARE.

package com.microsoft.identity.client;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * MSAL internal Deserializer class to backfill data that are not serialized.
 */

final class TokenCacheItemDeserializer<T extends BaseTokenCacheItem> implements JsonDeserializer<T> {
    private static final String TAG = TokenCacheItemDeserializer.class.getSimpleName();

    @Override
    public T deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        T deserializedTokenCacheItem = new Gson().fromJson(json, type);
        final String rawIdToken = deserializedTokenCacheItem.getRawIdToken();
        if (!MsalUtils.isEmpty(rawIdToken)) {
            try {
                final IdToken idToken = new IdToken(rawIdToken);;
                deserializedTokenCacheItem.setIdToken(idToken);
                deserializedTokenCacheItem.setUser(new User(idToken));
            } catch (final MsalClientException e) {
                Logger.error(TAG, null, "Fail to parse Id token", e);
            }
        }

        return deserializedTokenCacheItem;
    }
}
