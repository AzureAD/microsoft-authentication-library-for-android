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

package com.microsoft.identity.client.developersample;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GraphData extends Fragment {

    private Button mSignout;
    private Button mExtraScope;
    private OnFragmentInteractionListener mListener;
    private static final String ARG_JSON = "json";
    private String mJsonBlob;

    public GraphData() {
        // Required empty public constructor
    }

    public static GraphData newInstance(String jsonBlob) {
        final GraphData fragment = new GraphData();
        final Bundle args = new Bundle();
        args.putString(ARG_JSON, jsonBlob);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mJsonBlob = getArguments().getString(ARG_JSON);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_graph_data, container, false);
        mSignout = (Button) view.findViewById(R.id.clearCache);
        mSignout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mListener.onSignoutClicked();
            }
        });

        mExtraScope = (Button) view.findViewById(R.id.getAnotherScope);
        mExtraScope.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mListener.onExtraScopeRequested();
            }
        });

        final Map map = convertJsonToMap(mJsonBlob);

        final StringBuilder builder = new StringBuilder();
        builder.append("Hi ");
        builder.append(map.get("displayName"));
        builder.append(",\n");

        if (!map.get("businessPhones").equals("[]")) {
            builder.append("Business Phone numbers:  ");
            builder.append(map.get("businessPhones"));
            builder.append("?\n\n");
        }

        builder.append("Email address: ");
        builder.append(map.get("userPrincipalName"));
        builder.append("\n\n");

        builder.append("Job Title:  ");
        final String jobTitle = (String) map.get("jobTitle");
        if (!jobTitle.equals("null")) {
            builder.append(map.get("jobTitle"));
        } else {
            builder.append("NA");
        }

        final TextView graphText = (TextView) view.findViewById(R.id.graphData);
        graphText.setText(builder.toString());
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

    // The interface implemented by MainActivity, this helps isolate all the UI logic to this class and business logic
    // stays in MainActivity
    public interface OnFragmentInteractionListener {
        void onSignoutClicked();

        void onExtraScopeRequested();
    }

    private Map convertJsonToMap(final String jsonBlob) {
        try {
            final JSONObject jsonObject = new JSONObject(jsonBlob);
            final Iterator keys = jsonObject.keys();
            final Map<String, String> map = new HashMap<>();
            while (keys.hasNext()) {
                final String key = (String) keys.next();
                map.put(key, jsonObject.getString(key));
            }
            return map;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
