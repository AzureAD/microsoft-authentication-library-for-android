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

import com.microsoft.identity.common.java.cache.CacheRecord;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.internal.testutils.mocks.MockTokenCreator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
public class AuthenticationResultTest {

    private List<ICacheRecord> mCacheRecords;

    // setup mock data
    @Before
    public void setup() {
        final List<ICacheRecord> cacheRecords = new ArrayList<>();

        final AccountRecord accountRecord = new AccountRecord();
        accountRecord.setHomeAccountId("");
        accountRecord.setLocalAccountId("");

        final IdTokenRecord idTokenRecord = new IdTokenRecord();
        idTokenRecord.setSecret(MockTokenCreator.createMockIdToken());

        final CacheRecord.CacheRecordBuilder cacheRecord = CacheRecord.builder();
        cacheRecord.account(accountRecord);
        cacheRecord.idToken(idTokenRecord);

        cacheRecords.add(cacheRecord.build());

        mCacheRecords = cacheRecords;
    }

    @Test
    public void testAuthenticationResultHasCorrelationIdIfValidCorrelationIdWasProvided() {
        final UUID correlationId = UUID.randomUUID();

        AuthenticationResult authenticationResult = new AuthenticationResult(
                mCacheRecords,
                correlationId.toString()
        );

        Assert.assertNotNull(authenticationResult);
        Assert.assertNotNull(authenticationResult.getCorrelationId());
        Assert.assertEquals(correlationId, authenticationResult.getCorrelationId());
    }

    @Test
    public void testAuthenticationResultHasNullCorrelationIdIfNullProvided() {
        AuthenticationResult authenticationResult = new AuthenticationResult(
                mCacheRecords,
                null
        );

        Assert.assertNotNull(authenticationResult);
        Assert.assertNull(authenticationResult.getCorrelationId());
    }

    @Test
    public void testAuthenticationResultHasNullCorrelationIdIfEmptyStringProvided() {
        AuthenticationResult authenticationResult = new AuthenticationResult(
                mCacheRecords,
                ""
        );

        Assert.assertNotNull(authenticationResult);
        Assert.assertNull(authenticationResult.getCorrelationId());
    }

    @Test
    public void testAuthenticationResultHasNullCorrelationIdIfNotValidUUID() {
        final String invalidCorrelationId = "garbage";
        AuthenticationResult authenticationResult = new AuthenticationResult(
                mCacheRecords,
                invalidCorrelationId
        );

        Assert.assertNotNull(authenticationResult);
        Assert.assertNull(authenticationResult.getCorrelationId());
    }

}
