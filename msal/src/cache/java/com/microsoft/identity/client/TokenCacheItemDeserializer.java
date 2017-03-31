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

    @Override
    public T deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        T deserializedTokenCacheItem = new Gson().fromJson(json, type);

        final User user;
        if (deserializedTokenCacheItem instanceof AccessTokenCacheItem) {
            final AccessTokenCacheItem accessTokenCacheItem = (AccessTokenCacheItem) deserializedTokenCacheItem;
            try {
                final ClientInfo clientInfo = MSALUtils.isEmpty(accessTokenCacheItem.getRawClientInfo()) ? null
                        : new ClientInfo(accessTokenCacheItem.getRawClientInfo());
                user = User.create(new IdToken(accessTokenCacheItem.getRawIdToken()), clientInfo);
            } catch (MsalClientException e) {
                throw new JsonParseException("Fail to deserialize", e);
            }
        } else {
            final RefreshTokenCacheItem refreshTokenCacheItem = (RefreshTokenCacheItem) deserializedTokenCacheItem;
            user = new User(refreshTokenCacheItem.getDisplayableId(), refreshTokenCacheItem.getName(),
                    refreshTokenCacheItem.getIdentityProvider(), refreshTokenCacheItem.getUid(), refreshTokenCacheItem.getUtid());
        }

        deserializedTokenCacheItem.setUser(user);
        return deserializedTokenCacheItem;
    }
}
