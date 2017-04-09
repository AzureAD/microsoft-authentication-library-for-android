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
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static com.microsoft.identity.client.EventConstants.EventProperty;

/**
 * Internal class for ApiEvent telemetry data.
 */
final class ApiEvent extends Event {

    private static final String TAG = ApiEvent.class.getSimpleName();

    private ApiEvent(Builder builder) {
        super(builder);
        if (null != builder.mCorrelationId) {
            setProperty(EventProperty.CORRELATION_ID, builder.mCorrelationId.toString());
        }
        if (null != builder.mRequestId) {
            setProperty(EventProperty.REQUEST_ID, builder.mRequestId.toString());
        }
        setProperty(EventProperty.AUTHORITY_NAME, builder.mAuthority);
        setAuthorityType(builder.mAuthorityType);
        setProperty(EventProperty.UI_BEHAVIOR, builder.mUiBehavior);
        setProperty(EventProperty.API_ID, builder.mApiId);
        setProperty(EventProperty.AUTHORITY_VALIDATION, builder.mValidationStatus);
        setIdToken(builder.mRawIdToken);
        setLoginHint(builder.mLoginHint);
        setProperty(EventProperty.WAS_SUCCESSFUL, String.valueOf(builder.mWasApiCallSuccessful));
    }

    private void setAuthorityType(Authority.AuthorityType type) {
        if (type == null) {
            return;
        }
        final String authorityType;
        switch (type) {
            case AAD:
                authorityType = EventProperty.Value.AUTHORITY_TYPE_AAD;
                break;
            case ADFS:
                authorityType = EventProperty.Value.AUTHORITY_TYPE_ADFS;
                break;
            case B2C:
                authorityType = EventProperty.Value.AUTHORITY_TYPE_B2C;
                break;
            default:
                authorityType = EventProperty.Value.AUTHORITY_TYPE_UNKNOWN;
        }
        setProperty(EventProperty.AUTHORITY_TYPE, authorityType);
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
        } catch (MsalClientException ae) {
            return;
        }

        setProperty(EventProperty.IDP_NAME, idToken.getIssuer());

        try {
            setProperty(EventProperty.TENANT_ID, MSALUtils.createHash(idToken.getTenantId()));
            setProperty(EventProperty.USER_ID, MSALUtils.createHash(idToken.getPreferredName()));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            Logger.info(TAG, null, "Skipping TENANT_ID and USER_ID");
        }
    }

    String getAuthority() {
        return getProperty(EventProperty.AUTHORITY_NAME);
    }

    String getUiBehavior() {
        return getProperty(EventProperty.UI_BEHAVIOR);
    }

    String getApiId() {
        return getProperty(EventProperty.API_ID);
    }

    String getValidationStatus() {
        return getProperty(EventProperty.AUTHORITY_VALIDATION);
    }

    String getIdpName() {
        return getProperty(EventProperty.IDP_NAME);
    }

    String getTenantId() {
        return getProperty(EventProperty.TENANT_ID);
    }

    String getUserId() {
        return getProperty(EventProperty.USER_ID);
    }

    String getLoginHint() {
        return getProperty(EventProperty.LOGIN_HINT);
    }

    Boolean wasSuccessful() {
        return Boolean.valueOf(getProperty(EventProperty.WAS_SUCCESSFUL));
    }

    Telemetry.RequestId getRequestId() {
        return new Telemetry.RequestId(getProperty(EventProperty.REQUEST_ID));
    }

    String getAuthorityType() {
        return getProperty(EventProperty.AUTHORITY_TYPE);
    }

    /**
     * Builder object for ApiEvents.
     */
    static class Builder extends Event.Builder<Builder> {

        private String mAuthority;
        private Authority.AuthorityType mAuthorityType;
        private String mUiBehavior;
        private String mApiId;
        private String mValidationStatus;
        private String mRawIdToken;
        private String mLoginHint;
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

        Builder setAuthorityType(final Authority.AuthorityType authorityType) {
            mAuthorityType = authorityType;
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
         * @param rawIdToken the rawIdToken
         * @return the Builder instance.
         */
        Builder setRawIdToken(final String rawIdToken) {
            mRawIdToken = rawIdToken;
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
        ApiEvent build() {
            return new ApiEvent(this);
        }

    }
}
