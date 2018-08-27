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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

import com.microsoft.identity.client.UiBehavior;

import java.util.ArrayList;

import static com.microsoft.identity.client.testapp.R.id.enablePII;


/**
 * acquireToken Fragment, contains the flow for acquireToken interactively, acquireTokenSilent, getUsers, removeUser.
 */
public class AcquireTokenFragment extends Fragment {
    private Spinner mAuthority;
    private EditText mLoginhint;
    private Spinner mUiBehavior;
    private Spinner mDataProfile;
    private EditText mScope;
    private EditText mExtraScope;
    private Switch mEnablePII;
    private Switch mForceRefresh;
    private Button mGetUsers;
    private Button mClearCache;
    private Button mAcquireToken;
    private Button mAcquireTokenSilent;

    private OnFragmentInteractionListener mOnFragmentInteractionListener;

    public AcquireTokenFragment() {
        // left empty
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_acquire, container, false);

        mAuthority = (Spinner) view.findViewById(R.id.authorityType);
        mLoginhint = (EditText) view.findViewById(R.id.loginHint);
        mUiBehavior = (Spinner) view.findViewById(R.id.uiBehavior);
        mDataProfile = (Spinner) view.findViewById(R.id.data_profile);
        mScope = (EditText) view.findViewById(R.id.scope);
        mExtraScope = (EditText) view.findViewById(R.id.extraScope);
        mEnablePII = (Switch) view.findViewById(enablePII);
        mForceRefresh = (Switch) view.findViewById(R.id.forceRefresh);

        mGetUsers = (Button) view.findViewById(R.id.btn_getUsers);
        mClearCache = (Button) view.findViewById(R.id.btn_clearCache);
        mAcquireToken = (Button) view.findViewById(R.id.btn_acquiretoken);
        mAcquireTokenSilent = (Button) view.findViewById(R.id.btn_acquiretokensilent);

        bindSpinnerChoice(mAuthority, Constants.AuthorityType.class);
        bindSpinnerChoice(mUiBehavior, UiBehavior.class);
        bindSpinnerChoice(mDataProfile, Constants.DataProfile.class);

        mAcquireToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnFragmentInteractionListener.onAcquireTokenClicked(getCurrentRequestOptions());
            }
        });

        mAcquireTokenSilent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnFragmentInteractionListener.onAcquireTokenSilentClicked(getCurrentRequestOptions());
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
                mOnFragmentInteractionListener.onRemoveUserClicked();
            }
        });

        return view;
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
        final Constants.AuthorityType authorityType = Constants.AuthorityType.valueOf(mAuthority.getSelectedItem().toString()) ;
        final String loginHint = mLoginhint.getText().toString();
        final UiBehavior uiBehavior = UiBehavior.valueOf(mUiBehavior.getSelectedItem().toString());
        final Constants.DataProfile dataProfile = Constants.DataProfile.valueOf(mDataProfile.getSelectedItem().toString());
        final String scopes = mScope.getText().toString();
        final String extraScopesToConsent = mExtraScope.getText().toString();
        final boolean enablePII = mEnablePII.isChecked();
        final boolean forceRefresh = mForceRefresh.isChecked();
        return RequestOptions.create(authorityType, loginHint, uiBehavior, dataProfile, scopes, extraScopesToConsent, enablePII, forceRefresh);
    }

    static class RequestOptions {
        final Constants.AuthorityType mAuthorityType;
        final String mLoginHint;
        final UiBehavior mUiBehavior;
        final Constants.DataProfile mDataProfile;
        final String mScope;
        final String mExtraScope;
        final boolean mEnablePII;
        final boolean mForceRefresh;

        RequestOptions(final Constants.AuthorityType authorityType, final String loginHint, final UiBehavior uiBehavior,
                       final Constants.DataProfile dataProfile, final String scope, final String extraScope, final boolean enablePII, final boolean forceRefresh) {
            mAuthorityType = authorityType;
            mLoginHint = loginHint;
            mUiBehavior = uiBehavior;
            mDataProfile = dataProfile;
            mScope = scope;
            mExtraScope = extraScope;
            mEnablePII = enablePII;
            mForceRefresh = forceRefresh;
        }

        static RequestOptions create(final Constants.AuthorityType authority, final String loginHint, final UiBehavior uiBehavior, final Constants.DataProfile dataProfile,
                                     final String scope, final String extraScope, final boolean enablePII, final boolean forceRefresh) {
            return new RequestOptions(authority, loginHint, uiBehavior, dataProfile, scope, extraScope, enablePII, forceRefresh);
        }

        Constants.AuthorityType getAuthorityType() {
            return mAuthorityType;
        }

        String getLoginHint() {
            return mLoginHint;
        }

        UiBehavior getUiBehavior() {
            return mUiBehavior;
        }

        Constants.DataProfile getDataProfile() {
            return mDataProfile;
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
    }

    public interface OnFragmentInteractionListener {
        void onGetUser();

        void onRemoveUserClicked();

        void onAcquireTokenClicked(final RequestOptions requestOptions);

        void onAcquireTokenSilentClicked(final RequestOptions requestOptions);
    }
}
