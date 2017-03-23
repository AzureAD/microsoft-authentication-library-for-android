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

package com.microsoft.identity.client.sample;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.gson.Gson;
import com.microsoft.identity.client.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by weij on 3/22/2017.
 */

public class UsersFragment extends Fragment {

    private ListView mUserList;
    private Gson mGson;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_user, container, false);

        mUserList = (ListView) view.findViewById(R.id.user_list);

        final List<User> users = ((MainActivity) this.getActivity()).getUsers();
        mGson = new Gson();
        final List<String> serializedUsers = new ArrayList<>(users.size());
        for (final User user : users) {
            serializedUsers.add(mGson.toJson(user, User.class));
        }

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, serializedUsers);
        mUserList.setAdapter(arrayAdapter);

        mUserList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String value = (String) parent.getItemAtPosition(position);
                final User user = mGson.fromJson(value, User.class);

                ((MainActivity) getActivity()).setUser(user);
                getFragmentManager().popBackStack();
            }
        });
        return view;
    }
}
