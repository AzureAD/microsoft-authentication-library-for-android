package com.microsoft.identity.client;

import java.net.URL;

public class ADFSWebFingerValidator {
    public static boolean realmIsTrusted(URL mAuthorityUrl, WebFingerMetadata webFingerMetadata) {
        return false;
    }
}
