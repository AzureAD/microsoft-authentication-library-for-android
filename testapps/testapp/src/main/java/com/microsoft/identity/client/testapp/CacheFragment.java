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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Fragment to display TokenCache contents.
 */
public class CacheFragment extends Fragment {

    static final String ARG_LIST_CONTENTS = "list_contents";

    private ListView mLvCacheItems;
    private ProgressBar mProgressBar;
    private OnFragmentInteractionListener mListener;

    public CacheFragment() {
        // Required empty public constructor
    }

    private static String getUUID() {
        return UUID.randomUUID().toString();
    }

    static List<TokenListElement> TEST_LIST_ELEMENTS = new ArrayList<TokenListElement>() {{
        add(new TokenListElement(getUUID(), getUUID(), getUUID(), getUUID(), getUUID()));
        add(new TokenListElement(getUUID(), getUUID(), getUUID(), getUUID()));
        add(new TokenListElement(getUUID(), getUUID(), getUUID(), getUUID(), getUUID()));
        add(new TokenListElement(getUUID(), getUUID(), getUUID(), getUUID()));
    }};

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_cache, container, false);

        mLvCacheItems = (ListView) view.findViewById(R.id.lv_cache);
        final Bundle args = getArguments();
        final List<TokenListElement> elements = (List<TokenListElement>) args.getSerializable(ARG_LIST_CONTENTS);
        mLvCacheItems.setAdapter(createAdapter(elements));

        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

        return view;
    }

    void setLoading() {
        mLvCacheItems.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    void reload(final List<TokenListElement> tokenListElements) {
        mProgressBar.setVisibility(View.GONE);
        mLvCacheItems.setVisibility(View.VISIBLE);
        mLvCacheItems.setAdapter(createAdapter(tokenListElements));
    }

    private BaseAdapter createAdapter(final List<TokenListElement> tokenListElements) {
        return new BaseAdapter() {
            @Override
            public int getCount() {
                return tokenListElements.size();
            }

            @Override
            public TokenListElement getItem(final int i) {
                return tokenListElements.get(i);
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override
            public View getView(final int position, View convertView, final ViewGroup viewGroup) {
                if (null == convertView) {
                    convertView = LayoutInflater.from(
                            getActivity()
                    ).inflate(R.layout.cache_list_item, viewGroup, false);
                }

                final TokenListElement element = tokenListElements.get(position);
                final boolean isRt = tokenListElements.get(position).getElementType() == TokenListElement.ElementType.RT;

                // Client id
                final View clientIdLayout = convertView.findViewById(R.id.ll_row_clientid);
                final TextView valueClientId = (TextView) clientIdLayout.findViewById(R.id.value_clientid);
                valueClientId.setText(element.getClientId());

                // User identifier
                final View userIdentifierLayout = convertView.findViewById(R.id.ll_row_user_identifier);
                final TextView valueUserIdentifier = (TextView) userIdentifierLayout.findViewById(R.id.value_user_identifier);
                valueUserIdentifier.setText(element.getUserIdentifier());

                // Displayable id
                final View displayableIdLayout = convertView.findViewById(R.id.ll_row_displayable_id);
                final TextView valueDisplayableId = (TextView) displayableIdLayout.findViewById(R.id.value_displayable_id);
                valueDisplayableId.setText(element.getDisplayableId());

                // Scopes
                final View scopesLayout = convertView.findViewById(R.id.ll_row_scopes);
                scopesLayout.setVisibility(isRt ? View.GONE : View.VISIBLE);
                final TextView valueScopes = (TextView) convertView.findViewById(R.id.values_scopes);
                if (!isRt) {
                    valueScopes.setText(element.getScopes());
                }

                // Expires on
                final View expiresOnLayout = convertView.findViewById(R.id.ll_row_expires_on);
                expiresOnLayout.setVisibility(isRt ? View.GONE : View.VISIBLE);
                final TextView valueExpiresOn = (TextView) convertView.findViewById(R.id.value_expires_on);
                if (!isRt) {
                    valueExpiresOn.setText(element.getExpiresOn());
                }

                // Host
                final View hostLayout = convertView.findViewById(R.id.ll_row_host);
                hostLayout.setVisibility(isRt ? View.VISIBLE : View.GONE);
                final TextView valueHost = (TextView) convertView.findViewById(R.id.value_host);
                if (isRt) {
                    // set it
                    valueHost.setText(element.getHost());
                }

                // Designation
                final TextView tokenDesignation = (TextView) convertView.findViewById(R.id.tv_atrt);
                tokenDesignation.setText(element.getElementType().mDisplayValue);

                // Delete
                final Button deleteBtn = (Button) convertView.findViewById(R.id.btn_delete);
                deleteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        if (null != mListener) {
                            mListener.onDeleteToken(position, CacheFragment.this);
                        }
                    }
                });

                return convertView;
            }
        };
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    interface OnFragmentInteractionListener {
        void onDeleteToken(final int position, CacheFragment cacheFragment);
    }
}
