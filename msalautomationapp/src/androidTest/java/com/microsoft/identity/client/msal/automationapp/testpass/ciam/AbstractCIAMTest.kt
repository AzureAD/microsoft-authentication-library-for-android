package com.microsoft.identity.client.msal.automationapp.testpass.ciam

import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest
import com.microsoft.identity.labapi.utilities.client.LabQuery
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import com.microsoft.identity.labapi.utilities.constants.UserType


abstract class AbstractCIAMTest : AbstractMsalUiTest() {
    override fun getLabQuery(): LabQuery? {
        return LabQuery.builder()
            .userType(UserType.B2C)
            .build()
    }

    override fun getTempUserType(): TempUserType? {
        return null
    }

    override fun getScopes(): Array<String> {
        return arrayOf("https://graph.microsoft.com/.default")
    }

    override fun getAuthority(): String {
        return mApplication.configuration.defaultAuthority.authorityURL.toString()
    }
}

