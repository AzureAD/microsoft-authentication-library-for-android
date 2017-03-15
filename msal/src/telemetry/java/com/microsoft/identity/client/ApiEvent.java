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

import android.content.Context;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

public class ApiEvent extends BaseEvent {

    private static final String TAG = BaseEvent.class.getSimpleName();

    private final Telemetry.EventName mEventName;

    ApiEvent() {
        setProperty(Properties.EVENT_NAME, Names.API_EVENT.value);
        mEventName = Names.API_EVENT;
    }

    ApiEvent(final Context context, final String clientId) {
        this();
        setDefaults(context, clientId);
    }

    void setAuthority(final String authority) {
        if (MSALUtils.isEmpty(authority)) {
            return;
        }

        setProperty(Properties.AUTHORITY_NAME, authority);
        final URL authorityUrl = MSALUtils.getUrl(authority);
        if (authorityUrl == null) {
            return;
        }

        if (MSALUtils.isADFSAuthority(authorityUrl)) {
            setAuthorityType(Properties.Values.AUTHORITY_TYPE_ADFS);
        } else {
            setAuthorityType(Properties.Values.AUTHORITY_TYPE_AAD);
        }
    }

    void setAuthorityType(final String authorityType) {
        setProperty(Properties.AUTHORITY_TYPE, authorityType);
    }

    void setIsDeprecated(final boolean isDeprecated) {
        setProperty(Properties.API_DEPRECATED, String.valueOf(isDeprecated));
    }

    void setValidationStatus(final String validationStatus) {
        setProperty(Properties.AUTHORITY_VALIDATION, validationStatus);
    }

    void setExtendedExpiresOnSetting(final boolean extendedExpiresOnSetting) {
        setProperty(Properties.EXTENDED_EXPIRES_ON_SETTING, String.valueOf(extendedExpiresOnSetting));
    }

    void setPromptBehavior(final String promptBehavior) {
        setProperty(Properties.PROMPT_BEHAVIOR, promptBehavior);
    }

    void setAPIId(final String id) {
        setProperty(Properties.API_ID, id);
    }

    @Override
    Telemetry.EventName getEventName() {
        return mEventName;
    }

    void setWasApiCallSuccessful(final boolean isSuccess, final Exception exception) {
        setProperty(Properties.WAS_SUCCESSFUL, String.valueOf(isSuccess));

        if (exception != null) {
            if (exception instanceof AuthenticationException) {
                final AuthenticationException authException = (AuthenticationException) exception;
                setProperty(Properties.API_ERROR_CODE, authException.getErrorCode().toString());
            }
        }
    }

    void stopTelemetryAndFlush() {
        Telemetry.getInstance().stopEvent(getRequestId(), getEventName(), this);
        Telemetry.getInstance().flush(getRequestId());
    }

    void setIdToken(final String rawIdToken) {
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
        setProperty(Properties.IDP_NAME, user.getIdentityProvider());

        try {
            setProperty(Properties.TENANT_ID, MSALUtils.createHash(idToken.getTenantId()));
            setProperty(Properties.USER_ID, MSALUtils.createHash(user.getDisplayableId()));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            Logger.info(TAG, null, "Skipping TENANT_ID and USER_ID");
        }
    }

    void setLoginHint(final String loginHint) {
        try {
            setProperty(Properties.LOGIN_HINT, MSALUtils.createHash(loginHint));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            Logger.info(TAG, null, "Skipping telemetry for LOGIN_HINT");
        }
    }

}
