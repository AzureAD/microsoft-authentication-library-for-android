package com.microsoft.identity.client.authorities;

import android.net.Uri;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public abstract class Authority {

    private static final String ADFS_PATH_SEGMENT = "adfs";
    private static final String B2C_PATH_SEGMENT = "b2c";

    protected boolean mKnownToMicrosoft = false;
    protected boolean mKnownToDeveloper = false;

    public abstract Uri getAuthorityUri();

    public static Authority getAuthorityFromAuthorityUrl(String authorityUrl) throws MalformedURLException {

        URL authUrl = new URL(authorityUrl);
        Uri authorityUri = Uri.parse(authUrl.toString());
        List<String> pathSegments = authorityUri.getPathSegments();
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

    protected boolean getKnownToMicrosoft(){
        return mKnownToMicrosoft;
    }

    protected boolean getKnownToDeveloper(){
        return mKnownToMicrosoft;
    }



}
