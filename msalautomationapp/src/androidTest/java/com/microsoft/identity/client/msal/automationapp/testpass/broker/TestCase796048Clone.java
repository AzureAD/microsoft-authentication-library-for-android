package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.sdk.AuthResult;
import com.microsoft.identity.client.ui.automation.sdk.AuthTestParams;
import com.microsoft.identity.client.ui.automation.sdk.MsalSdk;
import com.microsoft.identity.client.ui.automation.sdk.SdkPromptParameter;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Test;

public class TestCase796048Clone extends AbstractMsalBrokerTest {

    @Test
    public void test_796048() {
        final String username = mLoginHint;
        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

        final MsalSdk msalSdk = new MsalSdk();

        final AuthTestParams authTestParams = AuthTestParams.builder()
                .activity(mActivity)
                .loginHint(mLoginHint)
                .resource(mScopes[0])
                .msalConfigResourceId(getConfigFileResourceId())
                .promptParameter(SdkPromptParameter.SELECT_ACCOUNT)
                .build();

        final AuthResult authResult = msalSdk.acquireTokenInteractive(authTestParams, new OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .loginHint(mLoginHint)
                        .sessionExpected(false)
                        .consentPageExpected(false)
                        .speedBumpExpected(false)
                        .broker(mBroker)
                        .expectingBrokerAccountChooserActivity(false)
                        .build();

                new AadPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);
            }
        });

        authResult.assertSuccess();

        // now expire AT

        TestContext.getTestContext().getTestDevice().getSettings().forwardDeviceTimeForOneDay();

        // SILENT REQUEST

        final AuthTestParams authTestSilentParams = AuthTestParams.builder()
                .activity(mActivity)
                .loginHint(mLoginHint)
                .resource(mScopes[0])
                .authority(getAuthority())
                .forceRefresh(true)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final AuthResult silentAuthResult = msalSdk.acquireTokenSilent(authTestSilentParams);

        silentAuthResult.assertSuccess();
    }


    @Override
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_GERMANY_CLOUD;
        return query;
    }

    @Override
    public String getTempUserType() {
        return null;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"00000002-0000-0000-c000-000000000000"};
    }

    @Override
    public String getAuthority() {
        return "https://login.microsoftonline.de/common";
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_instance_aware_common;
    }

}
