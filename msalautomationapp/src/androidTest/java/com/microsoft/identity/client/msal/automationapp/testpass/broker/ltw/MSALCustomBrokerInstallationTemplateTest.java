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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.ltw;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalCustomBrokerInstallationTest;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Use this class as a template for writing tests with complex broker installation steps
 */
@Ignore("We don't want to actually run this test on pipeline, as this is only meant to be used as reference")
public class MSALCustomBrokerInstallationTemplateTest extends AbstractMsalCustomBrokerInstallationTest {

    // Install Old Authenticator
    private BrokerMicrosoftAuthenticator mAuthenticator = installOldAuthenticator();

    // Install updated Brokerhost
    private BrokerHost mBrokerHost = installBrokerHost();

    @Test
    public void test_template() {
        // Test something

        // Update authenticator
        mAuthenticator.update();
        // Uninstall brokerhost
        mBrokerHost.uninstall();

        // Test something
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(UserType.CLOUD)
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
