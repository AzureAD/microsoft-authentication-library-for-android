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
        responseOut.setIdToken(responseIn.getRawIdToken());
        responseOut.setExpiresOn(responseIn.getExpiresOn());
        responseOut.setExpiresIn(responseIn.getExpiresIn());
        responseOut.setExtExpiresOn(responseIn.getExtendedExpiresOn());
        responseOut.setExtExpiresIn(responseIn.getExtExpiresIn());
        responseOut.setClientInfo(responseIn.getRawClientInfo());
        responseOut.setFamilyId(""); // TODO

        // TODOs
        // state?
        // response received time?
        // not before?

        return responseOut;
    }

}
