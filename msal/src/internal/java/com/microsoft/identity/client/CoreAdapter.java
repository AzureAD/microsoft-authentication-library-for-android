package com.microsoft.identity.client;

import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;

public class CoreAdapter {

    private CoreAdapter() {
        // Utility class.
    }

    static MicrosoftStsTokenResponse asMsStsTokenResponse(final TokenResponse responseIn) {
        final MicrosoftStsTokenResponse responseOut = new MicrosoftStsTokenResponse();
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

        responseOut.setExtExpiresOn(responseIn.getExtendedExpiresOn());

        // TODO no 'not before' ?
        //responseOut.setNotBefore();

        responseOut.setClientInfo(responseIn.getRawClientInfo());

        // TODO no family id?
        //responseOut.setFamilyId();

        return responseOut;
    }

}
