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

import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.ILoggerCallback;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The app's main activity.
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        AcquireTokenFragment.OnFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private IAuthenticationResult mAuthResult;

    private RelativeLayout mContentMain;

    private MsalWrapper.INotifyOperationResultCallback operationResultCallback = new MsalWrapper.INotifyOperationResultCallback() {
        @Override
        public void acquireTokenSucceed(IAuthenticationResult result) {
            mAuthResult = result;
            onNavigationItemSelected(getNavigationView().getMenu().getItem(1));
        }

        @Override
        public void notify(final String message) {
            getHandler().post(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    /**
     * When initializing the {@link PublicClientApplication}, all the apps should only provide us the application context instead of
     * the running activity itself. If running activity itself is provided, that will have the sdk hold a strong reference of the activity
     * which could potentially cause the object not correctly garbage collected and cause activity leak.
     * <p>
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
     * <p>
     * For the {@link AuthenticationCallback}, MSAL exposes three results 1) Success, which contains the {@link AuthenticationResult} 2) Failure case,
     * which contains {@link MsalException} and 3) Cancel, specifically for user canceling the flow.
     * <p>
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

        mContentMain = findViewById(R.id.content_main);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        final NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Provide secret key for token encryption.
        try {
            // For API version lower than 18, you have to provide the secret key. The secret key
            // needs to be 256 bits. You can use the following way to generate the secret key. And
            // use AuthenticationSettings.Instance.setSecretKey(secretKeyBytes) to supply us the key.
            // For API version 18 and above, we use android keystore to generate keypair, and persist
            // the keypair in AndroidKeyStore. Current investigation shows 1)Keystore may be locked with
            // a lock screen, if calling app has a lot of background activity, keystore cannot be
            // accessed when locked, we'll be unable to decrypt the cache items 2) AndroidKeystore could
            // be reset when gesture to unlock the device is changed.
            // We do recommend the calling app the supply us the key with the above two limitations.
            if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
                // use same key for tests
                SecretKeyFactory keyFactory = SecretKeyFactory
                        .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
                SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                        "abcdedfdfd".getBytes("UTF-8"), 100, 256));
                SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
                AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | UnsupportedEncodingException ex) {
            showMessage("Fail to generate secret key:" + ex.getMessage());
        }

        if (savedInstanceState == null) {
            // auto select the first item
            onNavigationItemSelected(navigationView.getMenu().getItem(0));
        }

        MsalWrapper.getInstance().loadMsalApplication(this.getApplicationContext(),
                R.raw.msal_config,
                operationResultCallback,
                null
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        MsalWrapper.getInstance().onResume(operationResultCallback);
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
                bundle.putString(ResultFragment.DISPLAYABLE, mAuthResult.getAccount().getUsername());
            }

            fragment.setArguments(bundle);
            mAuthResult = null;
        } else if (menuItemId == R.id.nav_log) {
            fragment = new LogFragment();
            final String logs = ((MsalSampleApp) this.getApplication()).getLogs();
            final Bundle bundle = new Bundle();
            bundle.putString(LogFragment.LOG_MSG, logs);
            fragment.setArguments(bundle);
        } else {
            fragment = null;
        }

        attachFragment(fragment);

        return true;
    }

    @Override
    public void onGetUser() {
        final Fragment fragment = new UsersFragment();
        attachFragment(fragment);
    }

    private void attachFragment(final Fragment fragment) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        final DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.closeDrawer(GravityCompat.START);
        fragmentTransaction.replace(mContentMain.getId(), fragment).addToBackStack(null).commitAllowingStateLoss();
    }

    @Override
    public void onAcquireTokenClicked(final AcquireTokenFragment.RequestOptions requestOptions) {
        loadMsalApplicationFromRequestParameters(requestOptions, new MsalWrapper.IMsalApplicationLoaded() {
            @Override
            public void onApplicationLoaded() {
                MsalWrapper.getInstance().acquireToken(MainActivity.this, requestOptions, operationResultCallback);
            }
        });
    }

    @Override
    public void onAcquireTokenWithResourceClicked(final AcquireTokenFragment.RequestOptions requestOptions) {
        loadMsalApplicationFromRequestParameters(requestOptions, new MsalWrapper.IMsalApplicationLoaded() {
            @Override
            public void onApplicationLoaded() {
                MsalWrapper.getInstance().acquireTokenWithResourceId(MainActivity.this, requestOptions, operationResultCallback);
            }
        });
    }

    @Override
    public void onAcquireTokenSilentWithResourceClicked(final AcquireTokenFragment.RequestOptions requestOptions) {
        loadMsalApplicationFromRequestParameters(requestOptions, new MsalWrapper.IMsalApplicationLoaded() {
            @Override
            public void onApplicationLoaded() {
                MsalWrapper.getInstance().acquireTokenSilentWithResource(requestOptions, operationResultCallback);
            }
        });
    }

    public void onRemoveUserClicked(final String username) {
        MsalWrapper.getInstance().removeAccount(username, operationResultCallback);
    }

    @Override
    public void onAcquireTokenSilentClicked(final AcquireTokenFragment.RequestOptions requestOptions) {
        loadMsalApplicationFromRequestParameters(requestOptions, new MsalWrapper.IMsalApplicationLoaded() {
            @Override
            public void onApplicationLoaded() {
                MsalWrapper.getInstance().acquireTokenSilent(requestOptions, operationResultCallback);
            }
        });
    }

    @Override
    public void bindSelectAccountSpinner(final Spinner selectUser,
                                         final List<IAccount> accounts) {
        final ArrayAdapter<String> userAdapter = new ArrayAdapter<>(
                getApplicationContext(), android.R.layout.simple_spinner_item,
                new ArrayList<String>() {{
                    if (accounts != null) {
                        for (IAccount account : accounts)
                            add(account.getUsername());
                    }
                }}
        );
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectUser.setAdapter(userAdapter);
    }

    void loadMsalApplicationFromRequestParameters(final AcquireTokenFragment.RequestOptions requestOptions,
                                                  final MsalWrapper.IMsalApplicationLoaded postApplicationLoaded) {

        boolean enablePiiLogging = requestOptions.mEnablePII;
        // The sample app is having the PII enable setting on the MainActivity. Ideally, app should decide to enable Pii or not,
        // if it's enabled, it should be set when the application is onCreate.
        Logger.getInstance().setEnableLogcatLog(enablePiiLogging);
        if (enablePiiLogging) {
            Logger.getInstance().setEnablePII(true);
        } else {
            Logger.getInstance().setEnablePII(false);
        }

        Constants.UserAgent userAgent = requestOptions.getUserAgent();
        //Azure Active Environment (PPE vs. Prod)
        Constants.AzureActiveDirectoryEnvironment environment = requestOptions.getEnvironment();

        int configFileResourceId = R.raw.msal_config;

        if (userAgent.name().equalsIgnoreCase("BROWSER")) {
            configFileResourceId = R.raw.msal_config_browser;
        } else if (userAgent.name().equalsIgnoreCase("WEBVIEW")) {
            configFileResourceId = R.raw.msal_config_webview;
        }

        if (environment == Constants.AzureActiveDirectoryEnvironment.PREPRODUCTION) {
            configFileResourceId = R.raw.msal_ppe_config;
        }

        MsalWrapper.getInstance().loadMsalApplication(this.getApplicationContext(),
                configFileResourceId,
                operationResultCallback,
                postApplicationLoaded
        );
    }

    private NavigationView getNavigationView() {
        final NavigationView navigationView = findViewById(R.id.nav_view);
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
        return new Handler(MainActivity.this.getMainLooper());
    }
}
