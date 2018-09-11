package com.microsoft.identity.client.authorities;

import android.net.Uri;

import com.microsoft.identity.common.internal.providers.li.LiOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Configuration;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;

import java.net.URL;

public class LiAuthority extends Authority {
    @Override
    public Uri getAuthorityUri() {
        return null;
    }

    @Override
    public URL getAuthorityURL() {
        return null;
    }

    @Override
    public OAuth2Strategy createOAuth2Strategy() {
        return new LiOAuth2Strategy(new OAuth2Configuration());
    }
}
