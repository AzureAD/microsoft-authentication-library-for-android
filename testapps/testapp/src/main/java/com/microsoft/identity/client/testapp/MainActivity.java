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

package com.microsoft.identity.client.testapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.Toast;

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
import com.microsoft.identity.client.UiBehavior;
import com.microsoft.identity.client.User;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The app's main activity.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        AcquireTokenFragment.OnFragmentInteractionListener, CacheFragment.OnFragmentInteractionListener {

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
    private User mUser;
    private Handler mHandler;

    private String mAuthority;
    private String[] mScopes;
    private UiBehavior mUiBehavior;
    private String mLoginHint;
    private String mExtraQp;
    private String[] mExtraScopesToConsent;
    private boolean mEnablePiiLogging;
    private boolean mForceRefresh;
    private AuthenticationResult mAuthResult;

    private RelativeLayout mContentMain;

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
     * Application can also set the component name. There are cases that other sdks will also take dependency on MSAL i.e. microsoft graph sdk
     * or Intune mam sdk, providing the component name will help separate the logs from application and the logs from the sdk running inside of
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

        mContentMain = (RelativeLayout) findViewById(R.id.content_main);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            // auto select the first item
            onNavigationItemSelected(navigationView.getMenu().getItem(0));
        }

        if (mApplication == null) {
            mApplication = new PublicClientApplication(this.getApplicationContext());
        }

    }

    @Override
    public boolean onNavigationItemSelected(final MenuItem item) {
        final Fragment fragment;
        int menuItemId = item.getItemId();
        if (menuItemId == R.id.nav_acquire) {
            fragment = new AcquireTokenFragment();
        } else if (menuItemId == R.id.nav_result) {
            fragment = new ResultFragment();
            final Bundle bundle = new Bundle();
            if (mAuthResult != null) {
                bundle.putString(ResultFragment.ACCESS_TOKEN, mAuthResult.getAccessToken());
                bundle.putString(ResultFragment.ID_TOKEN, mAuthResult.getIdToken());
                bundle.putString(ResultFragment.DISPLAYABLE, mAuthResult.getUser().getDisplayableId());
            }

            fragment.setArguments(bundle);
            mAuthResult = null;
        } else if (menuItemId == R.id.nav_cache) {
            fragment = new CacheFragment();
            final Bundle args = new Bundle();
            args.putSerializable(CacheFragment.ARG_LIST_CONTENTS, (Serializable) CacheFragment.TEST_LIST_ELEMENTS);
            fragment.setArguments(args);
        } else if (menuItemId == R.id.nav_log) {
            fragment = new LogFragment();
            final String logs = ((MsalSampleApp)this.getApplication()).getLogs();
            final Bundle bundle = new Bundle();
            bundle.putString(LogFragment.LOG_MSG, logs);
            fragment.setArguments(bundle);
        }else {
            fragment = null;
        }

        attachFragment(fragment);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mApplication.handleInteractiveRequestRedirect(requestCode, resultCode, data);
    }

    @Override
    public void onGetUser() {
        final Fragment fragment = new UsersFragment();
        attachFragment(fragment);
    }

    List<User> getUsers() {
        try {
            return mApplication.getUsers();
        } catch (final MsalClientException e) {
            Log.e(TAG, "Fail to retrieve users: " + e.getMessage(), e);
        }

        showMessage("No user found.");
        return Collections.emptyList();
    }

    private void attachFragment(final Fragment fragment) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.closeDrawer(GravityCompat.START);
        fragmentTransaction.replace(mContentMain.getId(), fragment).addToBackStack(null).commit();
    }

    @Override
    public void onAcquireTokenClicked(final AcquireTokenFragment.RequestOptions requestOptions) {
        prepareRequestParameters(requestOptions);

        if (mEnablePiiLogging) {
            Logger.getInstance().setEnableLogcatLog(mEnablePiiLogging);
        }

        callAcquireToken(mScopes, mUiBehavior, mLoginHint, mExtraQp, mExtraScopesToConsent);
    }

    public void onRemoveUserClicked() {
        try {
            final List<User> users = mApplication.getUsers();
            for (final User user : users) {
                mApplication.remove(user);
            }
        } catch (final MsalClientException e) {
            Log.e(TAG, "Fail to retrieve users: " + e.getMessage(), e);
        }

        mUser = null;
    }

    User getUser(String loginHint) {
        try {
            final List<User> users = mApplication.getUsers();
            for (final User user : users) {
                if (user.getDisplayableId().equals(loginHint.trim().toLowerCase())) {
                    return user;
                }
            }

            showMessage("No record of user with this login hint.");
        } catch (final MsalClientException e) {
            Log.e(TAG, "Fail to retrieve users: " + e.getMessage(), e);
        }

        return null;
    }


    @Override
    public void onAcquireTokenSilentClicked(final AcquireTokenFragment.RequestOptions requestOptions) {
        prepareRequestParameters(requestOptions);
        final User requestUser = getUser(requestOptions.getLoginHint());

        if (requestUser == null) {
            showMessage("Please select an user.");
            return;
        }

        callAcquireTokenSilent(mScopes, requestUser, mForceRefresh);
    }

    void prepareRequestParameters(final AcquireTokenFragment.RequestOptions requestOptions) {
        mAuthority = getAuthority(requestOptions.getAuthorityType());
        mLoginHint = requestOptions.getLoginHint();
        mUiBehavior = requestOptions.getUiBehavior();
        mEnablePiiLogging = requestOptions.enablePiiLogging();
        mForceRefresh = requestOptions.forceRefresh();

        final String scopes = requestOptions.getScopes();
        if (scopes == null) {
            throw new IllegalArgumentException("null scope");
        }

        mScopes = scopes.toLowerCase().split(" ");
        mExtraScopesToConsent = requestOptions.getExtraScopesToConsent() == null ? null : requestOptions.getExtraScopesToConsent().toLowerCase().split(" ");
    }

    final String getAuthority(Constants.AuthorityType authorityTypeType) {
        switch (authorityTypeType) {
            case AAD_COMMON :
                return Constants.AAD_AUTHORITY;
            case B2C:
                return "B2c is not configured yet";
            case AAD_MSDEVEX:
                return Constants.AAD_MSDEVEX;
            case AAD_GUEST:
                return Constants.AAD_GUEST;
        }

        throw new IllegalArgumentException("Not supported authority type");
    }

    void setUser(final User user) {
        mUser = user;
    }

    private void callAcquireToken(final String[] scopes, final UiBehavior uiBehavior, final String loginHint,
                                  final String extraQueryParam, final String[] extraScope) {
        // The sample app is having the PII enable setting on the MainActivity. Ideally, app should decide to enable Pii or not,
        // if it's enabled, it should be  the setting when the application is onCreate.
        if (mEnablePiiLogging) {
            Logger.getInstance().setEnablePII(true);
        } else {
            Logger.getInstance().setEnablePII(false);
        }

        try {
            mApplication.setValidateAuthority(true);
            mApplication.acquireToken(this, scopes, loginHint, uiBehavior, extraQueryParam, extraScope,
                    null, getAuthenticationCallback());
        } catch (IllegalArgumentException e) {
            showMessage("Scope cannot be blank.");
        }
    }

    private void callAcquireTokenSilent(final String[] scopes, final User user,  boolean forceRefresh) {
        mApplication.acquireTokenSilentAsync(scopes, user, null, forceRefresh, getAuthenticationCallback());
    }

    private AuthenticationCallback getAuthenticationCallback() {
        return new AuthenticationCallback() {

            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                mAuthResult = authenticationResult;
                onNavigationItemSelected(getNavigationView().getMenu().getItem(1));
                mUser = null;
            }

            @Override
            public void onError(MsalException exception) {
                // Check the exception type.
                if (exception instanceof MsalClientException) {
                    // This means errors happened in the sdk itself, could be network, Json parse, etc. Check MsalError.java
                    // for detailed list of the errors.
                    showMessage(exception.getMessage());
                } else if (exception instanceof MsalServiceException) {
                    // This means something is wrong when the sdk is communication to the service, mostly likely it's the client
                    // configuration.
                    showMessage(exception.getMessage());
                } else if (exception instanceof MsalUiRequiredException) {
                    // This explicitly indicates that developer needs to prompt the user, it could be refresh token is expired, revoked
                    // or user changes the password; or it could be that no token was found in the token cache.
                    callAcquireToken(mScopes, mUiBehavior, mLoginHint, mExtraQp, mExtraScopesToConsent);
                }
            }

            @Override
            public void onCancel() {
                showMessage("User cancelled the flow.");
            }
        };
    }

    private NavigationView getNavigationView() {
        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        return navigationView;
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

    @Override
    public void onDeleteToken(int position, final CacheFragment cacheFragment) {
        Log.d(TAG, "onDeleteToken(" + position + ")");
        cacheFragment.setLoading();
        // TODO delete the items or whatever
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                cacheFragment.reload(CacheFragment.TEST_LIST_ELEMENTS);
            }
        }, 750L);
    }
}
