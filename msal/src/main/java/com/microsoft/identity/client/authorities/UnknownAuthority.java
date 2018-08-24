package com.microsoft.identity.client.authorities;

import android.net.Uri;

public class UnknownAuthority extends Authority {
    @Override
    public Uri getAuthorityUri() {
        throw new UnsupportedOperationException();
    }
}
