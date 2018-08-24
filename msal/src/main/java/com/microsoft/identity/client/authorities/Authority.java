package com.microsoft.identity.client.authorities;

import android.net.Uri;

import com.google.gson.annotations.SerializedName;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public abstract class Authority {

    private static final String ADFS_PATH_SEGMENT = "adfs";
    private static final String B2C_PATH_SEGMENT = "b2c";

    protected boolean mKnownToMicrosoft = false;
    protected boolean mKnownToDeveloper = false;

    @SerializedName("default")
    protected boolean mIsDefault = false;

    @SerializedName("authority_type")
    protected String mAuthorityTypeString;
    protected AuthorityType mAuthorityType;

    @SerializedName("authority_url")
    protected String mAuthorityUrl;

    public abstract Uri getAuthorityUri();

    /**
     * Returns an Authority based on an authority url.  This method attempts to parse the URL and based on the contents of it
     * determine the authority type and tenantid associated with it.
     * @param authorityUrl
     * @return
     * @throws MalformedURLException
     */
    public static Authority getAuthorityFromAuthorityUrl(String authorityUrl) throws MalformedURLException {

        URL authUrl = new URL(authorityUrl);
        Uri authorityUri = Uri.parse(authUrl.toString());
        List<String> pathSegments = authorityUri.getPathSegments();

        if(pathSegments.size() == 0){
            throw new IllegalArgumentException("Authority urls are expected to include at least one path segment.");
        }

        String authorityType = pathSegments.get(0);
        Authority authority = null;

        switch(authorityType.toLowerCase()){
            case ADFS_PATH_SEGMENT:
                //Return new Azure Active Directory Federation Services Authority
                break;
            case B2C_PATH_SEGMENT:
                //Return new B2C Authority
                break;
            default:
                AzureActiveDirectoryAudience audience = AzureActiveDirectoryAudience.getAzureActiveDirectoryAudience(authorityUri.getAuthority(), pathSegments.get(0));
                authority = new AzureActiveDirectoryAuthority(audience);
                break;
        }

        return authority;

    }

    /**
     * Indicates whether the authority is known to Microsoft or not.  Microsoft can recognize authorities that exist within public clouds.  Microsoft does
     * not maintain a list of B2C authorities or a list of ADFS or 3rd party authorities (issuers).
     * @return
     */
    protected boolean getKnownToMicrosoft(){
        return mKnownToMicrosoft;
    }

    /**
     * These are authorities that the developer based on configuration of the public client application are known and trusted by the developer using the public client
     * application.  In order for the public client application to make a request to an authority.  That authority must be known by Microsoft or the developer
     * configuring the public client application.  Developers can check at runtime whether an authority (issuer) is known to Microsoft using the isKnownAuthority() method of
     * PublicClientApplication.  In addition the developer can request that Microsoft attempt to validate an unknown ADFS authority using the validateAuthority() method of the
     * PublicClientApplication.
     * @return
     */
    protected boolean getKnownToDeveloper(){
        return mKnownToDeveloper;
    }

    protected AuthorityType getAuthorityType(){
        return AuthorityType.valueOf(this.mAuthorityTypeString);
    }



}
