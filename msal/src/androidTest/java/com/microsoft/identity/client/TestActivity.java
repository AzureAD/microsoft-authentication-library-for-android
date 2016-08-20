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

import android.app.Activity;
import android.content.Intent;

import java.util.concurrent.CountDownLatch;

/**
 * Test activity for testing purpose.
 */
public class TestActivity extends Activity {

    private final CountDownLatch mSignal;
    private int mRequestCode = 0;
    private final String mPackageName;

    public TestActivity(final CountDownLatch signal, final String packageName) {
        mSignal = signal;
        mPackageName = packageName;
    }

    public TestActivity() {
        mSignal = null;
        mPackageName = null;
    }

    @Override
    public void startActivityForResult(final Intent intent, int requestCode) {
        mRequestCode = requestCode;
        mSignal.countDown();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        InteractiveRequest.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public String getPackageName() {
        return mPackageName;
    }

    int getRequestCode() {
        return mRequestCode;
    }

    boolean isLockReleased() {
        return mSignal.getCount() == 0;
    }
}
