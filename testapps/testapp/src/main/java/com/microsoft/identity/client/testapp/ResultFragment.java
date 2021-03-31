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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * The Fragment used to display the result.
 */
public class ResultFragment extends Fragment {

    public static final String CORRELATION_ID = "correlation_id";
    static final String ACCESS_TOKEN = "access_token";
    static final String DISPLAYABLE = "displayable";
    static final String STRING_DATA_TO_DISPLAY = "string_data_to_display";
    static String previousAccessToken = "";

    private TextView mTextView;
    private Button mCopyAuthResultButton;

    public ResultFragment() {
        // left empty
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_result, container, false);
        mTextView = view.findViewById(R.id.txt_result);

        final Bundle bundle = getArguments();

        String output = "";

        if (isDisplayingAccessTokenResult(bundle)) {
            final String accessToken = (String) bundle.get(ACCESS_TOKEN);
            final String displayable = (String) bundle.get(DISPLAYABLE);

            // Only display this when the app has acquired an access token at least once in this session.
            if (previousAccessToken != null && !previousAccessToken.isEmpty()) {
                final boolean isTokenChanged = !previousAccessToken.equalsIgnoreCase(accessToken);
                output += "Is access token changed? " + ": " + isTokenChanged + '\n';
            }

            output += ACCESS_TOKEN + ": " + accessToken + '\n'
                    + DISPLAYABLE + ": " + displayable;

            previousAccessToken = accessToken;
        } else {
            output = bundle.getString(STRING_DATA_TO_DISPLAY);
        }

        final String correlationId = (String) bundle.get(CORRELATION_ID);
        output = CORRELATION_ID + ": " + correlationId + '\n' + output;

        mTextView.setText(output);
        mTextView.setMovementMethod(new ScrollingMovementMethod());

        mCopyAuthResultButton = view.findViewById(R.id.btn_copyAuthResult);
        mCopyAuthResultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ClipboardManager clipboard = (ClipboardManager)
                        getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                final ClipData clip = ClipData.newPlainText("Auth Result", mTextView.getText());
                clipboard.setPrimaryClip(clip);
            }
        });

        return view;
    }

    private boolean isDisplayingAccessTokenResult(@Nullable final Bundle bundle) {
        return null != bundle && bundle.containsKey(ACCESS_TOKEN);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mTextView.setText("");
    }
}
