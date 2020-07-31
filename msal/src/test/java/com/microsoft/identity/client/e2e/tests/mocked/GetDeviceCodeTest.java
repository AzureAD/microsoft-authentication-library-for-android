package com.microsoft.identity.client.e2e.tests.mocked;

import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.e2e.shadows.ShadowHttpRequestForMockedTest;
import com.microsoft.identity.client.e2e.shadows.ShadowMsalUtils;
import com.microsoft.identity.client.e2e.tests.PublicClientApplicationAbstractTest;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.controllers.CommandDispatcher;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2StrategyParameters;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.SINGLE_ACCOUNT_DCF_TEST_CONFIG_FILE_PATH;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowMsalUtils.class})
public class GetDeviceCodeTest extends PublicClientApplicationAbstractTest {

    private MicrosoftStsAuthorizationRequest.Builder builder;
    private String urlBody;
    private OAuth2Strategy strategy;

    @Before
    public void setup() {
        super.setup();

        final PublicClientApplicationConfiguration config = mApplication.getConfiguration();
        urlBody = ((AzureActiveDirectoryAuthority) config.getAuthorities().get(0)).getAudience().getCloudUrl();
        builder = new MicrosoftStsAuthorizationRequest.Builder();
        builder.setClientId(config.getClientId())
                .setScope("user.read")
                .setState("State!");

        final OAuth2StrategyParameters options = new OAuth2StrategyParameters();
        strategy = new AzureActiveDirectoryOAuth2Strategy(
                new AzureActiveDirectoryOAuth2Configuration(),
                options
        );
    }

    @Override
    public String getConfigFilePath() {
        return SINGLE_ACCOUNT_DCF_TEST_CONFIG_FILE_PATH;
    }

    @Test
    public void testOne() {
        final MicrosoftStsAuthorizationRequest authorizationRequest = builder.build();
        final AuthorizationResult authorizationResult = runGetDeviceCodeThread(authorizationRequest, urlBody);
        final MicrosoftStsAuthorizationResponse authorizationResponse = (MicrosoftStsAuthorizationResponse) authorizationResult.getAuthorizationResponse();

        Assert.assertTrue(authorizationResult.getSuccess());
        Assert.assertNotNull(authorizationResponse);
        Assert.assertNotNull(authorizationResponse.getDeviceCode());
        Assert.assertNull(authorizationResult.getAuthorizationErrorResponse());
    }

    private AuthorizationResult runGetDeviceCodeThread(final MicrosoftStsAuthorizationRequest authorizationRequest, final String urlBody) {
        GetDeviceCodeRunner runner = new GetDeviceCodeRunner(authorizationRequest, urlBody);

        Thread thread = new Thread(runner);
        thread.start();

        try {
            thread.join();
        }
        catch (InterruptedException e){
            Assert.fail();
        }

        return runner.getResult();
    }

    /**
     *
     */
    private class GetDeviceCodeRunner implements Runnable {
        private MicrosoftStsAuthorizationRequest mAuthorizationRequest;
        private String mUrlBody;
        private AuthorizationResult mAuthorizationResult;

        public GetDeviceCodeRunner(final MicrosoftStsAuthorizationRequest authorizationRequest, final String urlBody){
            this.mAuthorizationRequest = authorizationRequest;
            this.mUrlBody = urlBody;
        }

        @Override
        public void run() {
            try {
                mAuthorizationResult = strategy.getDeviceCode(mAuthorizationRequest, mUrlBody);
            }
            catch (IOException e){
                Assert.fail();
            }
        }

        public AuthorizationResult getResult() {
            return mAuthorizationResult;
        }
    }
}
