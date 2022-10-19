package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

@SupportedBrokers(brokers = {BrokerMicrosoftAuthenticator.class})
public class TestCase833511 extends AbstractMsalBrokerTest{

    @Test
    public void test_833511() {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        //perform device registration
        mBroker.performSharedDeviceRegistrationDontValidate(username, password);

        final UiDevice device =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final UiSelector sharedDeviceConfirmationSelector = new UiSelector()
                .descriptionContains("Shared Device Mode")
                .className("android.widget.ImageView");

        //confirm that we are not in Shared Device Mode inside Authenticator - WPJ should fail
        final UiObject sharedDeviceConfirmation = device.findObject(sharedDeviceConfirmationSelector);
        sharedDeviceConfirmation.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
        Assert.assertFalse(
                "Microsoft Authenticator - Shared Device Confirmation page appears.",
                sharedDeviceConfirmation.exists());
    }
    @Override
    public LabQuery getLabQuery() {
        return null;
    }

    @Override
    public TempUserType getTempUserType() {
        return TempUserType.BASIC;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }
}
