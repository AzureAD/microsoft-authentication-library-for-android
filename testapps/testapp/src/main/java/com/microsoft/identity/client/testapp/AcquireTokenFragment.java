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

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.microsoft.identity.client.HttpMethod;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.PublicClientApplication;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.identity.client.testapp.R.id.enablePII;

/**
 * acquireToken Fragment, contains the flow for acquireToken interactively, acquireTokenSilent, getUsers, removeUser.
 */
public class AcquireTokenFragment extends Fragment {
    public static final String NONE_NULL = "NONE (NULL)";
    private EditText mAuthority;
    private EditText mLoginhint;
    private Spinner mPrompt;
    private EditText mScope;
    private EditText mExtraScope;
    private EditText mClaims;
    private Switch mEnablePII;
    private Switch mForceRefresh;
    private Button mGetUsers;
    private Button mClearCache;
    private Button mAcquireToken;
    private Button mAcquireTokenSilent;
    private Button mAcquireTokenWithResource;
    private Button mAcquireTokenSilentWithResource;
    private Button mAcquireTokenWithDeviceCodeFlow;
    private Button mBrokerHelper;
    private Button mGenerateSHR;
    private Spinner mSelectAccount;
    private Spinner mConfigFileSpinner;
    private Spinner mAuthScheme;
    private TextView mPublicApplicationMode;
    private TextView mDefaultBrowser;
    private TextView mStatus;
    private Button mStatusCopyBtn;
    private Spinner mPopHttpMethod;
    private EditText mPopResourceUrl;
    private EditText mPopClientClaims;

    private LinearLayout mPopSection;
    private LinearLayout mLoginHintSection;

    private OnFragmentInteractionListener mOnFragmentInteractionListener;
    private MsalWrapper mMsalWrapper;
    private List<IAccount> mLoadedAccounts = new ArrayList<>();

    public AcquireTokenFragment() {
        // left empty
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_acquire, container, false);

        mAuthority = view.findViewById(R.id.authority);
        mLoginhint = view.findViewById(R.id.loginHint);
        mPrompt = view.findViewById(R.id.promptBehavior);
        mScope = view.findViewById(R.id.scope);
        mExtraScope = view.findViewById(R.id.extraScope);
        mClaims = view.findViewById(R.id.claims);
        mEnablePII = view.findViewById(enablePII);
        mForceRefresh = view.findViewById(R.id.forceRefresh);
        mSelectAccount = view.findViewById(R.id.select_user);
        mGetUsers = view.findViewById(R.id.btn_getUsers);
        mClearCache = view.findViewById(R.id.btn_clearCache);
        mAcquireToken = view.findViewById(R.id.btn_acquiretoken);
        mAcquireTokenSilent = view.findViewById(R.id.btn_acquiretokensilent);
        mAcquireTokenWithResource = view.findViewById(R.id.btn_acquiretokenWithResource);
        mAcquireTokenSilentWithResource = view.findViewById(R.id.btn_acquiretokensilentWithResource);
        mAcquireTokenWithDeviceCodeFlow = view.findViewById(R.id.btn_acquiretokenWithDeviceCodeFlow);
        mBrokerHelper = view.findViewById(R.id.btnBrokerHelper);
        mGenerateSHR = view.findViewById(R.id.btn_generate_shr);
        mConfigFileSpinner = view.findViewById(R.id.configFile);
        mAuthScheme = view.findViewById(R.id.authentication_scheme);
        mPublicApplicationMode = view.findViewById(R.id.public_application_mode);
        mDefaultBrowser = view.findViewById(R.id.default_browser);
        mStatus = view.findViewById(R.id.status);
        mStatusCopyBtn = view.findViewById(R.id.btn_statusCopy);
        mPopHttpMethod = view.findViewById(R.id.pop_http_method);
        mPopResourceUrl = view.findViewById(R.id.pop_resource_url);
        mPopClientClaims = view.findViewById(R.id.pop_client_claims);

