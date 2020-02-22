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
import androidx.fragment.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * The Fragment used to display the result.
 */
public class ResultFragment extends Fragment {

    static final String ACCESS_TOKEN = "access_token";
    static final String DISPLAYABLE = "displayable";
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
        final String accessToken = (String) bundle.get(ACCESS_TOKEN);
        final String displayable = (String) bundle.get(DISPLAYABLE);

        String output = "";

        // Only display this when the app has acquired an access token at least once in this session.
        if(previousAccessToken != null && !previousAccessToken.isEmpty()){
            final boolean isTokenChanged = !previousAccessToken.equalsIgnoreCase(accessToken);
            output += "Is access token changed? " + ": " + isTokenChanged + '\n';
        }

        output += ACCESS_TOKEN + ": " + accessToken + '\n' + DISPLAYABLE + ": " + displayable;

        mTextView.setText(output);
        mTextView.setMovementMethod(new ScrollingMovementMethod());

        previousAccessToken = accessToken;

        mCopyAuthResultButton = (Button) view.findViewById(R.id.btn_copyAuthResult);
        mCopyAuthResultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager)
                        getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Auth Result", mTextView.getText());
                clipboard.setPrimaryClip(clip);
            }
        });

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mTextView.setText("");
    }
}
