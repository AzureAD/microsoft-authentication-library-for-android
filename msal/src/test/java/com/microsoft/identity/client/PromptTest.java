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
package com.microsoft.identity.client;

import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PromptTest {
    private Prompt prompt;

    @Test
    public void testOpenIdConnectParameterSelectAccount() {
        prompt = Prompt.SELECT_ACCOUNT;
        final OpenIdConnectPromptParameter promptValue = prompt.toOpenIdConnectPromptParameter();
        Assert.assertEquals(promptValue, OpenIdConnectPromptParameter.SELECT_ACCOUNT);
    }

    @Test
    public void testOpenIdConnectParameterLogin() {
        prompt = Prompt.LOGIN;
        final OpenIdConnectPromptParameter promptValue = prompt.toOpenIdConnectPromptParameter();
        Assert.assertEquals(promptValue, OpenIdConnectPromptParameter.LOGIN);
    }

    @Test
    public void testOpenIdConnectParameterConsent() {
        prompt = Prompt.CONSENT;
        final OpenIdConnectPromptParameter promptValue = prompt.toOpenIdConnectPromptParameter();
        Assert.assertEquals(promptValue, OpenIdConnectPromptParameter.CONSENT);
    }

    @Test
    public void testOpenIdConnectParameterWhenRequired() {
        prompt = Prompt.WHEN_REQUIRED;
        final OpenIdConnectPromptParameter promptValue = prompt.toOpenIdConnectPromptParameter();
        Assert.assertEquals(promptValue, OpenIdConnectPromptParameter.UNSET);
    }

}
