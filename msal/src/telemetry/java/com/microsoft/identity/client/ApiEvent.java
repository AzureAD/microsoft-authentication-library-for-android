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

package com.microsoft.identity.client;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import static com.microsoft.identity.client.EventConstants.EventProperty;

class ApiEvent extends Event implements IApiEvent {

    private static final String TAG = ApiEvent.class.getSimpleName();

    private ApiEvent(Builder builder) {
        super(builder);
        setAuthority(builder.mAuthority);
        setProperty(EventProperty.UI_BEHAVIOR, builder.mUiBehavior);
        setProperty(EventProperty.API_ID, builder.mApiId);
        setProperty(EventProperty.AUTHORITY_VALIDATION, builder.mValidationStatus);
        setIdToken(builder.mRawIdToken);
        setLoginHint(builder.mLoginHint);
        setProperty(EventProperty.API_DEPRECATED, String.valueOf(builder.mIsDeprecated));
        setProperty(EventProperty.EXTENDED_EXPIRES_ON_SETTING, String.valueOf(builder.mExtendedExpiresOnStatus));
        setProperty(EventProperty.WAS_SUCCESSFUL, String.valueOf(builder.mWasApiCallSuccessful));
    }

    private void setLoginHint(final String loginHint) {
        try {
            setProperty(EventProperty.LOGIN_HINT, MSALUtils.createHash(loginHint));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            Logger.info(TAG, null, "Skipping telemetry for LOGIN_HINT");
        }
    }

    private void setIdToken(final String rawIdToken) {
        if (MSALUtils.isEmpty(rawIdToken)) {
            return;
        }

        final IdToken idToken;
        try {
            idToken = new IdToken(rawIdToken);
        } catch (AuthenticationException ae) {
            return;
        }

        User user = new User(idToken);
        setProperty(EventProperty.IDP_NAME, user.getIdentityProvider());

        try {
            setProperty(EventProperty.TENANT_ID, MSALUtils.createHash(idToken.getTenantId()));
            setProperty(EventProperty.USER_ID, MSALUtils.createHash(user.getDisplayableId()));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            Logger.info(TAG, null, "Skipping TENANT_ID and USER_ID");
        }
    }

    private void setAuthority(final String authority) {
        if (MSALUtils.isEmpty(authority)) {
            return;
        }

        setProperty(EventProperty.AUTHORITY_NAME, authority);
        final URL authorityUrl = MSALUtils.getUrl(authority);
        if (authorityUrl == null) {
            return;
        }

        if (Authority.isAdfsAuthority(authorityUrl)) {
            setAuthorityType(EventProperty.Value.AUTHORITY_TYPE_ADFS);
        } else {
            setAuthorityType(EventProperty.Value.AUTHORITY_TYPE_AAD);
        }
    }

    private void setAuthorityType(final String authorityType) {
        setProperty(EventProperty.AUTHORITY_TYPE, authorityType);
    }

    @Override
    public final String getAuthority() {
        return getProperty(EventProperty.AUTHORITY_NAME);
    }

    @Override
    public final String getUiBehavior() {
        return getProperty(EventProperty.UI_BEHAVIOR);
    }

    @Override
    public final String getApiId() {
        return getProperty(EventProperty.API_ID);
    }

    @Override
    public final String getValidationStatus() {
        return getProperty(EventProperty.AUTHORITY_VALIDATION);
    }

    @Override
    public final String getIdpName() {
        return getProperty(EventProperty.IDP_NAME);
    }

    @Override
    public final String getTenantId() {
        return getProperty(EventProperty.TENANT_ID);
    }

    @Override
    public final String getUserId() {
        return getProperty(EventProperty.USER_ID);
    }

    @Override
    public final String getLoginHint() {
        return getProperty(EventProperty.LOGIN_HINT);
    }

    @Override
    public final Boolean isDeprecated() {
        return Boolean.valueOf(getProperty(EventProperty.API_DEPRECATED));
    }

    @Override
    public final Boolean hasExtendedExpiresOnStatus() {
        return Boolean.valueOf(getProperty(EventProperty.EXTENDED_EXPIRES_ON_SETTING));
    }

    @Override
    public final Boolean wasSuccessful() {
        return Boolean.valueOf(getProperty(EventProperty.WAS_SUCCESSFUL));
    }

    static class Builder extends Event.Builder<Builder> {

        private String mAuthority;
        private String mUiBehavior;
        private String mApiId;
        private String mValidationStatus;
        private String mRawIdToken;
        private String mLoginHint;
        private boolean mIsDeprecated;
        private boolean mExtendedExpiresOnStatus;
        private boolean mWasApiCallSuccessful;

        Builder authority(final String authority) {
            mAuthority = authority;
            return this;
        }

        Builder uiBehavior(final String promptBehavior) {
            mUiBehavior = promptBehavior;
            return this;
        }

        Builder apiId(final String apiId) {
            mApiId = apiId;
            return this;
        }

        Builder validationStatus(final String validationStatus) {
            mValidationStatus = validationStatus;
            return this;
        }

        Builder rawIdToken(final String rawIdToken) {
            mRawIdToken = rawIdToken;
            return this;
        }

        Builder loginHint(final String loginHint) {
            mLoginHint = loginHint;
            return this;
        }

        Builder isDeprecated(final boolean isDeprecated) {
            mIsDeprecated = isDeprecated;
            return this;
        }

        Builder hasExtendedExpiresOnStatus(final boolean hasExtendedExpiresOn) {
            mExtendedExpiresOnStatus = hasExtendedExpiresOn;
            return this;
        }

        Builder apiCallWasSuccessful(final boolean callWasSuccessful) {
            mWasApiCallSuccessful = callWasSuccessful;
            return this;
        }

        IApiEvent build() {
            eventName(EventName.API_EVENT);
            return new ApiEvent(this);
        }

    }
}
