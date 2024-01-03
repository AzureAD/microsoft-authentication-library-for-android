package com.microsoft.identity.client.msal.automationapp.testpass.ciam

import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.msal.automationapp.R
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout
import com.microsoft.identity.client.ui.automation.app.IApp
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters
import org.junit.Assert
import org.junit.Test
import java.util.Arrays

class CIAMTest : AbstractCIAMTest() {
    override fun getConfigFileResourceId() = R.raw.msal_config_ciam

    @Test
    @Throws(Throwable::class)
    fun testSimpleCiamScenario() {
        val username = mLabAccount.username
        val password = mLabAccount.password

        val msalSdk = MsalSdk()

        val authTestParams = MsalAuthTestParams.builder()
            .activity(mActivity)
            .loginHint(username)
            .scopes(Arrays.asList(*mScopes))
            .promptParameter(Prompt.SELECT_ACCOUNT)
            .msalConfigResourceId(configFileResourceId)
            .build()

        val authResult = msalSdk.acquireTokenInteractive(
            authTestParams,
            {
                (mBrowser as IApp).handleFirstRun()
                val promptHandlerParameters = MicrosoftStsPromptHandlerParameters.builder()
                    .prompt(PromptParameter.SELECT_ACCOUNT)

                    .loginHint(username)
                    .broker(null)
                    .build()

                MicrosoftStsPromptHandler(promptHandlerParameters)
                    .handlePrompt(username, password)
            },
            TokenRequestTimeout.LONG)
        authResult.assertSuccess()

        // Verify the CIAM issuer
        val issuerClaim = authResult.claims.get("iss") as String
        Assert.assertTrue(issuerClaim.contains("ciamlogin.com"))

        // ------ do silent request ------
        val authTestSilentParams = MsalAuthTestParams.builder()
            .activity(mActivity)
            .authority(authority)
            .loginHint(username)
            .forceRefresh(false)
            .scopes(Arrays.asList(*mScopes))
            .msalConfigResourceId(configFileResourceId)
            .build()
        val authSilentResult =
            msalSdk.acquireTokenSilent(authTestSilentParams, TokenRequestTimeout.SILENT)
        authSilentResult.assertSuccess()

        // ------ do force refresh silent request ------
        val silentForceParams = MsalAuthTestParams.builder()
            .activity(mActivity)
            .authority(authority)
            .loginHint(username)
            .forceRefresh(true)
            .scopes(Arrays.asList(*mScopes))
            .msalConfigResourceId(configFileResourceId)
            .build()
        val authSilentForceResult =
            msalSdk.acquireTokenSilent(silentForceParams, TokenRequestTimeout.SILENT)

        authSilentForceResult.assertSuccess()
    }
}