// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client;


import org.junit.Assert;
import org.junit.Test;

public final class UIBehaviorTest {

    public final static String LOGIN = "login";
    public final static String CONSENT = "consent";
    public final static String SELECT_ACCOUNT = "select_account";


    @Test
    public void testToStringForceLogin() {
        Prompt behavior = Prompt.LOGIN;
        Assert.assertEquals(behavior.toString(), LOGIN);
    }

    @Test
    public void testToStringConsent() {
        Prompt behavior = Prompt.CONSENT;
        Assert.assertEquals(behavior.toString(), CONSENT);
    }

    @Test
    public void testToStringSelectAccount() {
        Prompt behavior = Prompt.SELECT_ACCOUNT;
        Assert.assertEquals(behavior.toString(), SELECT_ACCOUNT);
    }
}
