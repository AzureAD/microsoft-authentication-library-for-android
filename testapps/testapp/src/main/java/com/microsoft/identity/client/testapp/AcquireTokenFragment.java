//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.
//
//   Permission is hereby granted, free of charge, to any person obtaining a copy
//   of this software and associated documentation files(the "Software"), to deal
//   in the Software without restriction, including without limitation the rights
//   to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//   copies of the Software, and to permit persons to whom the Software is
//   furnished to do so, subject to the following conditions :
//
//   The above copyright notice and this permission notice shall be included in
//   all copies or substantial portions of the Software.
//
//   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//   THE SOFTWARE.

package com.microsoft.identity.client.testapp;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.client.Prompt;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.identity.client.testapp.R.id.enablePII;

/**
 * acquireToken Fragment, contains the flow for acquireToken interactively, acquireTokenSilent, getUsers, removeUser.
 */
public class AcquireTokenFragment extends Fragment {
    private EditText mLoginhint;
    private Spinner mPrompt;
    private Spinner mUserAgent;
    private EditText mScope;
    private EditText mExtraScope;
    private Switch mEnablePII;
    private Switch mForceRefresh;
    private Button mGetUsers;
    private Button mClearCache;
    private Button mAcquireToken;
    private Button mAcquireTokenSilent;
    private Button mAcquireTokenWithResource;
    private Button mAcquireTokenSilentWithResource;
    private Spinner mSelectAccount;
    private Spinner mAADEnvironments;
    private TextView mPublicApplicationMode;

    private OnFragmentInteractionListener mOnFragmentInteractionListener;
    private MsalWrapper mMsalWrapper;
    private List<IAccount> mLoadedAccounts = new ArrayList<>();

    public AcquireTokenFragment() {
        // left empty
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_acquire, container, false);

        mLoginhint = view.findViewById(R.id.loginHint);
        mPrompt = view.findViewById(R.id.uiBehavior);
        mUserAgent = view.findViewById(R.id.auth_user_agent);
        mScope = view.findViewById(R.id.scope);
        mExtraScope = view.findViewById(R.id.extraScope);
        mEnablePII = view.findViewById(enablePII);
        mForceRefresh = view.findViewById(R.id.forceRefresh);
        mSelectAccount = view.findViewById(R.id.select_user);
        mGetUsers = view.findViewById(R.id.btn_getUsers);
        mClearCache = view.findViewById(R.id.btn_clearCache);
        mAcquireToken = view.findViewById(R.id.btn_acquiretoken);
        mAcquireTokenSilent = view.findViewById(R.id.btn_acquiretokensilent);
        mAcquireTokenWithResource = view.findViewById(R.id.btn_acquiretokenWithResource);
        mAcquireTokenSilentWithResource = view.findViewById(R.id.btn_acquiretokensilentWithResource);
        mAADEnvironments = view.findViewById(R.id.environment);
        mPublicApplicationMode = view.findViewById(R.id.public_application_mode);

        bindSelectAccountSpinner(mSelectAccount, null);
        bindSpinnerChoice(mPrompt, Prompt.class);

        final AdapterView.OnItemSelectedListener onReloadPcaItemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadMsalApplicationFromRequestParameters(getCurrentRequestOptions());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        bindSpinnerChoice(mUserAgent, Constants.UserAgent.class);
        mUserAgent.setOnItemSelectedListener(onReloadPcaItemSelectedListener);

        bindSpinnerChoice(mAADEnvironments, Constants.AzureActiveDirectoryEnvironment.class);
        mAADEnvironments.setOnItemSelectedListener(onReloadPcaItemSelectedListener);

        final INotifyOperationResultCallback acquireTokenCallback = new INotifyOperationResultCallback<IAuthenticationResult>() {
            @Override
            public void onSuccess(IAuthenticationResult result) {
                mOnFragmentInteractionListener.onGetAuthResult(result);
            }

            @Override
            public void showMessage(String message) {
                showMessageWithToast(message);
            }
        };

        mAcquireToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMsalWrapper.acquireToken(getActivity(), getCurrentRequestOptions(), acquireTokenCallback);
            }
        });

        mAcquireTokenSilent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMsalWrapper.acquireTokenSilent(getCurrentRequestOptions(), acquireTokenCallback);
            }
        });

        mAcquireTokenWithResource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMsalWrapper.acquireTokenWithResource(getActivity(), getCurrentRequestOptions(), acquireTokenCallback);
            }
        });

        mAcquireTokenSilentWithResource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMsalWrapper.acquireTokenSilentWithResource(getCurrentRequestOptions(), acquireTokenCallback);
            }
        });

        mGetUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnFragmentInteractionListener.onGetUsers();
            }
        });

        mClearCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMsalWrapper.removeAccount(
                        getAccountFromSpinner(),
                        new INotifyOperationResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                            }

                            @Override
                            public void showMessage(String message) {
                                showMessageWithToast(message);
                            }
                        });
            }
        });


        loadMsalApplicationFromRequestParameters(getCurrentRequestOptions());
        return view;
    }

    private IAccount getAccountFromSpinner() {
        if (mLoadedAccounts == null || mLoadedAccounts.isEmpty()) {
            return null;
        }

        int selectedAccount = mSelectAccount.getSelectedItemPosition();

        // This means that there is no selected account. Just pick the first one.
        if (selectedAccount == AdapterView.INVALID_POSITION){
            selectedAccount = 0;
        }

        return mLoadedAccounts.get(selectedAccount);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnFragmentInteractionListener) {
            mOnFragmentInteractionListener = (OnFragmentInteractionListener) context;
        } else {
            throw new IllegalStateException("OnFragmentInteractionListener is not implemented");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAccounts();
    }

    private void loadAccounts() {
        if (mMsalWrapper != null) {
            mMsalWrapper.loadAccounts(new INotifyOperationResultCallback<List<IAccount>>() {
                @Override
                public void onSuccess(List<IAccount> result) {
                    mLoadedAccounts = result;
                    refreshUI();
                }

                @Override
                public void showMessage(String message) {
                    showMessageWithToast(message);
                }
            });
        }
    }

    private void refreshUI() {
        bindSelectAccountSpinner(mSelectAccount, mLoadedAccounts);
        mPublicApplicationMode.setText(mMsalWrapper.getMode());
    }

    private void bindSelectAccountSpinner(final Spinner spinner,
                                          final List<IAccount> accounts) {
        final ArrayAdapter<String> userAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<String>() {{
                    if (accounts != null) {
                        for (IAccount account : accounts) {
                            add(account.getUsername());
                        }
                    }
                }}
        );
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(userAdapter);
        spinner.setSelection(0, false);
    }

    private void bindSpinnerChoice(final Spinner spinner, final Class<? extends Enum> spinnerChoiceClass) {
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item,
                new ArrayList<String>() {{
                    for (Enum choice : spinnerChoiceClass.getEnumConstants())
                        add(choice.name());
                }}
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0, false);
    }

    private RequestOptions getCurrentRequestOptions() {
        final Constants.AzureActiveDirectoryEnvironment environment = Constants.AzureActiveDirectoryEnvironment.valueOf(mAADEnvironments.getSelectedItem().toString());
        final String loginHint = mLoginhint.getText().toString();
        final IAccount account = getAccountFromSpinner();
        final Prompt uiBehavior = Prompt.valueOf(mPrompt.getSelectedItem().toString());
        final Constants.UserAgent userAgent = Constants.UserAgent.valueOf(mUserAgent.getSelectedItem().toString());
        final String scopes = mScope.getText().toString();
        final String extraScopesToConsent = mExtraScope.getText().toString();
        final boolean enablePII = mEnablePII.isChecked();
        final boolean forceRefresh = mForceRefresh.isChecked();
        final String authority = null;  // TODO
        final boolean usePop = false; // TODO
        return new RequestOptions(environment, loginHint, account, uiBehavior, userAgent, scopes, extraScopesToConsent, enablePII, forceRefresh, authority, usePop);
    }

    private void loadMsalApplicationFromRequestParameters(final RequestOptions requestOptions) {
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

        MsalWrapper.create(getContext(),
                configFileResourceId,
                new INotifyOperationResultCallback<MsalWrapper>() {
                    @Override
                    public void onSuccess(MsalWrapper result) {
                        mMsalWrapper = result;
                        loadAccounts();
                    }

                    @Override
                    public void showMessage(String message) {
                        showMessageWithToast(message);
                    }
                });
    }

    private void showMessageWithToast(final String msg) {
        new Handler(getActivity().getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    public interface OnFragmentInteractionListener {
        void onGetAuthResult(IAuthenticationResult result);

        void onGetUsers();
    }
}
