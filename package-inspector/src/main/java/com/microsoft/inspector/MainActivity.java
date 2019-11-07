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
package com.microsoft.inspector;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ListView mListView;
    private PackageManager mPackageManager;
    private List<ApplicationInfo> mApplications;

    private Map<String, String> mPkgAuthenticators = new HashMap<>();

    @Override
    @SuppressLint("PackageManagerGetSignatures")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = findViewById(R.id.lv_apps);

        mPackageManager = getPackageManager();
        populateAuthenticatorsLookup(AccountManager.get(this));
        mApplications = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        Collections.sort(mApplications, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(@NonNull final ApplicationInfo info1,
                               @NonNull final ApplicationInfo info2) {
                return info1.packageName.compareTo(info2.packageName);
            }
        });

        final List<String> packageNames = new ArrayList<>(mApplications.size());

        for (final ApplicationInfo applicationInfo : mApplications) {
            String packageDisplayName = applicationInfo.packageName;

            if (isAnAuthenticatorApp(applicationInfo.packageName)) {
                packageDisplayName = packageDisplayName + "**";
            }

            packageNames.add(packageDisplayName);
        }

        final ArrayAdapter<String> appAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                packageNames
        );

        mListView.setAdapter(appAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                final ApplicationInfo clickedAppInfo = mApplications.get(position);
                try {
                    final PackageInfo packageInfo = mPackageManager.getPackageInfo(
                            clickedAppInfo.packageName,
                            PackageManager.GET_SIGNATURES
                    );

                    String packageSigningSha = "";

                    if (null != packageInfo
                            && null != packageInfo.signatures
                            && packageInfo.signatures.length > 0) {
                        final Signature signature = packageInfo.signatures[0];
                        final MessageDigest digest = MessageDigest.getInstance("SHA");
                        digest.update(signature.toByteArray());
                        packageSigningSha = Base64.encodeToString(digest.digest(), Base64.NO_WRAP);
                    }

                    String msg = "Certificate hash:\n" + packageSigningSha;

                    if (isAnAuthenticatorApp(clickedAppInfo.packageName)) {
                        msg += "\n\n" + getAuthenticatorAppMetadata(clickedAppInfo.packageName);
                    }

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(clickedAppInfo.packageName)
                            .setMessage(msg)
                            .setPositiveButton(
                                    MainActivity.this.getString(R.string.dismiss),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    }
                            ).show();
                } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void populateAuthenticatorsLookup(@NonNull final AccountManager accountManager) {
        final AuthenticatorDescription[] authenticatorDescriptions = accountManager.getAuthenticatorTypes();

        for (final AuthenticatorDescription description : authenticatorDescriptions) {
            mPkgAuthenticators.put(description.packageName, description.type);
        }
    }

    private String getAuthenticatorAppMetadata(@NonNull final String pkgName) {
        return "App has account type affinity: "
                + "\n"
                + mPkgAuthenticators.get(pkgName);
    }

    private boolean isAnAuthenticatorApp(@NonNull final String pkgName) {
        return mPkgAuthenticators.containsKey(pkgName);
    }
}
