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
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static final String ACCOUNT_AFFINITY_SUFFIX = "**";
    private ListView mListView;
    private PackageManager mPackageManager;
    private List<ApplicationInfo> mApplications;

    private Map<String, String> mPkgAuthenticators = new HashMap<>();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        SearchView search = (SearchView) menu.findItem(R.id.search).getActionView();
        search.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        filterAndDisplayPackages(newText);
                        return true;
                    }
                });

        return true;
    }

    private void filterAndDisplayPackages(@Nullable final String filterText) {
        final List<String> packageNames = new ArrayList<>(mApplications.size());

        for (final ApplicationInfo applicationInfo : mApplications) {
            String packageDisplayName = applicationInfo.packageName;

            if (isAnAuthenticatorApp(applicationInfo.packageName)) {
                packageDisplayName = packageDisplayName + "**";
            }

            packageNames.add(packageDisplayName);
        }

        if (null != filterText && !filterText.isEmpty()) {
            // iterate over the package names, remove those who don't contain the filter text
            for (Iterator<String> nameItr = packageNames.iterator(); nameItr.hasNext(); ) {
                final String pkgName = nameItr.next();

                if (!pkgName.toLowerCase().contains(filterText.toLowerCase())) {
                    nameItr.remove();
                }
            }
        }

        final ArrayAdapter<String> appAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, packageNames);

        mListView.setAdapter(appAdapter);
        appAdapter.notifyDataSetChanged();
        mListView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> adapterView, View view, int position, long l) {
                        try {
                            String pkgName = packageNames.get(position);

                            if (pkgName.endsWith(ACCOUNT_AFFINITY_SUFFIX)) {
                                pkgName = pkgName.replace(ACCOUNT_AFFINITY_SUFFIX, "");
                            }

                            final ApplicationInfo clickedAppInfo =
                                    mPackageManager.getApplicationInfo(
                                            pkgName, PackageManager.GET_META_DATA);
                            final PackageInfo packageInfo =
                                    mPackageManager.getPackageInfo(
                                            clickedAppInfo.packageName, getPackageManagerFlag());

                            String packageSigningSha = "";

                            final Signature[] signatures = getSignatures(packageInfo);
                            if (null != signatures && signatures.length > 0) {
                                final Signature signature = signatures[0];
                                final MessageDigest digest = MessageDigest.getInstance("SHA");
                                digest.update(signature.toByteArray());
                                packageSigningSha =
                                        Base64.encodeToString(digest.digest(), Base64.NO_WRAP);
                            }

                            String msg = packageSigningSha;

                            if (isAnAuthenticatorApp(pkgName)) {
                                msg += "\n\n" + getAuthenticatorAppMetadata(pkgName);
                            }

                            LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);

                            View dialogView =
                                    layoutInflater.inflate(R.layout.dialog_layout, null, false);
                            TextView hashTextView =
                                    dialogView.findViewById(R.id.certificateHashTextView);

                            hashTextView.setText(msg);

                            new AlertDialog.Builder(MainActivity.this)
                                    .setView(dialogView)
                                    .setTitle(pkgName)
                                    .setPositiveButton(
                                            MainActivity.this.getString(R.string.dismiss),
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(
                                                        DialogInterface dialogInterface, int i) {
                                                    dialogInterface.dismiss();
                                                }
                                            })
                                    .show();
                        } catch (PackageManager.NameNotFoundException
                                | NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    @Override
    @SuppressLint("PackageManagerGetSignatures")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = findViewById(R.id.lv_apps);

        mPackageManager = getPackageManager();
        populateAuthenticatorsLookup(AccountManager.get(this));
        mApplications = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        Collections.sort(
                mApplications,
                new Comparator<ApplicationInfo>() {
                    @Override
                    public int compare(
                            @NonNull final ApplicationInfo info1,
                            @NonNull final ApplicationInfo info2) {
                        return info1.packageName.compareTo(info2.packageName);
                    }
                });

        filterAndDisplayPackages(null);
    }

    private void populateAuthenticatorsLookup(@NonNull final AccountManager accountManager) {
        final AuthenticatorDescription[] authenticatorDescriptions =
                accountManager.getAuthenticatorTypes();

        for (final AuthenticatorDescription description : authenticatorDescriptions) {
            mPkgAuthenticators.put(description.packageName, description.type);
        }
    }

    private String getAuthenticatorAppMetadata(@NonNull final String pkgName) {
        return "App has account type affinity: " + "\n" + mPkgAuthenticators.get(pkgName);
    }

    private boolean isAnAuthenticatorApp(@NonNull final String pkgName) {
        return mPkgAuthenticators.containsKey(pkgName);
    }

    private Signature[] getSignatures(@Nullable final PackageInfo packageInfo) {
        if (packageInfo == null) return null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (packageInfo.signingInfo == null) {
                return null;
            }
            if (packageInfo.signingInfo.hasMultipleSigners()) {
                return packageInfo.signingInfo.getApkContentsSigners();
            } else {
                return packageInfo.signingInfo.getSigningCertificateHistory();
            }
        }

        return packageInfo.signatures;
    }

    private static int getPackageManagerFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return PackageManager.GET_SIGNING_CERTIFICATES;
        }

        return PackageManager.GET_SIGNATURES;
    }
}
