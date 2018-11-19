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
package com.microsoft.identity.client.internal.controllers;

import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.exception.MsalArgumentException;
import com.microsoft.identity.client.internal.authorities.Authority;
import com.microsoft.identity.client.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.client.internal.authorities.AzureActiveDirectoryB2CAuthority;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.ArrayList;

public class MSALAcquireTokenSilentOperationParameters extends MSALOperationParameters {

    private RefreshTokenRecord mRefreshToken;
    private boolean mForceRefresh;

    public RefreshTokenRecord getRefreshToken() {
        return mRefreshToken;
    }

    public void setRefreshToken(final RefreshTokenRecord refreshToken) {
        mRefreshToken = refreshToken;
    }

    public void setForceRefresh(final boolean forceRefresh) {
        mForceRefresh = forceRefresh;
    }

    public boolean getForceRefresh() {
        return mForceRefresh;
    }

    @Override
    public void validate() throws MsalArgumentException {
        super.validate();

        if (mAccount == null) {
            throw new MsalArgumentException(MsalArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME, MsalArgumentException.IACCOUNT_ARGUMENT_NAME, "account is null");
        }

    }

    /**
     * Factory method for creating MSALAcquireTokenSilentOperation parameters based on the public API AcquireTokenSilentParameters
     *
     * @param acquireTokenSilentParameters
     * @param publicClientApplicationConfiguration
     * @return
     */
    public static MSALAcquireTokenSilentOperationParameters createMSALAcquireTokenSilentOperationParameters(AcquireTokenSilentParameters acquireTokenSilentParameters, PublicClientApplicationConfiguration publicClientApplicationConfiguration) {
        final MSALAcquireTokenSilentOperationParameters msalAcquireTokenSilentOperationParameters = new MSALAcquireTokenSilentOperationParameters();
        msalAcquireTokenSilentOperationParameters.setAppContext(publicClientApplicationConfiguration.getAppContext());
        msalAcquireTokenSilentOperationParameters.setScopes(new ArrayList<>(acquireTokenSilentParameters.getScopes()));
        msalAcquireTokenSilentOperationParameters.setClientId(publicClientApplicationConfiguration.getClientId());
        msalAcquireTokenSilentOperationParameters.setTokenCache(publicClientApplicationConfiguration.getOAuth2TokenCache());
        msalAcquireTokenSilentOperationParameters.setAccount(acquireTokenSilentParameters.getAccountRecord());

        if (StringUtil.isEmpty(acquireTokenSilentParameters.getAuthority())) {
            acquireTokenSilentParameters.setAuthority(getSilentRequestAuthority(acquireTokenSilentParameters.getAccount(), publicClientApplicationConfiguration));
        }
        msalAcquireTokenSilentOperationParameters.setAuthority(Authority.getAuthorityFromAuthorityUrl(acquireTokenSilentParameters.getAuthority()));

        msalAcquireTokenSilentOperationParameters.setRedirectUri(publicClientApplicationConfiguration.getRedirectUri());

        if (msalAcquireTokenSilentOperationParameters.getAuthority() instanceof AzureActiveDirectoryAuthority) {
            AzureActiveDirectoryAuthority aadAuthority = (AzureActiveDirectoryAuthority) msalAcquireTokenSilentOperationParameters.getAuthority();
            aadAuthority.setMultipleCloudsSupported(publicClientApplicationConfiguration.getMultipleCloudsSupported());
        }
        msalAcquireTokenSilentOperationParameters.setForceRefresh(acquireTokenSilentParameters.getForceRefresh());

        return msalAcquireTokenSilentOperationParameters;
    }

    /**
     * Return the correct silent request authority when none provided as a parameter to public acquireTokenSilent method.
     *
     * @param account
     * @param publicClientApplicationConfiguration
     * @return
     */
    private static String getSilentRequestAuthority(final IAccount account, final PublicClientApplicationConfiguration publicClientApplicationConfiguration) {
        String requestAuthority = null;

        // For a B2C request, the silent request will use the passed-in authority string from client app.
        if (publicClientApplicationConfiguration.getDefaultAuthority() instanceof AzureActiveDirectoryB2CAuthority) {
            requestAuthority = publicClientApplicationConfiguration.getDefaultAuthority().getAuthorityURL().toString();
        }

        // If the request is not a B2C request or the passed-in authority is not a valid URL.
        // MSAL will construct the request authority based on the account info.
        if (requestAuthority == null) {
            requestAuthority = Authority.getAuthorityFromAccount(account);
        }

        if (requestAuthority == null) {
            requestAuthority = publicClientApplicationConfiguration.getDefaultAuthority().getAuthorityURL().toString();
        }

        return requestAuthority;
    }
}
