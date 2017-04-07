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
import java.util.UUID;

import static com.microsoft.identity.client.EventConstants.EventProperty;

/**
 * Internal class for ApiEvent telemetry data.
 */
final class ApiEvent extends Event implements IApiEvent {

    private static final String TAG = ApiEvent.class.getSimpleName();

    private ApiEvent(Builder builder) {
        super(builder);
        if (null != builder.mCorrelationId) {
            setProperty(EventProperty.CORRELATION_ID, builder.mCorrelationId.toString());
        }
        if (null != builder.mRequestId) {
            setProperty(EventProperty.REQUEST_ID, builder.mRequestId.toString());
        }
        setAuthority(builder.mAuthority);
        setProperty(EventProperty.UI_BEHAVIOR, builder.mUiBehavior);
        setProperty(EventProperty.API_ID, builder.mApiId);
        setProperty(EventProperty.AUTHORITY_VALIDATION, builder.mValidationStatus);
        setIdToken(builder.mUser);
        setLoginHint(builder.mLoginHint);
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

    private void setIdToken(final User user) {
        if (null == user) {
            return;
        }
        setProperty(EventProperty.IDP_NAME, user.getIdentityProvider());

        try {
            setProperty(EventProperty.TENANT_ID, MSALUtils.createHash(user.getUtid()));
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
    public String getAuthority() {
        return getProperty(EventProperty.AUTHORITY_NAME);
    }

    @Override
    public String getUiBehavior() {
        return getProperty(EventProperty.UI_BEHAVIOR);
    }

    @Override
    public String getApiId() {
        return getProperty(EventProperty.API_ID);
    }

    @Override
    public String getValidationStatus() {
        return getProperty(EventProperty.AUTHORITY_VALIDATION);
    }

    @Override
    public String getIdpName() {
        return getProperty(EventProperty.IDP_NAME);
    }

    @Override
    public String getTenantId() {
        return getProperty(EventProperty.TENANT_ID);
    }

    @Override
    public String getUserId() {
        return getProperty(EventProperty.USER_ID);
    }

    @Override
    public String getLoginHint() {
        return getProperty(EventProperty.LOGIN_HINT);
    }

    @Override
    public Boolean getExtendedExpiresOnStatus() {
        return Boolean.valueOf(getProperty(EventProperty.EXTENDED_EXPIRES_ON_SETTING));
    }

    @Override
    public Boolean wasSuccessful() {
        return Boolean.valueOf(getProperty(EventProperty.WAS_SUCCESSFUL));
    }

    @Override
    public Telemetry.RequestId getRequestId() {
        return new Telemetry.RequestId(getProperty(EventProperty.REQUEST_ID));
    }

    /**
     * Builder object for ApiEvents.
     */
    static class Builder extends Event.Builder<Builder> {

        private String mAuthority;
        private String mUiBehavior;
        private String mApiId;
        private String mValidationStatus;
        private User mUser;
        private String mLoginHint;
        private boolean mExtendedExpiresOnStatus;
        private boolean mWasApiCallSuccessful;
        private UUID mCorrelationId;
        private Telemetry.RequestId mRequestId;

        Builder(final Telemetry.RequestId requestId) {
            super(EventName.API_EVENT);
            mRequestId = requestId;
        }

        /**
         * Sets the authority.
         *
         * @param authority the authority to set.
         * @return the Builder instance.
         */
        Builder setAuthority(final String authority) {
            mAuthority = authority;
            return this;
        }

        /**
         * Sets the UiBehavior.
         *
         * @param uiBehavior the UiBehavior to set.
         * @return the Builder instance.
         */
        Builder setUiBehavior(final String uiBehavior) {
            mUiBehavior = uiBehavior;
            return this;
        }

        /**
         * Sets the apiId.
         *
         * @param apiId the apiId to set.
         * @return the Builder instance.
         */
        Builder setApiId(final String apiId) {
            mApiId = apiId;
            return this;
        }

        /**
         * Sets the validation status.
         *
         * @param validationStatus the validation status to set.
         * @return the Builder instance.
         */
        Builder setValidationStatus(final String validationStatus) {
            mValidationStatus = validationStatus;
            return this;
        }

        /**
         * Sets the IdToken.
         *
         * @param user the {@link User} used to derive the idToken.
         * @return the Builder instance.
         */
        Builder setIdToken(final User user) {
            mUser = user;
            return this;
        }

        /**
         * Sets the loginHint.
         *
         * @param loginHint the loginHint to set.
         * @return the Builder instance.
         */
        Builder setLoginHint(final String loginHint) {
            mLoginHint = loginHint;
            return this;
        }

        /**
         * Sets the ExtendedExpiresOnStatus.
         *
         * @param hasExtendedExpiresOn the status to set.
         * @return the Builder instance.
         */
        Builder setExtendedExpiresOnStatus(final boolean hasExtendedExpiresOn) {
            mExtendedExpiresOnStatus = hasExtendedExpiresOn;
            return this;
        }

        /**
         * Sets the success status of the api call.
         *
         * @param callWasSuccessful the status to set.
         * @return the Builder instance.
         */
        Builder setApiCallWasSuccessful(final boolean callWasSuccessful) {
            mWasApiCallSuccessful = callWasSuccessful;
            return this;
        }

        /**
         * Sets the correlationId of the api call.
         *
         * @param correlationId the correlationId to set
         * @return the Builder instance
         */
        Builder setCorrelationId(final UUID correlationId) {
            mCorrelationId = correlationId;
            return this;
        }

        /**
         * Constructs a new ApiEvent.
         *
         * @return the new ApiEvent.
         */
        @Override
        IApiEvent build() {
            return new ApiEvent(this);
        }

    }
}
