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

import android.content.Context;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.msal.test.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ConstructorTest {


    @Test
    public void testSingleAccountConstructor(){

        Context context = InstrumentationRegistry.getContext();
        try {
            ISingleAccountPublicClientApplication app = PublicClientApplication.createSingleAccountPublicClientApplication(context, R.raw.test_msal_config_single_account);
            Assert.assertTrue(app instanceof ISingleAccountPublicClientApplication);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MsalException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testMultipleAccountConstructor(){

        Context context = InstrumentationRegistry.getContext();
        try {
            IMultipleAccountPublicClientApplication app = PublicClientApplication.createMultipleAccountPublicClientApplication(context, R.raw.test_msal_config_multiple_account);
            Assert.assertTrue(app instanceof IMultipleAccountPublicClientApplication);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MsalException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testMultipleAccountAsyncConstructor(){
        Context context = InstrumentationRegistry.getContext();
        PublicClientApplication.createMultipleAccountPublicClientApplication(context, R.raw.test_msal_config_multiple_account, new PublicClientApplication.ApplicationCreatedListener() {
            @Override
            public void onCreated(IPublicClientApplication application) {
                Assert.assertTrue(application instanceof IMultipleAccountPublicClientApplication);
            }

            @Override
            public void onError(MsalException exception) {
            }
        });
    }

    @Test
    public void testSingleAccountAsyncConstructor(){
        Context context = InstrumentationRegistry.getContext();
        PublicClientApplication.createMultipleAccountPublicClientApplication(context, R.raw.test_msal_config_single_account, new PublicClientApplication.ApplicationCreatedListener() {
            @Override
            public void onCreated(IPublicClientApplication application) {
                Assert.assertTrue(application instanceof ISingleAccountPublicClientApplication);
            }

            @Override
            public void onError(MsalException exception) {
            }
        });
    }

}
