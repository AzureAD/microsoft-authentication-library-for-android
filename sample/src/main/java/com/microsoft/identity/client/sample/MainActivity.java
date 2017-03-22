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

package com.microsoft.identity.client.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.MsalException;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.UIBehavior;
import com.microsoft.identity.client.User;

public class MainActivity extends Activity {

    private PublicClientApplication mApplication;
    private static String[] SCOPES = new String [] {"User.Read"};

    private Handler mHandler;
    private static User sUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mApplication = new PublicClientApplication(this);
//
//        final Button buttonForInteractiveRequest = (Button) findViewById(R.id.AcquireTokenInteractiveForR1);
//        buttonForInteractiveRequest.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                callAcquireToken(SCOPES, UIBehavior.FORCE_LOGIN, null, null, null);
//            }
//        });
//
//        final Button buttonForLaunchingChrome = (Button) findViewById(R.id.LaunchChrome);
//        buttonForLaunchingChrome.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                callAcquireToken(SCOPES, UIBehavior.FORCE_LOGIN, null, null, null);
//            }
//        });
//
//        final Button buttonForSilentFlow = (Button) findViewById(R.id.AcquireTokenSilentForR1);
//        buttonForSilentFlow.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                callAcquireTokenSilent(SCOPES, true);
//            }
//        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mApplication.handleInteractiveRequestRedirect(requestCode, resultCode, data);
    }


    private void callAcquireToken(final String[] scopes, final UIBehavior uiBehavior, final String loginHint,
                                  final String extraQueryParam, final String[] additionalScope) {
        mApplication.acquireToken(scopes, loginHint, uiBehavior, extraQueryParam, additionalScope,
                null, new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult o) {
                        showMessage("Receive Success Response " + o.getAccessToken());
                        sUser = o.getUser();
                    }

                    @Override
                    public void onError(final MsalException exception) {
                        showMessage("Receive Failure Response " + exception.getMessage());
                    }

                    @Override
                    public void onCancel() {
                        showMessage("User cancelled the flow.");
                    }
                });
    }

    private void callAcquireTokenSilent(final String[] scopes, boolean forceRefresh) {
        mApplication.acquireTokenSilentAsync(scopes, sUser, null, forceRefresh, new AuthenticationCallback() {

            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                showMessage("Receive Success Response for silent request: " + authenticationResult.getAccessToken());
            }

            @Override
            public void onError(MsalException exception) {
                showMessage("Receive Failure Response for silent request: " + exception.getMessage());
            }

            @Override
            public void onCancel() {
                showMessage("User cancelled the flow.");
            }
        });
    }

    private void showMessage(final String msg) {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private Handler getHandler() {
        if (mHandler == null) {
            return new Handler(MainActivity.this.getMainLooper());
        }

        return mHandler;
    }
}
