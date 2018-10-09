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
package com.microsoft.identity.client.internal.authorities;

import android.net.Uri;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class AzureActiveDirectoryAuthority extends Authority {

    private static transient final String TAG = AzureActiveDirectoryAuthority.class.getSimpleName();

    @SerializedName("audience")
    public AzureActiveDirectoryAudience mAudience;

    @SerializedName("slice")
    public AzureActiveDirectorySlice mSlice;

    @SerializedName("flight_parameters")
    public Map<String, String> mFlightParameters;

    private AzureActiveDirectoryCloud mAzureActiveDirectoryCloud;

    private void getAzureActiveDirectoryCloud() {
        final String methodName = ":getAzureActiveDirectoryCloud";
        AzureActiveDirectoryCloud cloud = null;

        try {
            cloud = AzureActiveDirectory.getAzureActiveDirectoryCloud(new URL(mAudience.getCloudUrl()));
            mKnownToMicrosoft = true;
        } catch (MalformedURLException e) {
            Logger.errorPII(
                    TAG + methodName,
                    "AAD cloud URL was malformed.",
                    e
            );
            cloud = null;
            mKnownToMicrosoft = false;
        }

        mAzureActiveDirectoryCloud = cloud;
    }

    public AzureActiveDirectoryAuthority(AzureActiveDirectoryAudience signInAudience) {
        mAudience = signInAudience;
        mAuthorityTypeString = "AAD";
        getAzureActiveDirectoryCloud();
    }

    public AzureActiveDirectoryAuthority() {
        //Defaulting to AllAccounts which maps to the "common" tenant
        mAudience = new AllAccounts();
        mAuthorityTypeString = "AAD";
        getAzureActiveDirectoryCloud();
    }

    public AzureActiveDirectorySlice getSlice() {
        return this.mSlice;
    }

    public Map<String, String> getFlightParameters() {
        return this.mFlightParameters;
    }

    @Override
    public Uri getAuthorityUri() {
        getAzureActiveDirectoryCloud();
        Uri issuer;

        if (mAzureActiveDirectoryCloud == null) {
            issuer = Uri.parse(mAudience.getCloudUrl());
        } else {
            issuer = Uri.parse("https://" + mAzureActiveDirectoryCloud.getPreferredNetworkHostName());
        }

        return issuer.buildUpon().appendPath(mAudience.getTenantId()).build();
    }

    @Override
    public URL getAuthorityURL() {
        try {
            return new URL(this.getAuthorityUri().toString());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Authority URL is not a URL.", e);
        }
    }

    @Override
    public OAuth2Strategy createOAuth2Strategy() {
        final String methodName = ":createOAuth2Strategy";
        Logger.verbose(
                TAG + methodName,
                "Creating OAuth2Strategy"
        );
        MicrosoftStsOAuth2Configuration config = new MicrosoftStsOAuth2Configuration();
        config.setAuthorityUrl(this.getAuthorityURL());

        if (mSlice != null) {
            Logger.info(
                    TAG + methodName,
                    "Setting slice parameters..."
            );
            com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice slice = new com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice();
            slice.setSlice(mSlice.getSlice());
            slice.setDataCenter(mSlice.getDC());
            config.setSlice(slice);
        }

        if (mFlightParameters != null) {
            Logger.info(
                    TAG + methodName,
                    "Setting flight parameters..."
            );
            //GSON Returns a LinkedTreeMap which implement AbstractMap....
            for (Map.Entry<String, String> entry : mFlightParameters.entrySet()) {
                config.getFlightParameters().put(entry.getKey(), entry.getValue());
            }
        }

        return new MicrosoftStsOAuth2Strategy(config);
    }

}
