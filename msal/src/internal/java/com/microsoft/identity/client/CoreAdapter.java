package com.microsoft.identity.client;

import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryTokenResponse;

public class CoreAdapter {

    private CoreAdapter() {
        // Utility class.
    }

    static AzureActiveDirectoryTokenResponse asAadTokenResponse(final TokenResponse responseIn) {
        final AzureActiveDirectoryTokenResponse responseOut = new AzureActiveDirectoryTokenResponse();
        responseOut.setAccessToken(responseIn.getAccessToken());
        responseOut.setTokenType(responseIn.getTokenType());
        responseOut.setRefreshToken(responseIn.getRefreshToken());
        responseOut.setScope(responseIn.getScope());

        // TODO no state parameter?
        //responseOut.setState();

        responseOut.setIdToken(responseIn.getRawIdToken());

        // TODO no response received time?
        //responseOut.setResponseReceivedTime();

        responseOut.setExpiresOn(responseIn.getExpiresOn());

        // TODO no resource?
        //responseOut.setResource();

        responseOut.setExtExpiresOn(responseIn.getExtendedExpiresOn());

        // TODO no 'not before' ?
        //responseOut.setNotBefore();

        responseOut.setClientInfo(responseIn.getRawClientInfo());

        // TODO no family id?
        //responseOut.setFamilyId();

        return responseOut;
    }

}
