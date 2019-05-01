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
import android.support.v4.app.Fragment;
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

import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.UiBehavior;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.ui.browser.Browser;
import com.microsoft.identity.common.internal.ui.browser.BrowserSelector;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.identity.client.testapp.R.id.enablePII;


/**
 * acquireToken Fragment, contains the flow for acquireToken interactively, acquireTokenSilent, getUsers, removeUser.
 */
public class AcquireTokenFragment extends Fragment {
    private Spinner mAuthority;
    private EditText mLoginhint;
    private Spinner mUiBehavior;
    private Spinner mUserAgent;
    private EditText mScope;
    private EditText mExtraScope;
    private Switch mEnablePII;
    private Switch mForceRefresh;
    private Button mGetUsers;
    private Button mClearCache;
    private Button mAcquireToken;
    private Button mAcquireTokenSilent;
    private TextView mDefaultBrowser;
    private Spinner mSelectAccount;
    private Spinner mAADEnvironments;
    private TextView mPublicApplicationMode;

    private OnFragmentInteractionListener mOnFragmentInteractionListener;

    public AcquireTokenFragment() {
        // left empty
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_acquire, container, false);

        mAuthority = view.findViewById(R.id.authorityType);
        mLoginhint = view.findViewById(R.id.loginHint);
        mUiBehavior = view.findViewById(R.id.uiBehavior);
        mUserAgent = view.findViewById(R.id.auth_user_agent);
        mScope = view.findViewById(R.id.scope);
        mExtraScope = view.findViewById(R.id.extraScope);
        mEnablePII = view.findViewById(enablePII);
        mForceRefresh = view.findViewById(R.id.forceRefresh);
        mDefaultBrowser = view.findViewById(R.id.default_browser);
        mSelectAccount = view.findViewById(R.id.select_user);
        mGetUsers = view.findViewById(R.id.btn_getUsers);
        mClearCache = view.findViewById(R.id.btn_clearCache);
        mAcquireToken = view.findViewById(R.id.btn_acquiretoken);
        mAcquireTokenSilent = view.findViewById(R.id.btn_acquiretokensilent);
        mAADEnvironments = view.findViewById(R.id.environment);
        mPublicApplicationMode = view.findViewById(R.id.public_application_mode);

        bindSpinnerChoice(mAuthority, Constants.AuthorityType.class);
        bindSpinnerChoice(mUiBehavior, UiBehavior.class);
        bindSpinnerChoice(mUserAgent, Constants.UserAgent.class);
        bindSpinnerChoice(mAADEnvironments, Constants.AzureActiveDirectoryEnvironment.class);

        mAcquireToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnFragmentInteractionListener.onAcquireTokenClicked(getCurrentRequestOptions());
            }
        });

        mAcquireTokenSilent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectAccount.getSelectedItem() != null) {
                    mLoginhint.setText(mSelectAccount.getSelectedItem().toString());
                }
                mOnFragmentInteractionListener.onAcquireTokenSilentClicked(getCurrentRequestOptions());
            }
        });

        mSelectAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getSelectedItem() != null) {
                    mLoginhint.setText(parent.getSelectedItem().toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mGetUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnFragmentInteractionListener.onGetUser();
            }
        });

        mClearCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String accountToRemove = null;
                if (mSelectAccount.getSelectedItem() != null) {
                    accountToRemove = mSelectAccount.getSelectedItem().toString();
                }

                mOnFragmentInteractionListener.onRemoveUserClicked(accountToRemove);
            }
        });

        return view;
    }

    private void setCurrentDefaultBrowserValue() {
        try {
            if (getActivity() != null) {
                Browser browser = BrowserSelector.select(getActivity().getApplicationContext());
                mDefaultBrowser.setText(browser.getPackageName());
            }
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mOnFragmentInteractionListener != null) {
            MsalWrapper.sharedInstance().registerPostAccountLoadedJob("bindSelectAccountSpinner",
                new MsalWrapper.IPostAccountLoaded() {
                    @Override
                    public void onLoaded(List<IAccount> loadedAccount) {
                        mOnFragmentInteractionListener.bindSelectAccountSpinner(mSelectAccount, loadedAccount);
                        mPublicApplicationMode.setText(MsalWrapper.sharedInstance().getPublicApplicationMode());
                    }
                });
        }
        if (mSelectAccount.getSelectedItem() != null) {
            mLoginhint.setText(mSelectAccount.getSelectedItem().toString());
        }
        setCurrentDefaultBrowserValue();
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        if (context instanceof OnFragmentInteractionListener) {
            mOnFragmentInteractionListener = (OnFragmentInteractionListener) context;
        } else {
            throw new IllegalStateException("OnFragmentInteractionListener is not implemented");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mOnFragmentInteractionListener = null;
    }

    void bindSpinnerChoice(final Spinner spinner, final Class<? extends Enum> spinnerChoiceClass) {
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item,
                new ArrayList<String>() {{
                    for (Enum choice : spinnerChoiceClass.getEnumConstants())
                        add(choice.name());
                }}
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    RequestOptions getCurrentRequestOptions() {
        final Constants.AuthorityType authorityType = Constants.AuthorityType.valueOf(mAuthority.getSelectedItem().toString());
        final Constants.AzureActiveDirectoryEnvironment environment = Constants.AzureActiveDirectoryEnvironment.valueOf(mAADEnvironments.getSelectedItem().toString());
        final String loginHint = mLoginhint.getText().toString();
        final UiBehavior uiBehavior = UiBehavior.valueOf(mUiBehavior.getSelectedItem().toString());
        final Constants.UserAgent userAgent = Constants.UserAgent.valueOf(mUserAgent.getSelectedItem().toString());
        final String scopes = mScope.getText().toString();
        final String extraScopesToConsent = mExtraScope.getText().toString();
        final boolean enablePII = mEnablePII.isChecked();
        final boolean forceRefresh = mForceRefresh.isChecked();
        return RequestOptions.create(authorityType, environment, loginHint, uiBehavior, userAgent, scopes, extraScopesToConsent, enablePII, forceRefresh);
    }

    static class RequestOptions {
        final Constants.AuthorityType mAuthorityType;
        final Constants.AzureActiveDirectoryEnvironment mEnvironment;
        final String mLoginHint;
        final UiBehavior mUiBehavior;
        final Constants.UserAgent mUserAgent;
        final String mScope;
        final String mExtraScope;
        final boolean mEnablePII;
        final boolean mForceRefresh;

        RequestOptions(final Constants.AuthorityType authorityType,
                       final Constants.AzureActiveDirectoryEnvironment environment,
                       final String loginHint, final UiBehavior uiBehavior,
                       final Constants.UserAgent userAgent, final String scope,
                       final String extraScope, final boolean enablePII, final boolean forceRefresh) {
            mAuthorityType = authorityType;
            mEnvironment = environment;
            mLoginHint = loginHint;
            mUiBehavior = uiBehavior;
            mUserAgent = userAgent;
            mScope = scope;
            mExtraScope = extraScope;
            mEnablePII = enablePII;
            mForceRefresh = forceRefresh;
        }

        static RequestOptions create(final Constants.AuthorityType authority, final Constants.AzureActiveDirectoryEnvironment environment, final String loginHint,
                                     final UiBehavior uiBehavior,
                                     final Constants.UserAgent userAgent, final String scope, final String extraScope, final boolean enablePII, final boolean forceRefresh) {
            return new RequestOptions(authority, environment, loginHint, uiBehavior, userAgent, scope, extraScope, enablePII, forceRefresh);
        }

        Constants.AuthorityType getAuthorityType() {
            return mAuthorityType;
        }

        Constants.AzureActiveDirectoryEnvironment getEnvironment() { return mEnvironment;}

        String getLoginHint() {
            return mLoginHint;
        }

        UiBehavior getUiBehavior() {
            return mUiBehavior;
        }

        String getScopes() {
            return mScope;
        }

        String getExtraScopesToConsent() {
            return mExtraScope;
        }

        boolean enablePiiLogging() {
            return mEnablePII;
        }

        boolean forceRefresh() {
            return mForceRefresh;
        }

        Constants.UserAgent getUserAgent() {
            return mUserAgent;
        }
    }

    public interface OnFragmentInteractionListener {
        void onGetUser();

        void onRemoveUserClicked(String username);

        void onAcquireTokenClicked(final RequestOptions requestOptions);

        void onAcquireTokenSilentClicked(final RequestOptions requestOptions);

        void bindSelectAccountSpinner(Spinner selectAccount, List<IAccount> accounts);
    }
}
