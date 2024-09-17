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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.msa

import com.microsoft.identity.client.msal.automationapp.R
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest
import com.microsoft.identity.client.ui.automation.app.OneDriveApp
import com.microsoft.identity.client.ui.automation.app.OutlookApp
import com.microsoft.identity.client.ui.automation.app.WordApp
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller
import com.microsoft.identity.labapi.utilities.client.LabQuery
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import com.microsoft.identity.labapi.utilities.constants.UserType
import org.junit.Assert
import org.junit.Test

// [Brokered] When Broker Installed, OneDrive and Office should show phone sign up option, Outlook should not
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/3040042
class TestCase3040042 : AbstractMsalBrokerTest(){

    @Test
    @Throws(Throwable::class)
    fun test_3007768() {
        // Word should have phone sign-up option
        val word = WordApp(LocalApkInstaller())
        word.install()
        Assert.assertTrue("Word should have option for phone sign-up, but doesn't...", word.checkPhoneSignUpIsAvailable())

        // OneDrive should have phone sign-up option
        val onedrive = OneDriveApp(LocalApkInstaller())
        onedrive.install()
        Assert.assertTrue("OneDrive should have option for phone sign-up, but doesn't...", word.checkPhoneSignUpIsAvailable())

        // Outlook should NOT have phone sign-up option
        val outlook = OutlookApp(LocalApkInstaller())
        outlook.install();
        Assert.assertTrue("Outlook should not have an option for phone sign-up, but does...", outlook.checkPhoneSignUpIsNotAvailable())
    }

    override fun getScopes(): Array<String> {
        return arrayOf("User.read")
    }

    override fun getAuthority(): String {
        return mApplication.configuration.defaultAuthority.toString()
    }

    override fun getConfigFileResourceId(): Int {
        return R.raw.msal_config_msa_only
    }

    override fun getLabQuery(): LabQuery {
        return LabQuery.builder()
            .userType(UserType.MSA)
            .build()
    }

    override fun getTempUserType(): TempUserType? {
        return null;
    }
}