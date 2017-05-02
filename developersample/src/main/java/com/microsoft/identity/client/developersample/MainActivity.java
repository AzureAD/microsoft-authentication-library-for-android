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

package com.microsoft.identity.client.developersample;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.ILoggerCallback;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.client.MsalClientException;
import com.microsoft.identity.client.IMsalEventReceiver;
import com.microsoft.identity.client.MsalException;
import com.microsoft.identity.client.MsalServiceException;
import com.microsoft.identity.client.MsalUiRequiredException;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.Telemetry;
import com.microsoft.identity.client.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements GraphData.OnFragmentInteractionListener, SigninFragment.OnFragmentInteractionListener{

    private static final String TAG = MainActivity.class.getSimpleName();

    static {
        Telemetry.getInstance().registerReceiver(new IMsalEventReceiver() {
            @Override
            public void onEventsReceived(List<Map<String, String>> events) {
                Log.d(TAG, "Received events");
                Log.d(TAG, "Event count: [" + events.size() + "]");
                for (final Map<String, String> event : events) {
                    Log.d(TAG, "Begin event --------");
                    for (final String key : event.keySet()) {
                        Log.d(TAG, "\t" + key + " :: " + event.get(key));
                    }
                    Log.d(TAG, "End event ----------");
                }
            }
        });
    }

    private PublicClientApplication mApplication;

    private static final String[] SCOPES = {"https://graph.microsoft.com/User.Read"};
    private static final String CLIENT_ID = "9851987a-55e5-46e2-8d70-75f8dc060f21";
    final static String MSGRAPH_URL = "https://graph.microsoft.com/v1.0/me";

    /**
     * When initializing the {@link PublicClientApplication}, all the apps should only provide us the application context instead of
     * the running activity itself. If running activity itself is provided, that will have the sdk hold a strong reference of the activity
     * which could potentially cause the object not correctly garbage collected and cause activity leak.
     *
     * External Logger should be provided by the Calling app. The sdk logs to the logcat by default, and loglevel is enabled at verbose level.
     * To set external logger,
     * {@link Logger#setExternalLogger(ILoggerCallback)}.
     * To set log level,
     * {@link Logger#setLogLevel(Logger.LogLevel)}
     * By default, the sdk won't give back any Pii logging. However the app can turn it on, this is up to the application's privacy policy.
     * To turn on the Pii logging,
     * {@link Logger#setEnablePII(boolean)}
     * Application can also set the component name. There are cases that other sdks will also take dependency on MSAL i.e. microsoft graph sdk,
     * providing the component name will help separate the logs from application and the logs from the sdk running inside of
     * the apps.
     * To set component name:
     * {@link PublicClientApplication#setComponent(String)}
     *
     * For the {@link AuthenticationCallback}, MSAL exposes three results 1) Success, which contains the {@link AuthenticationResult} 2) Failure case,
     * which contains {@link MsalException} and 3) Cancel, specifically for user canceling the flow.
     *
     * For the failure case, MSAL exposes three sub exceptions:
     * 1) {@link MsalClientException}, which is specifically for the exceptions running inside the client app itself, could be no active network,
     * Json parsing failure, etc.
     * 2) {@link MsalServiceException}, which is the error that the sdk gets back when communicating to the service, could be oauth2 errors, socket timout
     * or 500/503/504. For oauth2 erros, MSAL returns back the exact error that server returns back to the sdk.
     * 3) {@link MsalUiRequiredException}, which means that UI is required.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (mApplication == null) {
            mApplication = new PublicClientApplication(this.getApplicationContext(), CLIENT_ID);
        }

        List<User> users = null;
        try {
            users = mApplication.getUsers();
        } catch (final MsalException exc) {
            Log.e(TAG, "Exception when getting users", exc);
        }

        if (users != null && users.size() > 0) {

            mApplication.acquireTokenSilentAsync(SCOPES, users.get(0), new AuthenticationCallback() {
                @Override
                public void onSuccess(AuthenticationResult authenticationResult) {
                    Log.e("TAG", "Success");
                    callGraphAPI(authenticationResult);
                }

                @Override
                public void onError(MsalException exception) {
                    Log.e("TAG", "Error");
                }

                @Override
                public void onCancel() {
                    Log.e("TAG", "Cancel");
                }
            });
        } else {
            updateSignedOutUI();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mApplication.handleInteractiveRequestRedirect(requestCode, resultCode, data);
    }

    @Override
    public void onSigninClicked() {
        mApplication.acquireToken(this, SCOPES, new AuthenticationCallback() {
            @Override
            public void onSuccess(final AuthenticationResult authenticationResult) {
                Log.e("TAG", "Success");
                callGraphAPI(authenticationResult);
            }

            @Override
            public void onError(final MsalException exception) {
                Log.e("TAG", "Error");
            }

            @Override
            public void onCancel() {
                Log.e("TAG", "Cancel");
            }
        });
    }

    /* Set the UI for successful token acquisition data */
    private void updateSuccessUI(final AuthenticationResult authenticationResult) {

        //callGraphAPI(authenticationResult);

    }

    private void updateSignedOutUI() {
        final Fragment signinFragment = new SigninFragment();
        attachFragment(signinFragment);
    }

    @Override
    public void onSignoutClicked() {
        List<User> users = null;
        try {
            users = mApplication.getUsers();
        } catch (final MsalException exc) {
            Log.e(TAG, "Exception when getting users", exc);
            return;
        }
        if (users != null && users.size() >= 1) {
            mApplication.remove(users.get(0));
        }
        updateSignedOutUI();
    }

    /* Use Volley to make an HTTP request to the /me endpoint from MS Graph using an access token */
    private void callGraphAPI(final AuthenticationResult authResult) {
        Log.d(TAG, "Starting volley request to graph");

        /* Make sure we have a token to send to graph */
        if (authResult.getAccessToken() == null) {return;}

        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject parameters = new JSONObject();

        try {
            parameters.put("key", "value");
        } catch (Exception e) {
            Log.d(TAG, "Failed to put parameters: " + e.toString());
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, MSGRAPH_URL,
                parameters,new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                /* Successfully called graph, process data and send to UI */
                Log.d(TAG, "Response: " + response.toString());

                updateGraphUI(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error: " + error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + authResult.getAccessToken());
                return headers;
            }
        };

        Log.d(TAG, "Adding HTTP GET to Queue, Request: " + request.toString());

        request.setRetryPolicy(new DefaultRetryPolicy(
                3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    private void updateGraphUI(JSONObject graphResponse) {
        final Fragment graphDataFragment = GraphData.newInstance(graphResponse.toString());

        attachFragment(graphDataFragment);
    }

    private void attachFragment(final Fragment fragment) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.replace(R.id.activity_main, fragment).addToBackStack(null).commitAllowingStateLoss();
    }
}
