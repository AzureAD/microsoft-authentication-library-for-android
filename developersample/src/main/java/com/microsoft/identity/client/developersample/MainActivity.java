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

import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import com.microsoft.identity.client.IMsalEventReceiver;
import com.microsoft.identity.client.MsalException;
import com.microsoft.identity.client.Telemetry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements GraphData.OnFragmentInteractionListener, SigninFragment.OnFragmentInteractionListener,
        AuthUtil.AuthenticatedTask {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final static String MSGRAPH_URL = "https://graph.microsoft.com/v1.0/me";
    private AuthUtil mAuthUtil;

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

    // Called from SigninFragment when the signin button is clicked
    @Override
    public void onSigninClicked() {
        mAuthUtil.doAcquireToken(this);
    }

    // Called from GraphData when the Signout button is clicked
    @Override
    public void onSignoutClicked() {
        mAuthUtil.doSignout();
        updateSignedOutUI();
    }

    // Called from GraphData when the extra scope button is clicked
    @Override
    public void onExtraScopeRequested() {
        mAuthUtil.doExtraScopeRequest();
    }

    // Called from AuthUtil on the failure callback
    @Override
    public void onRequestFailure(final MsalException exception) {
        // Failure UI or mitigation for signin failure should be handled here
    }

    // Called from AuthUtil from the success callback
    @Override
    public void useAccessToken(final String accessToken) {
        Log.d(TAG, "Starting volley request to graph");

        // Make sure we have a token to send to graph
        if (accessToken == null) {
            return;
        }

        final RequestQueue queue = Volley.newRequestQueue(this);
        final JSONObject parameters = new JSONObject();
        try {
            parameters.put("key", "value");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to put parameters: " + e.toString());
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, MSGRAPH_URL,
                parameters, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                // Successfully called graph, process data and send to UI
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
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuthUtil = new AuthUtil(this);

        if (mAuthUtil.getUserCount() == 0) {
            updateSignedOutUI();
        } else {
            mAuthUtil.doAcquireTokenSilent(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mAuthUtil.doCallback(requestCode, resultCode, data);
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

    private void updateSignedOutUI() {
        final Fragment signinFragment = new SigninFragment();
        attachFragment(signinFragment);
    }
}
