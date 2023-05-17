package com.microsoft.identity.client.msal.automationapp.testpass.msalonly.update

import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.msal.automationapp.R
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerUpdateTest
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalCustomBrokerInstallationTest
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal
import com.microsoft.identity.client.ui.automation.constants.AuthScheme
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler
import com.microsoft.identity.labapi.utilities.client.LabQuery
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import org.junit.Test
import java.util.*

//@SupportedBrokers(brokers = [BrokerCompanyPortal::class])
@RetryOnFailure
class TestCaseUpdateCompanyPortal : AbstractMsalCustomBrokerInstallationTest() {

    @Test
    @Throws(Throwable::class)
    fun test_UpdateAuthenticator() {
        val username = mLabAccount.username
        val password = mLabAccount.password

        val brokerCompanyPortal = BrokerCompanyPortal()
        brokerCompanyPortal.install()

        val msalSdk = MsalSdk()
        val authTestParams = MsalAuthTestParams.builder()
            .activity(mActivity)
            .loginHint(username)
            .scopes(Arrays.asList(*mScopes))
            .promptParameter(Prompt.LOGIN)
            .authScheme(AuthScheme.POP)
            .msalConfigResourceId(configFileResourceId)
            .build()

        val authResult = msalSdk.acquireTokenInteractive(authTestParams, {
            val promptHandlerParameters = PromptHandlerParameters.builder()
                .prompt(PromptParameter.LOGIN)
                .loginHint(username)
                .sessionExpected(false)
                .consentPageExpected(false)
                .speedBumpExpected(false)
                .broker(null)
                .expectingBrokerAccountChooserActivity(false)
                .build()
            AadPromptHandler(promptHandlerParameters)
                .handlePrompt(username, password)
        }, TokenRequestTimeout.MEDIUM)

        // Check if auth result is success
        authResult.assertSuccess()
        MsalAuthResult.verifyATForPop(authResult.accessToken)

        // Update the authenticator app
        brokerCompanyPortal.update()
        // start silent token request in MSAL

        // start silent token request in MSAL
        val authTestSilentParams = MsalAuthTestParams.builder()
            .activity(mActivity)
            .loginHint(username)
            .scopes(Arrays.asList(*mScopes))
            .authority(authority)
            .authScheme(AuthScheme.POP)
            .msalConfigResourceId(configFileResourceId)
            .build()

        val authResultPostUpdate : MsalAuthResult = msalSdk.acquireTokenSilent(authTestSilentParams, TokenRequestTimeout.SILENT)
        authResultPostUpdate.assertSuccess()
        MsalAuthResult.verifyATForPop(authResult.accessToken)
    }



    override fun getLabQuery(): LabQuery {
        return LabQuery.builder()
            .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
            .build()
    }

    override fun getTempUserType(): TempUserType? {
        return null
    }

    override fun getScopes(): Array<String>? {
        return arrayOf("User.read")
    }

    override fun getAuthority(): String? {
        return mApplication.configuration.defaultAuthority.authorityURL.toString()
    }

    override fun getConfigFileResourceId(): Int {
        return R.raw.msal_config_default
    }

}