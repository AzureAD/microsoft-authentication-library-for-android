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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.brokerapi;

import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LocalBrokerHostDebugUiTest;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

// SSO Token Requests
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1561652
@SupportedBrokers(brokers = {BrokerHost.class})
@LocalBrokerHostDebugUiTest
@RetryOnFailure
public class TestCase1561652 extends AbstractMsalBrokerTest {
    @Test
    public void test_1561652() {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        mBroker.performDeviceRegistration(username, password);

        final String nonce = "testNonce";
        // Get SSO token and decode to confirm nonce
        final String ssoToken = ((BrokerHost) mBroker).acquireSSOToken(nonce);

        decodeSSOTokenAndVerifyNonce(ssoToken, nonce);
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
        return mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }

    /**
     * Decode SSO token and verify the expected nonce
     */
    public void decodeSSOTokenAndVerifyNonce(@NonNull final String ssoToken,
                                             @NonNull final String nonce) {
        Assert.assertFalse("Passed an empty or null token", StringUtil.isNullOrEmpty(ssoToken));
        String token = new String(Base64.decode(ssoToken.split("\\.")[1], Base64.NO_WRAP));
        final Map<Object, Object> map = new Gson().fromJson(token, Map.class);
        StringBuilder sb = new StringBuilder();
        final Set<Map.Entry<Object, Object>> set = map.entrySet();
        for (Map.Entry<Object, Object> e : set) {
            sb.append(e.getKey()).append(" => ")
                    .append(e.getValue())
                    .append('\n');
        }
        final String decodedToken = sb.toString();
        if (decodedToken.contains("request_nonce")) {
            final String[] str = decodedToken.split("request_nonce => ");
            if (str.length > 1) {
                Assert.assertEquals(str[1].trim(), nonce);
            } else {
                Assert.fail("decoded token does not contain correct nonce");
            }
        } else {
            Assert.fail("decoded token does not contain correct nonce");
        }
    }
}
