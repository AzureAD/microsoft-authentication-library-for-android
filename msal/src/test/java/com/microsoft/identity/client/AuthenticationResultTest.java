package com.microsoft.identity.client;

import com.microsoft.identity.common.internal.cache.CacheRecord;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
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

        final CacheRecord cacheRecord = new CacheRecord();
        cacheRecord.setAccount(accountRecord);
        cacheRecord.setIdToken(idTokenRecord);

        cacheRecords.add(cacheRecord);

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
