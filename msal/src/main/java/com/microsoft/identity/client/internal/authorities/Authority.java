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
import com.microsoft.identity.client.AzureActiveDirectoryAccountIdentifier;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public abstract class Authority {

    private static final String TAG = Authority.class.getSimpleName();

    private static final String ADFS_PATH_SEGMENT = "adfs";
    private static final String B2C_PATH_SEGMENT = "tfp";

    protected boolean mKnownToMicrosoft = false;
    protected boolean mKnownToDeveloper = false;

    @SerializedName("default")
    protected boolean mIsDefault = false;

    @SerializedName("type")
    protected String mAuthorityTypeString;

    @SerializedName("authority_url")
    protected String mAuthorityUrl;

    public abstract Uri getAuthorityUri();

    public abstract URL getAuthorityURL();

    public boolean getDefault() {
        return mIsDefault;
    }

    /**
     * Returns an Authority based on an authority url.  This method attempts to parse the URL and based on the contents of it
     * determine the authority type and tenantid associated with it.
     *
     * @param authorityUrl
     * @return
     * @throws MalformedURLException
     */
    public static Authority getAuthorityFromAuthorityUrl(String authorityUrl) {
        final String methodName = ":getAuthorityFromAuthorityUrl";
        URL authUrl;

        try {
            authUrl = new URL(authorityUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid authority URL");
        }

        Uri authorityUri = Uri.parse(authUrl.toString());
        List<String> pathSegments = authorityUri.getPathSegments();

        if (pathSegments.size() == 0) {
            return new UnknownAuthority();
        }

        String authorityType = pathSegments.get(0);
        Authority authority = null;

        switch (authorityType.toLowerCase()) {
            case ADFS_PATH_SEGMENT:
                //Return new Azure Active Directory Federation Services Authority
                Logger.verbose(
                        TAG + methodName,
                        "Authority type is ADFS"
                );
                authority = new ActiveDirectoryFederationServicesAuthority(authorityUrl);
                break;
            case B2C_PATH_SEGMENT:
                //Return new B2C Authority
                Logger.verbose(
                        TAG + methodName,
                        "Authority type is B2C"
                );
                authority = new AzureActiveDirectoryB2CAuthority(authorityUrl);
                break;
            default:
                Logger.verbose(
                        TAG + methodName,
                        "Authority type default: AAD"
                );
                AzureActiveDirectoryAudience audience = AzureActiveDirectoryAudience.getAzureActiveDirectoryAudience(
                        authorityUri.getScheme() + "://" + authorityUri.getHost(),
                        pathSegments.get(0)
                );
                authority = new AzureActiveDirectoryAuthority(audience);
                break;
        }

        return authority;
    }

    public abstract OAuth2Strategy createOAuth2Strategy();

    /**
     * Indicates whether the authority is known to Microsoft or not.  Microsoft can recognize authorities that exist within public clouds.  Microsoft does
     * not maintain a list of B2C authorities or a list of ADFS or 3rd party authorities (issuers).
     *
     * @return
     */
    protected boolean getKnownToMicrosoft() {
        return mKnownToMicrosoft;
    }

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Authority)) return false;

        Authority authority = (Authority) o;

        if (!mAuthorityTypeString.equals(authority.mAuthorityTypeString)) return false;
        return getAuthorityURL().equals(authority.getAuthorityURL());
    }
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public int hashCode() {
        int result = mAuthorityTypeString.hashCode();
        result = 31 * result + getAuthorityURL().hashCode();
        return result;
    }
    //CHECKSTYLE:ON

    /**
     * These are authorities that the developer based on configuration of the public client application are known and trusted by the developer using the public client
     * application.  In order for the public client application to make a request to an authority.  That authority must be known by Microsoft or the developer
     * configuring the public client application.  Developers can check at runtime whether an authority (issuer) is known to Microsoft using the isKnownAuthority() method of
     * PublicClientApplication.  In addition the developer can request that Microsoft attempt to validate an unknown ADFS authority using the validateAuthority() method of the
     * PublicClientApplication.
     *
     * @return
     */
    protected boolean getKnownToDeveloper() {
        return mKnownToDeveloper;
    }

    private static List<Authority> knownAuthorities = new ArrayList<>();
    private static Object sLock = new Object();

    private static void performCloudDiscovery() throws IOException {
        final String methodName = ":performCloudDiscovery";
        Logger.verbose(
                TAG + methodName,
                "Performing cloud discovery..."
        );
        synchronized (sLock) {
            if (!AzureActiveDirectory.isInitialized()) {
                AzureActiveDirectory.performCloudDiscovery();
            }
        }
    }

    public static void addKnownAuthorities(List<Authority> authorities) {
        synchronized (sLock) {
            knownAuthorities.addAll(authorities);
        }
    }

    /**
     * Authorities are either known by the developer and communicated to the library via configuration or they
     * are known to Microsoft based on the list of clouds returned from:
     *
     * @return
     */
    public static boolean isKnownAuthority(Authority authority) {
        final String methodName = ":isKnownAuthority";
        boolean knownToDeveloper = false;
        boolean knownToMicrosoft = false;

        if (authority == null) {
            Logger.warn(
                    TAG + methodName,
                    "Authority is null"
            );
            return false;
        }

        //Check if authority was added to configuration
        knownToDeveloper = knownAuthorities.contains(authority);

        //Check if authority host is known to Microsoft
        knownToMicrosoft = AzureActiveDirectory.hasCloudHost(authority.getAuthorityURL());

        final boolean isKnown = (knownToDeveloper || knownToMicrosoft);

        Logger.verbose(
                TAG + methodName,
                "Authority is known to developer? [" + knownToDeveloper + "]"
        );

        Logger.verbose(
                TAG + methodName,
                "Authority is known to Microsoft? [" + knownToMicrosoft + "]"
        );

        return isKnown;
    }

    public static KnownAuthorityResult getKnownAuthorityResult(Authority authority) {
        final String methodName = ":getKnownAuthorityResult";
        Logger.verbose(
                TAG + methodName,
                "Getting known authority result..."
        );
        MsalClientException msalClientException = null;
        boolean known = false;

        try {
            Logger.verbose(
                    TAG + methodName,
                    "Performing cloud discovery"
            );
            performCloudDiscovery();
        } catch (IOException ex) {
            msalClientException = new MsalClientException(MsalClientException.IO_ERROR, "Unable to perform cloud discovery", ex);
        }

        if (msalClientException == null) {
            if (!isKnownAuthority(authority)) {
                msalClientException = new MsalClientException(MsalClientException.UNKNOWN_AUTHORITY, "Provided authority is not known.  MSAL will only make requests to known authorities");
            } else {
                known = true;
            }
        }

        return new KnownAuthorityResult(known, msalClientException);
    }

    public static class KnownAuthorityResult {
        private boolean mKnown = false;
        private MsalClientException mMsalClientException = null;

        KnownAuthorityResult(boolean known, MsalClientException exception) {
            mKnown = known;
            mMsalClientException = exception;
        }

        public boolean getKnown() {
            return mKnown;
        }

        public MsalClientException getMsalClientException() {
            return mMsalClientException;
        }
    }

    public static String getAuthorityFromAccount(final IAccount account) {
        final String methodName = ":getAuthorityFromAccount";
        Logger.verbose(
                TAG + methodName,
                "Getting authority from account..."
        );

        final AzureActiveDirectoryAccountIdentifier aadIdentifier;
        if (null != account
                && null != account.getAccountIdentifier()
                && account.getAccountIdentifier() instanceof AzureActiveDirectoryAccountIdentifier
                && null != (aadIdentifier = (AzureActiveDirectoryAccountIdentifier) account.getAccountIdentifier()).getTenantIdentifier()
                && !StringUtil.isEmpty(aadIdentifier.getTenantIdentifier())) {
            return "https://"
                    + account.getEnvironment()
                    + "/"
                    + aadIdentifier.getTenantIdentifier()
                    + "/";
        } else {
            Logger.warn(
                    TAG + methodName,
                    "Account was null..."
            );
        }

        return null;
    }
}
