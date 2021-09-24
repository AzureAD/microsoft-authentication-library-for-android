// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.e2e.shadows;

import com.microsoft.identity.common.java.cache.CacheRecord;
import com.microsoft.identity.common.internal.commands.DeviceCodeFlowCommand;
import com.microsoft.identity.common.internal.commands.DeviceCodeFlowCommandCallback;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.java.result.LocalAuthenticationResult;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Shadow class that simulates Device Code Flow successfully returning an acquire token result object.
 */
@Implements(DeviceCodeFlowCommand.class)
public class ShadowDeviceCodeFlowCommandSuccessful {
    private final static String AUTHORITY_TYPE = "MSSTS";
    private final static String LOCAL_ACCOUNT_ID = "99a1340e-0f35-4ac1-94ac-0837718f0b1f";
    private final static String USERNAME = "mock-username";
    private final static String HOME_ACCOUNT_ID = "mock-home-account-id";
    private final static String ENVIRONMENT = "login.windows.net";
    private final static String REALM = "3c62ac97-29eb-4aed-a3c8-add0298508d";
    private final static String TARGET = "mock-target";
    private final static String CACHE_AT = "mock-cache-at";
    private final static String EXPIRES_ON = String.valueOf(System.currentTimeMillis() + 100000);
    private final static String AT_SECRET = "d22d37bf-6e2c-40ea-b763-774897c05262";
    private final static String CLIENT_ID = "4b0db8c2-9f26-4417-8bde-3f0e3656f8e0";
    private final static String RT_SECRET = "adf49b53-a92f-4930-9de6-926505d29e18";
    private final static String RAW_ID_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJUZXN0U3ViamVjdCIsImF1ZCI6ImF1ZGllbmNlLWZvci10ZXN0aW5nIiwidmVyIjoiMi4wIiwibmJmIjoxNjMyMzYxODY1LCJpc3MiOiJodHRwczpcL1wvdGVzdC5hdXRob3JpdHlcLzM1OTY1NDJlLTFlMGItNGM4Yy05YjM0LWI4M2ZkZDA1Mjk5MFwvdjIuMCIsIm5hbWUiOiJ0ZXN0IiwicHJlZmVycmVkX3VzZXJuYW1lIjoidGVzdEB0ZXN0Lm9ubWljcm9zb2Z0LmNvbSIsIm9pZCI6Ijk5YTEzNDBlLTBmMzUtNGFjMS05NGFjLTA4Mzc3MThmMGIxZiIsImV4cCI6MTYzMjM2NTQ2NSwiaWF0IjoxNjMyMzYxODY1LCJ0aWQiOiIzNTk2NTQyZS0xZTBiLTRjOGMtOWIzNC1iODNmZGQwNTI5OTAifQ.1VvzdN6NuiP8kXqnblJNX_NR9kegC5m44uibA2q3c-Y";
    private final static String FAMILY_ID = "mock-family-id";
    private final static String PRT_SESSION_KEY = "mock-prt-session-key";
    private final static CredentialType ID_TOKEN_TYPE = CredentialType.IdToken;

    @RealObject
    private DeviceCodeFlowCommand mDeviceCodeFlowCommand;

    @Implementation
    public AcquireTokenResult execute() {
        // 15 minutes is the default timeout.
        final Date expiryDate = new Date();
        expiryDate.setTime(expiryDate.getTime() + TimeUnit.MINUTES.toMillis(15));

        final DeviceCodeFlowCommandCallback callback = (DeviceCodeFlowCommandCallback) mDeviceCodeFlowCommand.getCallback();
        callback.onUserCodeReceived(
                "vUri Here",
                "ABCDEFGH",
                "Follow these instructions to authenticate.",
                expiryDate);

        // Create parameters for dummy authentication result
        final AccountRecord accountRecord = new AccountRecord();
        accountRecord.setHomeAccountId(HOME_ACCOUNT_ID);
        accountRecord.setLocalAccountId(LOCAL_ACCOUNT_ID);
        accountRecord.setEnvironment(ENVIRONMENT);
        accountRecord.setRealm(REALM);
        accountRecord.setUsername(USERNAME);
        accountRecord.setFirstName("mock");
        accountRecord.setMiddleName("mock");
        accountRecord.setFamilyName("mock");
        accountRecord.setClientInfo("mock");
        accountRecord.setName("mock");
        accountRecord.setAuthorityType(AUTHORITY_TYPE);

        final RefreshTokenRecord refreshTokenRecord = new RefreshTokenRecord();

        final AccessTokenRecord accessTokenRecord = new AccessTokenRecord();
        accessTokenRecord.setRealm(REALM);
        accessTokenRecord.setAuthority("https://login.microsoftonline.com/common");
        accessTokenRecord.setClientId(CLIENT_ID);
        accessTokenRecord.setSecret(AT_SECRET);
        accessTokenRecord.setExpiresOn(EXPIRES_ON);
        accessTokenRecord.setExtendedExpiresOn(EXPIRES_ON + 40000);
        accessTokenRecord.setAccessTokenType("Bearer");

        final IdTokenRecord idTokenRecord = new IdTokenRecord();
        idTokenRecord.setHomeAccountId(HOME_ACCOUNT_ID);
        idTokenRecord.setEnvironment(ENVIRONMENT);
        idTokenRecord.setCredentialType(ID_TOKEN_TYPE.toString());
        idTokenRecord.setClientId(CLIENT_ID);
        idTokenRecord.setRealm(REALM);
        idTokenRecord.setSecret(RAW_ID_TOKEN);

        final CacheRecord.CacheRecordBuilder recordBuilder = CacheRecord.builder();
        recordBuilder.refreshToken(refreshTokenRecord);
        recordBuilder.accessToken(accessTokenRecord);
        recordBuilder.account(accountRecord);
        recordBuilder.idToken(idTokenRecord);

        final List<ICacheRecord> cacheRecordList = new ArrayList<>();
        final ICacheRecord cacheRecord = recordBuilder.build();
        cacheRecordList.add(cacheRecord);

        // Create dummy authentication result
        final ILocalAuthenticationResult localAuthenticationResult = new LocalAuthenticationResult(
                cacheRecord,
                cacheRecordList,
                SdkType.MSAL,
                false
        );

        final AcquireTokenResult tokenResult = new AcquireTokenResult();
        tokenResult.setLocalAuthenticationResult(localAuthenticationResult);

        return tokenResult;
    }
}
