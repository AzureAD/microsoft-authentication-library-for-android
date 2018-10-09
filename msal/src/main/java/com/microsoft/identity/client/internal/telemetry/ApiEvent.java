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

package com.microsoft.identity.client.internal.telemetry;


import com.microsoft.identity.client.internal.MsalUtils;
import com.microsoft.identity.common.internal.logging.Logger;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static com.microsoft.identity.client.internal.telemetry.EventConstants.EventProperty;

/**
 * Internal class for ApiEvent telemetry data.
 */
public final class ApiEvent extends Event {

    private static final String TAG = ApiEvent.class.getSimpleName();

    private ApiEvent(Builder builder) {
        super(builder);
        if (null != builder.mCorrelationId) {
            setProperty(EventProperty.CORRELATION_ID, builder.mCorrelationId.toString());
        }
        if (null != builder.mRequestId) {
            setProperty(EventProperty.REQUEST_ID, builder.mRequestId);
        }
        setProperty(EventProperty.AUTHORITY_NAME, HttpEvent.sanitizeUrlForTelemetry(builder.mAuthority));
        //setAuthorityType(builder.mAuthorityType);
        setProperty(EventProperty.UI_BEHAVIOR, builder.mUiBehavior);
        setProperty(EventProperty.API_ID, builder.mApiId);
        setProperty(EventProperty.AUTHORITY_VALIDATION, builder.mValidationStatus);
        //setIdToken(builder.mRawIdToken);
        setLoginHint(builder.mLoginHint);
        setProperty(EventProperty.WAS_SUCCESSFUL, String.valueOf(builder.mWasApiCallSuccessful));
        setProperty(EventProperty.API_ERROR_CODE, builder.mApiErrorCode);
    }

    public void setLoginHint(final String loginHint) {
        try {
            setProperty(EventProperty.LOGIN_HINT, MsalUtils.createHash(loginHint));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            Logger.info(TAG, null, "Skipping telemetry for LOGIN_HINT");
        }
    }

    /*
    private void setIdToken(final String rawIdToken) {
        if (MsalUtils.isEmpty(rawIdToken)) {
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
            setProperty(EventProperty.TENANT_ID, MsalUtils.createHash(idToken.getTenantId()));
            setProperty(EventProperty.USER_ID, MsalUtils.createHash(idToken.getPreferredName()));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            Logger.info(TAG, null, "Skipping TENANT_ID and USER_ID");
        }
    }
    */

    public String getAuthority() {
        return getProperty(EventProperty.AUTHORITY_NAME);
    }

    public String getUiBehavior() {
        return getProperty(EventProperty.UI_BEHAVIOR);
    }

    public String getApiId() {
        return getProperty(EventProperty.API_ID);
    }

    public String getValidationStatus() {
        return getProperty(EventProperty.AUTHORITY_VALIDATION);
    }

    public String getIdpName() {
        return getProperty(EventProperty.IDP_NAME);
    }

    public String getTenantId() {
        return getProperty(EventProperty.TENANT_ID);
    }

    public String getUserId() {
        return getProperty(EventProperty.USER_ID);
    }

    public String getLoginHint() {
        return getProperty(EventProperty.LOGIN_HINT);
    }

    public Boolean wasSuccessful() {
        return Boolean.valueOf(getProperty(EventProperty.WAS_SUCCESSFUL));
    }

    public String getRequestId() {
        return getProperty(EventProperty.REQUEST_ID);
    }

    public String getAuthorityType() {
        return getProperty(EventProperty.AUTHORITY_TYPE);
    }

    public String getApiErrorCode() {
        return getProperty(EventProperty.API_ERROR_CODE);
    }

    /**
     * Builder object for ApiEvents.
     */
    public static class Builder extends Event.Builder<Builder> {

        private String mAuthority;
        private String mUiBehavior;
        private String mApiId;
        private String mValidationStatus;
        private String mRawIdToken;
        private String mLoginHint;
        private boolean mWasApiCallSuccessful;
        private UUID mCorrelationId;
        private String mRequestId;
        private String mApiErrorCode;

        public Builder(final String requestId) {
            super(EventConstants.EventName.API_EVENT);
            mRequestId = requestId;
        }

        /**
         * Sets the authority.
         *
         * @param authority the authority to set.
         * @return the Builder instance.
         */
        public Builder setAuthority(final String authority) {
            mAuthority = authority;
            return this;
        }

        /**
         * Sets the UiBehavior.
         *
         * @param uiBehavior the UiBehavior to set.
         * @return the Builder instance.
         */
        public Builder setUiBehavior(final String uiBehavior) {
            mUiBehavior = uiBehavior;
            return this;
        }

        /**
         * Sets the apiId.
         *
         * @param apiId the apiId to set.
         * @return the Builder instance.
         */
        public Builder setApiId(final String apiId) {
            mApiId = apiId;
            return this;
        }

        /**
         * Sets the validation status.
         *
         * @param validationStatus the validation status to set.
         * @return the Builder instance.
         */
        public Builder setValidationStatus(final String validationStatus) {
            mValidationStatus = validationStatus;
            return this;
        }

        /**
         * Sets the IdToken.
         *
         * @param rawIdToken the rawIdToken
         * @return the Builder instance.
         */
        public Builder setRawIdToken(final String rawIdToken) {
            mRawIdToken = rawIdToken;
            return this;
        }

        /**
         * Sets the loginHint.
         *
         * @param loginHint the loginHint to set.
         * @return the Builder instance.
         */
        public Builder setLoginHint(final String loginHint) {
            mLoginHint = loginHint;
            return this;
        }

        /**
         * Sets the success status of the api call.
         *
         * @param callWasSuccessful the status to set.
         * @return the Builder instance.
         */
        public Builder setApiCallWasSuccessful(final boolean callWasSuccessful) {
            mWasApiCallSuccessful = callWasSuccessful;
            return this;
        }

        /**
         * Sets the correlationId of the api call.
         *
         * @param correlationId the correlationId to set
         * @return the Builder instance.
         */
        public Builder setCorrelationId(final UUID correlationId) {
            mCorrelationId = correlationId;
            return this;
        }

        /**
         * Sets the api error code.
         *
         * @param errorCode the error code to set.
         * @return the Builder instance.
         */
        public Builder setApiErrorCode(final String errorCode) {
            mApiErrorCode = errorCode;
            return this;
        }

        /**
         * Constructs a new ApiEvent.
         *
         * @return the new ApiEvent.
         */
        @Override
        public ApiEvent build() {
            return new ApiEvent(this);
        }

    }
}
