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

package com.microsoft.identity.client.sample;

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
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hugo.weaving.DebugLog;


public class AcquireTokenFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    static class Options {

        static <T extends Enum<T>> T getChoice(Class<T> clazz, String choiceText) {
            return Enum.valueOf(clazz, choiceText);
        }


        enum IdType {
            OPTIONAL(R.string.id_optional),
            REQUIRED(R.string.id_required),
            UNIQUE(R.string.id_unique);

            final String mDisplayText;

            @DebugLog
            IdType(int textId) {
                mDisplayText = getString(textId);
                System.out.println(mDisplayText);
            }
        }

        enum Prompt {
            ALWAYS(R.string.prompt_always),
            AUTO(R.string.prompt_auto);

            final String mDisplayText;

            Prompt(int textId) {
                mDisplayText = getString(textId);
            }
        }

    }

    EditText mEtUserId;
    Spinner mSpIdType;
    Spinner mSpPrompt;
    Spinner mSpProfile;
    @BindView(R.id.btn_clearcookies)
    Button mBtnClearCookies;

    @BindView(R.id.btn_clearcache)
    Button mBtnClearCache;

    @BindView(R.id.txt_output)
    TextView mTxtOutput;

    @BindView(R.id.btn_acquiretoken)
    Button mBtnAcquireToken;

    @BindView(R.id.btn_acquiretokensilent)
    Button getmBtnAcquireTokenToken;

    List<Spinner> mSpinners = new ArrayList<>();


    public AcquireTokenFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_acquire, container, false);
        initializeChoices();
        populateSpinnerList();
        return view;
    }

    @Override
    public void onAttach(Context context) {
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

    private RequestOptions getCurrentRequestOptions() {
        return RequestOptions.newInstance(
                mEtUserId.getText().toString(),
                mSpIdType.getSelectedItem().toString(),
                mSpPrompt.getSelectedItem().toString(),
        );
    }
    
    @OnClick(R.id.btn_clearcookies)
    void onClearCookiesClicked() {
        mListener.onClearCookiesClicked();
    }

    @DebugLog
    @OnClick(R.id.btn_clearcache)
    void onClearCacheClicked() {
        mListener.onClearCacheClicked();
    }

    @DebugLog
    @OnClick(R.id.btn_acquiretoken)
    void onAcquireTokenClicked() {
        mListener.onAcquireTokenClicked(getCurrentRequestOptions());
    }

    @DebugLog
    @OnClick(R.id.btn_acquiretokensilent)
    void onAcquireTokenSilentClicked() {
        mListener.onAcquireTokenSilentClicked(getCurrentRequestOptions());
    }

    @DebugLog
    private void populateSpinnerList() {
        mSpinners.add(mSpIdType);
        mSpinners.add(mSpPrompt);
        mSpinners.add(mSpProfile);
        mSpinners.add(mSpWebView);
        mSpinners.add(mSpFullscreen);
        mSpinners.add(mSpBroker);
        mSpinners.add(mSpValAuth);
    }

    @DebugLog
    private void initializeChoices() {
        bindChoices(mSpIdType, Options.IdType.class);
        bindChoices(mSpPrompt, Options.Prompt.class);
        bindChoices(mSpWebView, Options.WebView.class);
        bindChoices(mSpFullscreen, Options.FullScreen.class);
        bindChoices(mSpBroker, Options.Broker.class);
        bindChoices(mSpValAuth, Options.ValAuth.class);
    }

    @DebugLog
    private void bindChoices(Spinner spinner, final Class<? extends Enum> choiceClass) {
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<CharSequence>() {{
                    for (Enum choice : choiceClass.getEnumConstants())
                        add(choice.name());
                }}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    static class RequestOptions {

        final String mUserId;
        final Options.IdType mIdType;
        final Options.Prompt mPrompt;
        // Profile??
        final Options.WebView mWebView;
        final Options.FullScreen mFullScreen;
        final Options.Broker mBroker;
        final Options.ValAuth mValAuth;

        @DebugLog
        private RequestOptions(
                String userId,
                Options.IdType idType,
                Options.Prompt prompt,
                Options.WebView webView,
                Options.FullScreen fullScreen,
                Options.Broker broker,
                Options.ValAuth valAuth
        ) {
            mUserId = userId;
            mIdType = idType;
            mPrompt = prompt;
            mWebView = webView;
            mFullScreen = fullScreen;
            mBroker = broker;
            mValAuth = valAuth;
        }

        @DebugLog
        static RequestOptions newInstance(
                String userId,
                String idType,
                String prompt,
                String webView,
                String fullScreen,
                String broker,
                String valAuth
        ) {
            return new RequestOptions(
                    userId,
                    Options.getChoice(Options.IdType.class, idType),
                    Options.getChoice(Options.Prompt.class, prompt),
                    Options.getChoice(Options.WebView.class, webView),
                    Options.getChoice(Options.FullScreen.class, fullScreen),
                    Options.getChoice(Options.Broker.class, broker),
                    Options.getChoice(Options.ValAuth.class, valAuth)
            );
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("RequestOptions{");
            sb.append("mUserId='").append(mUserId).append('\'');
            sb.append(", mIdType=").append(mIdType);
            sb.append(", mPrompt=").append(mPrompt);
            sb.append(", mWebView=").append(mWebView);
            sb.append(", mFullScreen=").append(mFullScreen);
            sb.append(", mBroker=").append(mBroker);
            sb.append(", mValAuth=").append(mValAuth);
            sb.append('}');
            return sb.toString();
        }
    }

    public interface OnFragmentInteractionListener {
        void onClearCookiesClicked();

        void onClearCacheClicked();

        void onAcquireTokenClicked(RequestOptions options);

        void onAcquireTokenSilentClicked(RequestOptions options);
    }
}
