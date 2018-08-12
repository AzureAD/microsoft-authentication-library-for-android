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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.microsoft.identity.client.AzureActiveDirectoryAccountId;
import com.microsoft.identity.client.IAccount;

import java.util.ArrayList;
import java.util.List;

/**
 * The Fragment used to display the current list of users.
 */
public class UsersFragment extends Fragment {

    private static final String USERNAME = "username";
    private static final String IS_CREDENTIAL_PRESENT = "is_credential_present";
    private static final String IDENTIFIER = "identifier";
    private static final String OBJECT_ID = "object_id";
    private static final String TENANT_ID = "tenant_id";
    private static final String ACCOUNT_ID = "account_id";
    private static final String HOME_ACCOUNT_ID = "home_account_id";
    private ListView mUserList;
    private Gson mGson;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_user, container, false);

        mUserList = (ListView) view.findViewById(R.id.user_list);

        final List<IAccount> accounts = ((MainActivity) this.getActivity()).getAccounts();
        mGson = new GsonBuilder().setPrettyPrinting().create();
        final List<String> serializedUsers = new ArrayList<>(accounts.size());
        for (final IAccount account : accounts) {
            JsonObject jsonAcct = transformToJson(account);
            serializedUsers.add(mGson.toJson(jsonAcct));
        }

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, serializedUsers);
        mUserList.setAdapter(arrayAdapter);

        mUserList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final IAccount selectedAccount = accounts.get(position);
                ((MainActivity) getActivity()).setUser(selectedAccount);
                getFragmentManager().popBackStack();
            }
        });
        return view;
    }

    @NonNull
    private JsonObject transformToJson(IAccount account) {
        JsonObject jsonAcct = new JsonObject();
        jsonAcct.addProperty(USERNAME, account.getUsername());
        jsonAcct.addProperty(IS_CREDENTIAL_PRESENT, account.isCredentialPresent());

        JsonObject accountId = new JsonObject();
        accountId.addProperty(IDENTIFIER, account.getAccountId().getIdentifier());

        if (account.getAccountId() instanceof AzureActiveDirectoryAccountId) {
            final AzureActiveDirectoryAccountId acctId = (AzureActiveDirectoryAccountId) account.getAccountId();
            accountId.addProperty(OBJECT_ID, acctId.getObjectId());
            accountId.addProperty(TENANT_ID, acctId.getTenantId());
        }


        JsonObject homeAccountId = new JsonObject();
        homeAccountId.addProperty(IDENTIFIER, account.getHomeAccountId().getIdentifier());

        if (account.getHomeAccountId() instanceof AzureActiveDirectoryAccountId) {
            final AzureActiveDirectoryAccountId acctId = (AzureActiveDirectoryAccountId) account.getHomeAccountId();
            homeAccountId.addProperty(OBJECT_ID, acctId.getObjectId());
            homeAccountId.addProperty(TENANT_ID, acctId.getTenantId());
        }

        jsonAcct.add(ACCOUNT_ID, accountId);
        jsonAcct.add(HOME_ACCOUNT_ID, homeAccountId);

        return jsonAcct;
    }
}
