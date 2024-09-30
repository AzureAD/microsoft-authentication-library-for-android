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

import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest
import com.microsoft.identity.client.ui.automation.broker.BrokerHost
import com.microsoft.identity.client.ui.automation.utils.CommonUtils
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils
import com.microsoft.identity.labapi.utilities.client.ILabAccount
import com.microsoft.identity.labapi.utilities.client.LabQuery
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import com.microsoft.identity.labapi.utilities.constants.UserType
import org.junit.Assert
import org.junit.Before

/**
 * Parent class for Brokered MSA tests. Ensures PRTv3 is enabled before tests are run
 */
abstract class AbstractMsaBrokerTest : AbstractMsalBrokerTest() {

    override fun getAuthority(): String {
        return mApplication.configuration.defaultAuthority.toString()
    }

    override fun getLabQuery(): LabQuery {
        return LabQuery.builder()
            .userType(UserType.MSA)
            .build()
    }

    override fun getTempUserType(): TempUserType? {
        return null
    }

    override fun getScopes(): Array<String> {
        return arrayOf("User.read")
    }
}
