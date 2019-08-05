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

import androidx.test.runner.AndroidJUnit4;

import com.microsoft.identity.client.internal.telemetry.CacheEvent;
import com.microsoft.identity.client.internal.telemetry.EventConstants;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CacheEventTest {

    static final String TEST_TOKEN_TYPE = "bearer";

    static CacheEvent.Builder getTestCacheEventBuilder(final String eventName, final String tokenType) {
        return new CacheEvent.Builder(eventName)
                .setTokenType(tokenType)
                .setIsAT(true);
    }

    static CacheEvent getTestCacheEvent(
            final String eventName,
            final String tokenType
    ) {
        return getTestCacheEventBuilder(eventName, tokenType).build();
    }

    @Test
    public void testCacheEventInitializes() {
        final String eventName = EventConstants.EventName.TOKEN_CACHE_DELETE;
        final CacheEvent cacheEvent = getTestCacheEvent(eventName, TEST_TOKEN_TYPE);
        Assert.assertEquals(eventName, cacheEvent.getEventName());
        Assert.assertEquals(TEST_TOKEN_TYPE, cacheEvent.getTokenType());
        Assert.assertEquals(Boolean.TRUE, Boolean.valueOf(cacheEvent.isAT()));
    }
}
