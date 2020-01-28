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
package com.microsoft.identity.client.msal.automationapp;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.test.uiautomator.UiDevice;

import com.microsoft.identity.internal.testutils.TestUtils;

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

public abstract class AcquireTokenAbstractTest extends PublicClientApplicationAbstractTest implements IAcquireTokenTest {

    private static final String TAG = AcquireTokenAbstractTest.class.getSimpleName();

    protected String[] mScopes;

    @Before
    public void setup() {
        mScopes = getScopes();
        super.setup();
        //installApp();
    }

    @After
    public void cleanup() {
        super.cleanup();
        AcquireTokenTestHelper.setAccount(null);
        // remove everything from cache after test ends
        TestUtils.clearCache(SHARED_PREFERENCES_NAME);
        //clearCache(mContext, 0);
        removeApp();
    }

    //helper method for clearCache() , recursive
    //returns number of deleted files
    static int clearCacheFolder(final File dir, final int numDays) {

        int deletedFiles = 0;
        if (dir != null && dir.isDirectory()) {
            try {
                for (File child : dir.listFiles()) {

                    //first delete subdirectories recursively
                    if (child.isDirectory()) {
                        deletedFiles += clearCacheFolder(child, numDays);
                    }

                    //then delete the files and subdirectories in this dir
                    //only empty directories can be deleted, so subdirs have been done first
                    if (child.lastModified() < new Date().getTime() - numDays * DateUtils.DAY_IN_MILLIS) {
                        if (child.delete()) {
                            deletedFiles++;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, String.format("Failed to clean the cache, error %s", e.getMessage()));
            }
        }
        return deletedFiles;
    }

    /*
     * Delete the files older than numDays days from the application cache
     * 0 means all files.
     */
    public static void clearCache(final Context context, final int numDays) {
        Log.i(TAG, String.format("Starting cache prune, deleting files older than %d days", numDays));
        int numDeletedFiles = clearCacheFolder(context.getCacheDir(), numDays);
        Log.i(TAG, String.format("Cache pruning completed, %d files deleted", numDeletedFiles));
    }

    public void installApp() {
        UiDevice mDevice = UiDevice.getInstance(getInstrumentation());
        try {
            String output = mDevice.executeShellCommand("pm install " + "com.azure.authenticator");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeApp() {
        UiDevice mDevice = UiDevice.getInstance(getInstrumentation());
        try {
            String output = mDevice.executeShellCommand("pm uninstall " + mContext.getPackageName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
