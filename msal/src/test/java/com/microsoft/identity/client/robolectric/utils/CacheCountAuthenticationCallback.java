package com.microsoft.identity.client.robolectric.utils;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.internal.controllers.CommandDispatcher;
import com.microsoft.identity.common.internal.util.StringUtil;

import org.junit.Assert;

import static junit.framework.Assert.fail;

public class CacheCountAuthenticationCallback implements AuthenticationCallback {

    private int mExpectedCount;
    public CacheCountAuthenticationCallback(int expectedCount){
        mExpectedCount = expectedCount;
    }

    @Override
    public void onCancel() {
        fail("Cancel unexpected on silent requests.");
    }

    @Override
    public void onSuccess(IAuthenticationResult authenticationResult) {
        Assert.assertTrue(!StringUtil.isEmpty(authenticationResult.getAccessToken()));
        Assert.assertEquals(mExpectedCount, CommandDispatcher.getCachedResultCount());
    }

    @Override
    public void onError(MsalException exception) {
        fail(exception.getMessage());
    }
}
