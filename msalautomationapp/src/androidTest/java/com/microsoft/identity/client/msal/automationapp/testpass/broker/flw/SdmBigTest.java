package com.microsoft.identity.client.msal.automationapp.testpass.broker.flw;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.MultipleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SignInParameters;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.SingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.ErrorCodes;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.app.OutlookApp;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.util.ThreadUtils;
import com.microsoft.identity.labapi.utilities.client.ILabAccount;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.ProtectionPolicy;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserRole;
import com.microsoft.identity.labapi.utilities.constants.UserType;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

// TODO: Map this to an ADO Test Case Item
public class SdmBigTest extends AbstractMsalBrokerTest {

    final String TAG = SdmBigTest.class.getSimpleName();

    @Test
    public void test_BIG_SDM_TEST() throws MsalException, InterruptedException, LabApiException {

        // 833514
        // Basic SDM SSO flow
        final String deviceAdminUsername = mLabAccount.getUsername();
        final String deviceAdminPassword = mLabAccount.getPassword();

        // pca should be in MULTIPLE account mode starting out
        Assert.assertTrue(mApplication instanceof MultipleAccountPublicClientApplication);

        //we should NOT be in shared device mode
        Assert.assertFalse(mApplication.isSharedDevice());

        // perform shared device registration
        mBroker.performSharedDeviceRegistration(
                deviceAdminUsername, deviceAdminPassword
        );

        // re-create PCA after device registration
        mApplication = PublicClientApplication.create(mContext, getConfigFileResourceId());

        // pca should now be in SINGLE account mode
        Assert.assertTrue(mApplication instanceof SingleAccountPublicClientApplication);

        // we should be in shared device mode
        Assert.assertTrue(mApplication.isSharedDevice());

        // query to load a user from the same tenant that was used for WPJ
        final LabQuery query = LabQuery.builder()
                .userType(UserType.CLOUD)
                .build();

        // get username and password for this account
        final ILabAccount BasicCloudAccount = mLabClient.getLabAccount(query);
        final String basicAccountUsername = BasicCloudAccount.getUsername();
        final String basicSccountPassword = BasicCloudAccount.getPassword();

        // use azure sample app and make sure we do a fresh install
        final AzureSampleApp azureSampleApp = new AzureSampleApp();
        azureSampleApp.uninstall();

        azureSampleApp.install();
        azureSampleApp.launch();
        azureSampleApp.handleFirstRun();

        final MicrosoftStsPromptHandlerParameters microsoftStsPromptHandlerParameters =
                MicrosoftStsPromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .broker(mBroker)
                        .loginHint(null)
                        .consentPageExpected(false)
                        .speedBumpExpected(false)
                        .enrollPageExpected(false)
                        .registerPageExpected(false)
                        .expectingBrokerAccountChooserActivity(false)
                        .expectingLoginPageAccountPicker(false)
                        .isFederated(false)
                        .sessionExpected(false)
                        .build();

        // Give the sample a bit of time to replicate being in shared device mode
        Thread.sleep(TimeUnit.SECONDS.toMillis(5));

        // sign in into Azure Sample App
        azureSampleApp.signInWithSingleAccountFragment(basicAccountUsername, basicSccountPassword, getBrowser(), false, microsoftStsPromptHandlerParameters);

        Thread.sleep(TimeUnit.SECONDS.toMillis(5));

        // make sure we have successfully signed in
        azureSampleApp.confirmSignedIn(basicAccountUsername);

        final SingleAccountPublicClientApplication singleAccountPCA =
                (SingleAccountPublicClientApplication) mApplication;

        final TokenRequestLatch getAccountLatch = new TokenRequestLatch(1);

        final IAccount[] accounts = new IAccount[1];

        Thread.sleep(TimeUnit.SECONDS.toMillis(5));

        // perform get account from MSAL Automation App
        ((SingleAccountPublicClientApplication) mApplication).getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount activeAccount) {
                assert activeAccount != null;
                Assert.assertEquals(activeAccount.getUsername(), basicAccountUsername);
                accounts[0] = activeAccount;
                getAccountLatch.countDown();
            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
                assert currentAccount != null;
                Assert.assertEquals(currentAccount.getUsername(), basicAccountUsername);
                accounts[0] = currentAccount;
                getAccountLatch.countDown();
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                Assert.fail(exception.getMessage());
                getAccountLatch.countDown();
            }
        });

        getAccountLatch.await(TokenRequestTimeout.SILENT);

        Thread.sleep(TimeUnit.SECONDS.toMillis(5));

        final TokenRequestLatch silentLatch = new TokenRequestLatch(1);

        // perform acquire token silent with account used for get account
        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .fromAuthority(getAuthority())
                .forAccount(accounts[0])
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulSilentCallback(silentLatch))
                .build();

        singleAccountPCA.acquireTokenSilentAsync(silentParameters);

        silentLatch.await(TokenRequestTimeout.SILENT);

        // 833513
        // try sign in with an account from a different tenant

        final LabQuery usGovQuery = LabQuery.builder()
                .azureEnvironment(AzureEnvironment.AZURE_US_GOVERNMENT)
                .build();

        final ILabAccount usGovAccount = mLabClient.getLabAccount(usGovQuery);
        final String usGovAccountUsername = usGovAccount.getUsername();
        final String usGovAccountPassword = usGovAccount.getPassword();

        // expect failure result from Interactive call
        final TokenRequestLatch tokenRequestLatch = new TokenRequestLatch(1);
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(usGovAccountUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(failureInteractiveCallback(tokenRequestLatch, ClientException.BRT_TENANT_MISMATCH))
                .build();
        singleAccountPCA.signIn(signInParameters);

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .loginHint(usGovAccountUsername)
                .broker(mBroker)
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .build();

        final AadPromptHandler promptHandler = new AadPromptHandler(promptHandlerParameters);
        promptHandler.handlePrompt(usGovAccountUsername, usGovAccountPassword);

        tokenRequestLatch.await();

        // 833516
        // try sign in with an account from the same tenant
        // query to load another user from the same tenant
        final LabQuery query2 = LabQuery.builder()
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
                .protectionPolicy(ProtectionPolicy.MAM_CA)
                .build();

        final ILabAccount mamCAAccount = mLabClient.getLabAccount(query2);
        final String mamCaAccountUsername = mamCAAccount.getUsername();

        final TokenRequestLatch latch2 = new TokenRequestLatch(1);

        // try sign in with an account from the same tenant
        final SignInParameters signInParameters2 = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mamCaAccountUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(failureInteractiveCallback(latch2, ErrorCodes.INVALID_PARAMETER))
                .build();
        singleAccountPCA.signIn(signInParameters2);

        latch2.await(TokenRequestTimeout.MEDIUM);

        // 833515 and 2495140
        // install and launch the Azure Sample app
        // 833515 is covered by 2495140
        azureSampleApp.install();
        azureSampleApp.launch();
        azureSampleApp.confirmSignedIn(basicAccountUsername);
        Logger.i(TAG, "Azure sample verified signed in account.");
        final TokenRequestLatch silentTokenLatch = new TokenRequestLatch(1);

        final AcquireTokenSilentParameters silentParametersForInterrupt = new AcquireTokenSilentParameters.Builder()
                .forAccount(getAccount())
                .fromAuthority(getAuthority())
                .withScopes(Collections.singletonList("User.read"))
                .forceRefresh(false)
                .withCallback(new SilentAuthenticationCallback() {
                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        Assert.assertFalse(StringUtil.isEmpty(authenticationResult.getAccessToken()));
                        silentTokenLatch.countDown();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        Assert.assertTrue(exception.getMessage().contains("thread interrupted"));
                        silentTokenLatch.countDown();
                    }
                })
                .build();
        // Advance time by a day to force the silent request to do network call
        TestContext.getTestContext().getTestDevice().getSettings().forwardDeviceTimeForOneDay();
        Logger.i(TAG, "Performing a silent request from automation app.");
        mApplication.acquireTokenSilentAsync(silentParametersForInterrupt);

        // wait for sometime for the network requests to start from silent call.
        // This is to ensure that silent call reads the data from cache and makes network call
        // before its cleaned up by signout operation
        ThreadUtils.sleepSafely(600, TAG, "Sleep failed");

        final TokenRequestLatch signOutLatch = new TokenRequestLatch(1);
        Logger.i(TAG, "Triggering sign out from the application");
        ((SingleAccountPublicClientApplication) mApplication).signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
            @Override
            public void onSignOut() {
                signOutLatch.countDown();
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                Assert.fail("Sign out failed: " + exception.getMessage());
            }
        });

        signOutLatch.await(TokenRequestTimeout.LONG);
        silentTokenLatch.await(TokenRequestTimeout.LONG);

        Logger.i(TAG, "Confirming account is signed out in Azure.");
        azureSampleApp.launch();
        azureSampleApp.confirmSignedIn("None");
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userRole(UserRole.CLOUD_DEVICE_ADMINISTRATOR)
                .build();
    }

    @Override
    public TempUserType getTempUserType() {
        return null;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
    }

    @Override
    public int getConfigFileResourceId() {
    return R.raw.msal_config_default;
}

}