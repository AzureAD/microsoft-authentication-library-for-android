package com.microsoft.identity.client;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.microsoft.identity.client.internal.controllers.OperationParametersAdapter;
import com.microsoft.identity.common.internal.cache.IAccountCredentialAdapter;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.msal.R;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class OperationParametersAndroidTest {

    private Context mAppContext;
    private Activity mActivity;

    @Before
    public void setUp() {
        System.setProperty(
                "dexmaker.dexcache",
                androidx.test.platform.app.InstrumentationRegistry
                        .getInstrumentation()
                        .getTargetContext()
                        .getCacheDir()
                        .getPath()
        );

        System.setProperty(
                "org.mockito.android.target",
                ApplicationProvider
                        .getApplicationContext()
                        .getCacheDir()
                        .getPath()
        );

        mAppContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext().getApplicationContext();

    }

    @Test
    public void testAcquireTokenOperationParameters() {

        AcquireTokenOperationParameters operationParameters = OperationParametersAdapter.createAcquireTokenOperationParameters(getAcquireTokenParameters(), getConfiguration(), getCache());

        Assert.assertEquals(true, operationParameters.getForceRefresh());

    }

    private ClaimsRequest getAccessTokenClaimsRequest(@NonNull String claimName, @NonNull String claimValue){
        ClaimsRequest cp1ClaimsRequest = new ClaimsRequest();
        RequestedClaimAdditionalInformation info = new RequestedClaimAdditionalInformation();
        info.setValues(new ArrayList<Object>(Arrays.asList(claimValue)));
        cp1ClaimsRequest.requestClaimInAccessToken(claimName, info);
        return cp1ClaimsRequest;
    }

    private AcquireTokenParameters getAcquireTokenParameters(){
        AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .withClaims(getAccessTokenClaimsRequest("device_id", ""))
                .withScopes(new ArrayList<String>(Arrays.asList("User.Read")))
                .build();

        return parameters;
    }

    private PublicClientApplicationConfiguration getConfiguration(){
        PublicClientApplicationConfiguration config = new PublicClientApplicationConfiguration();
        config.mClientCapabilities = "CP1";
        PublicClientApplicationConfiguration defaultConfig = PublicClientApplicationConfigurationFactory.loadConfiguration(mAppContext, R.raw.msal_default_config);

        defaultConfig.mergeConfiguration(config);
        return defaultConfig;
    }

    private OAuth2TokenCache getCache(){
        return new TestOAuthTokenCachen(mAppContext, null, null);
    }

    private class TestOAuthTokenCachen extends MsalOAuth2TokenCache {

        /**
         * Constructor of MsalOAuth2TokenCache.
         *
         * @param context                  Context
         * @param accountCredentialCache   IAccountCredentialCache
         * @param accountCredentialAdapter IAccountCredentialAdapter
         */
        public TestOAuthTokenCachen(Context context, IAccountCredentialCache accountCredentialCache, IAccountCredentialAdapter accountCredentialAdapter) {
            super(context, accountCredentialCache, accountCredentialAdapter);
        }
    }
}