        mPopSection = view.findViewById(R.id.pop_section);
        mLoginHintSection = view.findViewById(R.id.login_hint_section);

        bindSelectAccountSpinner(mSelectAccount, null);
        mSelectAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateUiBasedOnCurrentAccount();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        bindSpinnerChoice(mPrompt, Prompt.class);
        bindSpinnerChoice(mAuthScheme, Constants.AuthScheme.class);
        mAuthScheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateUiBasedOnAuthScheme();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        bindSpinnerChoice(mPopHttpMethod, HttpMethod.class);

        bindSpinnerChoice(mConfigFileSpinner, Constants.ConfigFile.class);
        mConfigFileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadMsalApplicationFromRequestParameters(getCurrentRequestOptions());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mStatusCopyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                final ClipData clip = ClipData.newPlainText("MSAL Test App", mStatus.getText());
                clipboard.setPrimaryClip(clip);

                Toast.makeText(getContext(), "Text copied to clipboard.", Toast.LENGTH_LONG).show();
            }
        });

        final INotifyOperationResultCallback acquireTokenCallback = new INotifyOperationResultCallback<IAuthenticationResult>() {
            @Override
            public void onSuccess(IAuthenticationResult result) {
                mOnFragmentInteractionListener.onGetAuthResult(result);
            }

            @Override
            public void showMessage(String message) {
                AcquireTokenFragment.this.showMessage(message);
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

        mAcquireTokenWithDeviceCodeFlow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMsalWrapper.acquireTokenWithDeviceCodeFlow(getCurrentRequestOptions(), acquireTokenCallback);
            }
        });


        final Activity activity = this.getActivity();
        mBrokerHelper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublicClientApplication.showExpectedMsalRedirectUriInfo(activity);
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
                                loadAccounts();
                            }

                            @Override
                            public void showMessage(String message) {
                                AcquireTokenFragment.this.showMessage(message);
                            }
                        });
            }
        });

        final INotifyOperationResultCallback<String> generateShrCallback =
                new INotifyOperationResultCallback<String>() {

            @Override
            public void onSuccess(String result) {
                mOnFragmentInteractionListener.onGetStringResult(result);
            }

            @Override
            public void showMessage(String message) {
                AcquireTokenFragment.this.showMessage(message);
            }
        };

        mGenerateSHR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMsalWrapper.generateSignedHttpRequest(getCurrentRequestOptions(), generateShrCallback);
            }
        });


        loadMsalApplicationFromRequestParameters(getCurrentRequestOptions());
        return view;
    }

    private void updateUiBasedOnAuthScheme() {
        final Constants.AuthScheme authScheme = Constants.AuthScheme.valueOf(mAuthScheme.getSelectedItem().toString());
        if (authScheme == Constants.AuthScheme.POP) {
            mPopSection.setVisibility(View.VISIBLE);
            mGenerateSHR.setVisibility(View.VISIBLE);
        } else {
            mPopSection.setVisibility(View.GONE);
            mGenerateSHR.setVisibility(View.GONE);
        }
    }

    // If an account is selected.
    //  - Hide loginhint section.
    //  - Set hint in mAuthority.
    private void updateUiBasedOnCurrentAccount() {
        final IAccount account = getAccountFromSpinner();
        if (account == null) {
            mLoginhint.setCursorVisible(true);
            mLoginhint.setEnabled(true);
            mLoginHintSection.setVisibility(View.VISIBLE);
            mAuthority.setHint("");
        } else {
            mLoginhint.setText("");
            mLoginhint.setCursorVisible(false);
            mLoginhint.setEnabled(false);
            mLoginHintSection.setVisibility(View.GONE);
            mAuthority.setHint("Default: account's authority");
        }
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
                    AcquireTokenFragment.this.showMessage(message);
                }
            });
        }
    }

    private void refreshUI() {
        bindSelectAccountSpinner(mSelectAccount, mLoadedAccounts);
        updateUiBasedOnAuthScheme();
        mPublicApplicationMode.setText(mMsalWrapper.getMode());
        mDefaultBrowser.setText(mMsalWrapper.getDefaultBrowser());
    }

    private IAccount getAccountFromSpinner() {
        if (mLoadedAccounts == null || mLoadedAccounts.isEmpty()) {
            return null;
        }

        int selectedAccountPosition = mSelectAccount.getSelectedItemPosition();

        // This means that there is no selected account.
        if (selectedAccountPosition == AdapterView.INVALID_POSITION) {
            return null;
        }

        // We're using the last tile to display "-- No Account Selected --"
        if (selectedAccountPosition == mLoadedAccounts.size()) {
            return null;
        }

        return mLoadedAccounts.get(selectedAccountPosition);
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
                    add("-- No Account Selected --");
                }}
        );
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(userAdapter);
        spinner.setSelection(0, false);
    }

    private void bindSpinnerChoice(final Spinner spinner, final Class<? extends Enum> spinnerChoiceClass) {
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<String>() {{
                    for (Enum choice : spinnerChoiceClass.getEnumConstants()) {
                        add(choice.name());
                    }

                    if (spinnerChoiceClass.isAssignableFrom(HttpMethod.class)) {
                        // Add 1 more option for "none"
                        add(NONE_NULL);
                    }
                }}
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0, false);
    }

    private RequestOptions getCurrentRequestOptions() {
        final Constants.ConfigFile configFile = Constants.ConfigFile.valueOf(mConfigFileSpinner.getSelectedItem().toString());
        final String loginHint = mLoginhint.getText().toString();
        final IAccount account = getAccountFromSpinner();
        final Prompt promptBehavior = Prompt.valueOf(mPrompt.getSelectedItem().toString());
        final String scopes = mScope.getText().toString();
        final String extraScopesToConsent = mExtraScope.getText().toString();
        final String claims = mClaims.getText().toString();
        final boolean enablePII = mEnablePII.isChecked();
        final boolean forceRefresh = mForceRefresh.isChecked();
        final String authority = mAuthority.getText().toString();
        final Constants.AuthScheme authScheme = Constants.AuthScheme.valueOf(mAuthScheme.getSelectedItem().toString());
        final String httpMethodTextFromSpinner = mPopHttpMethod.getSelectedItem().toString();
        final HttpMethod popHttpMethod = httpMethodTextFromSpinner.equals(NONE_NULL)
                ? null // None specified
                : HttpMethod.valueOf(httpMethodTextFromSpinner);
        final String popResourceUrl = mPopResourceUrl.getText().toString();
        final String popClientClaimsTxt = mPopClientClaims.getText().toString();

        return new RequestOptions(
                configFile,
                loginHint,
                account,
                promptBehavior,
                scopes,
                extraScopesToConsent,
                claims,
                enablePII,
                forceRefresh,
                authority,
                authScheme,
                popHttpMethod,
                popResourceUrl,
                popClientClaimsTxt
        );
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

        MsalWrapper.create(getContext(),
                Constants.getResourceIdFromConfigFile(requestOptions.getConfigFile()),
                new INotifyOperationResultCallback<MsalWrapper>() {
                    @Override
                    public void onSuccess(MsalWrapper result) {
                        mMsalWrapper = result;
                        loadAccounts();
                    }

                    @Override
                    public void showMessage(String message) {
                        AcquireTokenFragment.this.showMessage(message);
                    }
                });
    }

    private void showMessage(final String msg) {
        new Handler(getActivity().getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                mStatus.setText(msg);
            }
        });
    }

    public interface OnFragmentInteractionListener {
        void onGetAuthResult(IAuthenticationResult result);

        void onGetStringResult(String valueToDisplay);

        void onGetUsers();
    }
}
