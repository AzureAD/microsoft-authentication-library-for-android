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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.ITenantProfile;
import com.microsoft.identity.client.MultiTenantAccount;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Fragment used to display the current list of users.
 */
public class UsersFragment extends Fragment {

    private ListView mUserList;
    private Gson mGson;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_user, container, false);

        mUserList = view.findViewById(R.id.user_list);

        MsalWrapper.getInstance().registerPostAccountLoadedJob("UsersFragment.onCreateView",
                new MsalWrapper.IPostAccountLoaded() {
                    @Override
                    public void onLoaded(List<IAccount> loadedAccount) {
                        createViewWithAccountList(loadedAccount);
                        MsalWrapper.getInstance().deregisterPostAccountLoadedJob("UsersFragment.onCreateView");
                    }
                });

        return view;
    }

    private void createViewWithAccountList(final List<IAccount> accounts) {
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
                getFragmentManager().popBackStack();
            }
        });
    }

    @NonNull
    private JsonObject transformToJson(IAccount account) {
        JsonObject jsonAcct = new JsonObject();

        jsonAcct.addProperty("id", account.getId());
        jsonAcct.add("claims", claimsToJson(account.getClaims()));

        if (account instanceof MultiTenantAccount) {
            jsonAcct.add(
                    "tenant_profiles",
                    tenantProfilesToJson(
                            ((MultiTenantAccount) account).getTenantProfiles()
                    )
            );
        }

        return jsonAcct;
    }

    @Nullable
    private JsonElement tenantProfilesToJson(@Nullable final Map<String, ITenantProfile> tenantProfiles) {
        if (null != tenantProfiles) {
            final JsonArray jsonArray = new JsonArray();

            for (final Map.Entry<String, ITenantProfile> profileEntry : tenantProfiles.entrySet()) {
                final JsonObject object = new JsonObject();

                object.addProperty("id", profileEntry.getValue().getId());
                object.add("claims", claimsToJson(profileEntry.getValue().getClaims()));

                jsonArray.add(object);
            }

            return jsonArray;
        }

        return null;
    }

    @Nullable
    private JsonElement claimsToJson(@Nullable final Map<String, ?> claims) {
        if (null != claims) {
            JsonObject element = new JsonObject();

            for (final Map.Entry<String, ?> claim : claims.entrySet()) {
                element.addProperty(claim.getKey(), claim.getValue().toString());
            }

            return element;
        }

        return null;
    }
}
