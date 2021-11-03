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

import androidx.fragment.app.Fragment;

/**
 * Logger fragment for displaying all the logs.
 */
public class LogFragment extends Fragment {
    static final String LOG_MSG = "log_msg";

    private TextView mTextView;
    private Button mClearLogButton;
    private Button mCopyLogButton;

    public LogFragment() {
        // left empty
    }

    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_log, container, false);

        mTextView = view.findViewById(R.id.txt_log);
        final Bundle bundle = getArguments();
        final String logs = (String) bundle.get(LOG_MSG);

        mTextView.setText(logs);
        mTextView.setMovementMethod(new ScrollingMovementMethod());

        mClearLogButton = view.findViewById(R.id.btn_clearLogs);
        mClearLogButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mTextView.setText("");
                        ((MsalSampleApp) getActivity().getApplication()).clearLogs();
                    }
                });

        mCopyLogButton = view.findViewById(R.id.btn_copyLogs);
        mCopyLogButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final ClipboardManager clipboard =
                                (ClipboardManager)
                                        getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        final ClipData clip = ClipData.newPlainText("MSAL logs", logs);
                        clipboard.setPrimaryClip(clip);
                    }
                });

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
