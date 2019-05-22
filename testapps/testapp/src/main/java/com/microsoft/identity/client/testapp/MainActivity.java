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
import android.util.Pair;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.ILoggerCallback;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.UiBehavior;
import com.microsoft.identity.client.exception.MsalArgumentException;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.Serializable;
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
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        AcquireTokenFragment.OnFragmentInteractionListener, CacheFragment.OnFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private PublicClientApplication mApplication;
    private IAccount mSelectedAccount;
    private Handler mHandler;

    private String mAuthority;
    private String[] mScopes;
    private String mResource;
    private UiBehavior mUiBehavior;
    private String mLoginHint;
    private List<Pair<String, String>> mExtraQp;
    private String[] mExtraScopesToConsent;
    private boolean mEnablePiiLogging;
    private boolean mForceRefresh;
    private IAuthenticationResult mAuthResult;

    private RelativeLayout mContentMain;

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

        if (mApplication == null) {
            mApplication = new PublicClientApplication(this.getApplicationContext(), R.raw.msal_config);
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
                bundle.putString(ResultFragment.DISPLAYABLE, mAuthResult.getAccount().getUsername());
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
        prepareRequestParameters(requestOptions);

        if (mEnablePiiLogging) {
            Logger.getInstance().setEnableLogcatLog(mEnablePiiLogging);
        }

        callAcquireToken(mScopes, mUiBehavior, mLoginHint, mExtraQp, mExtraScopesToConsent);
    }

    @Override
    public void  onAcquireTokenWithResourceClicked(final AcquireTokenFragment.RequestOptions requestOptions) {
        prepareRequestParameters(requestOptions);

        if (mEnablePiiLogging) {
            Logger.getInstance().setEnableLogcatLog(mEnablePiiLogging);
        }

        if (mEnablePiiLogging) {
            Logger.getInstance().setEnablePII(true);
        } else {
            Logger.getInstance().setEnablePII(false);
        }

        try {
            AcquireTokenParameters.Builder builder = new AcquireTokenParameters.Builder();
            AcquireTokenParameters acquireTokenParameters = builder.startAuthorizationFromActivity(this)
                    .withResource(mResource)
                    .withUiBehavior(mUiBehavior)
                    .withAuthorizationQueryStringParameters(mExtraQp)
                    .callback(getAuthenticationCallback())
                    .withLoginHint(mLoginHint)
                    .build();

            mApplication.acquireTokenAsync(acquireTokenParameters);
        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage());
        }
    }

    @Override
    public void onAcquireTokenSilentWithResourceClicked(final AcquireTokenFragment.RequestOptions requestOptions) {
        prepareRequestParameters(requestOptions);

        //final IAccount requestAccount = getAccount();
        mApplication.getAccounts(new PublicClientApplication.LoadAccountCallback() {
            @Override
            public void onTaskCompleted(final List<IAccount> accounts) {
                IAccount requestAccount = null;

                for (final IAccount account : accounts) {
                    if (account.getUsername().equalsIgnoreCase(requestOptions.getLoginHint().trim())) {
                        requestAccount = account;
                        break;
                    }
                }

                if (null != requestAccount) {
                    AcquireTokenSilentParameters.Builder builder = new AcquireTokenSilentParameters.Builder();
                    AcquireTokenSilentParameters acquireTokenSilentParameters =
                            builder.withResource(mResource)
                                    .forAccount(requestAccount)
                                    .forceRefresh(mForceRefresh)
                                    .callback(getAuthenticationCallback())
                                    .build();

                    mApplication.acquireTokenSilentAsync(acquireTokenSilentParameters);
                } else {
                    showMessage("No account found matching loginHint");
                }
            }

            @Override
            public void onError(final Exception exception) {
                showMessage("No account found matching loginHint");
            }
        });
    }

    public void onRemoveUserClicked(final String username) {
        mApplication.getAccounts(new PublicClientApplication.LoadAccountCallback() {
            @Override
            public void onTaskCompleted(List<IAccount> accountsToRemove) {
                for (final IAccount accountToRemove : accountsToRemove) {
                    if (StringUtil.isEmpty(username) || accountToRemove.getUsername().equalsIgnoreCase(username.trim())) {
                        mApplication.removeAccount(
                                accountToRemove,
                                new PublicClientApplication.RemoveAccountCallback() {
                                    @Override
                                    public void onTaskCompleted(Boolean isSuccess) {
                                        if (isSuccess) {
                                            showMessage("The account is successfully removed.");
                                        } else {
                                            showMessage("Failed to remove the account.");
                                        }
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        showMessage(e.getClass().getSimpleName()
                                                + " Exception thrown during removing account.");
                                    }
                                });
                    }
                }
            }

            @Override
            public void onError(Exception exception) {
                showMessage(exception.getClass().getSimpleName()
                        + " Exception thrown during getting accounts.");
            }
        });
    }

    public PublicClientApplication getPublicClientApplication() {
        return mApplication;
    }

    @Override
    public void onAcquireTokenSilentClicked(final AcquireTokenFragment.RequestOptions requestOptions) {
        prepareRequestParameters(requestOptions);
        mApplication.getAccount(
                requestOptions.getLoginHint().trim(),
                new PublicClientApplication.GetAccountCallback() {
                    @Override
                    public void onTaskCompleted(final IAccount account) {
                        if(null != account) {
                            callAcquireTokenSilent(mScopes, account, mForceRefresh);
                        } else {
                            showMessage("No account found matching loginHint");
                        }
                    }

                    @Override
                    public void onError(final Exception e) {
                        showMessage(e.getClass().getSimpleName()
                                + " Exception thrown during getting account.");
                    }
                });
    }

    @Override
    public void bindSelectAccountSpinner(final Spinner selectUser) {
        mApplication.getAccounts(new PublicClientApplication.LoadAccountCallback() {
            @Override
            public void onTaskCompleted(final List<IAccount> accounts) {
                final ArrayAdapter<String> userAdapter = new ArrayAdapter<>(
                        getApplicationContext(), android.R.layout.simple_spinner_item,
                        new ArrayList<String>() {{
                            for (IAccount account : accounts)
                                add(account.getUsername());
                        }}
                );
                userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                selectUser.setAdapter(userAdapter);
            }

            @Override
            public void onError(final Exception e) {
                showMessage(e.getClass().getSimpleName()
                        + " Exception thrown during getting account.");
            }
        });
    }

    void prepareRequestParameters(final AcquireTokenFragment.RequestOptions requestOptions) {
        mAuthority = getAuthority(requestOptions.getAuthorityType());
        mLoginHint = requestOptions.getLoginHint();
        mUiBehavior = requestOptions.getUiBehavior();
        mEnablePiiLogging = requestOptions.enablePiiLogging();
        mForceRefresh = requestOptions.forceRefresh();
        Constants.UserAgent userAgent = requestOptions.getUserAgent();
        //Azure Active Environment (PPE vs. Prod)
        Constants.AzureActiveDirectoryEnvironment environment = requestOptions.getEnvironment();


        final String scopes = requestOptions.getScopes();
        if (scopes == null) {
            throw new IllegalArgumentException("null scope");
        }

        mScopes = scopes.toLowerCase().split(" ");
        mResource = scopes.toLowerCase();
        mExtraScopesToConsent = requestOptions.getExtraScopesToConsent() == null ? null : requestOptions.getExtraScopesToConsent().toLowerCase().split(" ");

        if (userAgent.name().equalsIgnoreCase("BROWSER")) {
            mApplication = new PublicClientApplication(this.getApplicationContext(), R.raw.msal_config_browser);
        } else if (userAgent.name().equalsIgnoreCase("WEBVIEW")) {
            mApplication = new PublicClientApplication(this.getApplicationContext(), R.raw.msal_config_webview);
        } else {
            mApplication = new PublicClientApplication(this.getApplicationContext(), R.raw.msal_config);
        }

        if(environment == Constants.AzureActiveDirectoryEnvironment.PREPRODUCTION){
            mApplication = new PublicClientApplication(this.getApplicationContext(), R.raw.msal_ppe_config);
        }
    }

    final String getAuthority(Constants.AuthorityType authorityTypeType) {
        switch (authorityTypeType) {
            case AAD_COMMON:
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

    void setUser(final IAccount user) {
        mSelectedAccount = user;
    }

    private void callAcquireToken(final String[] scopes,
                                  final UiBehavior uiBehavior,
                                  final String loginHint,
                                  final List<Pair<String, String>> extraQueryParam,
                                  final String[] extraScope) {
        // The sample app is having the PII enable setting on the MainActivity. Ideally, app should decide to enable Pii or not,
        // if it's enabled, it should be  the setting when the application is onCreate.
        if (mEnablePiiLogging) {
            Logger.getInstance().setEnablePII(true);
        } else {
            Logger.getInstance().setEnablePII(false);
        }

        try {
            mApplication.acquireToken(
                    this,
                    scopes,
                    loginHint,
                    uiBehavior,
                    extraQueryParam,
                    extraScope,
                    null,
                    getAuthenticationCallback()
            );
        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage());
        }
    }

    private void callAcquireTokenSilent(final String[] scopes,
                                        final IAccount account,
                                        boolean forceRefresh) {
        mApplication.acquireTokenSilentAsync(
                scopes,
                account,
                null,
                forceRefresh,
                getAuthenticationCallback()
        );
    }

    private AuthenticationCallback getAuthenticationCallback() {
        return new AuthenticationCallback() {

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                mAuthResult = authenticationResult;
                onNavigationItemSelected(getNavigationView().getMenu().getItem(1));
                mSelectedAccount = null;
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
                } else if (exception instanceof MsalArgumentException) {
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
