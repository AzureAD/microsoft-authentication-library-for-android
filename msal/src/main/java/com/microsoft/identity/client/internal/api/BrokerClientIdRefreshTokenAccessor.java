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

package com.microsoft.identity.client.internal.api;

import static com.microsoft.identity.client.exception.MsalClientException.NOT_ELIGIBLE_TO_USE_BROKER;
import static com.microsoft.identity.common.internal.broker.PackageHelper.getPackageInfo;
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.BROKER_CLIENT_ID;
import static com.microsoft.identity.common.java.commands.parameters.CommandParameters.APPLICATION_IDENTIFIER_FORMAT;
import static com.microsoft.identity.common.java.exception.ClientException.TOKEN_CACHE_ITEM_NOT_FOUND;
import static com.microsoft.identity.common.java.exception.ErrorStrings.APP_PACKAGE_NAME_NOT_FOUND;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.common.AndroidPlatformComponents;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.broker.PackageHelper;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.logging.Logger;

/**
 * For Broker apps to obtain an RT associated to Broker's client ID (for WPJ scenario).
 */
public final class BrokerClientIdRefreshTokenAccessor {

    private static final String TAG = BrokerClientIdRefreshTokenAccessor.class.getSimpleName();

    /**
     * Returns a refresh token associated to Broker's client ID.
     * Will throw an exception if The caller is not a valid Broker app.
     * NOTE: This will also wipe the AT/RT associated to Broker's client ID from MSAL local cache.
     *
     * @param context         application context
     * @param accountObjectId local_account_id of the account.
     * @return an RT, if there's any.
     * @throws MsalClientException if the calling app is not a broker app.
     */
    public static @Nullable
    String get(@NonNull final Context context,
               @NonNull final String accountObjectId) throws MsalClientException {
        final String methodTag = TAG + ":get";

        throwIfNotValidBroker(context);

        final MsalOAuth2TokenCache tokenCache = MsalOAuth2TokenCache.create(AndroidPlatformComponents.createFromContext(context));
        String applicationIdentitifier = "";

        try {
            final PackageInfo packageInfo = PackageHelper.getPackageInfo(context.getPackageManager(), context.getPackageName());
            applicationIdentitifier = String.format(APPLICATION_IDENTIFIER_FORMAT, context.getPackageName(), PackageHelper.getCurrentSignatureForPackage(packageInfo));
        }catch(PackageManager.NameNotFoundException e){
            throw new MsalClientException(APP_PACKAGE_NAME_NOT_FOUND, e.getMessage(), e);
        }

        final ICacheRecord cacheRecord = getCacheRecordForIdentifier(tokenCache, accountObjectId, applicationIdentitifier);


        if (cacheRecord == null) {
            Logger.verbose(methodTag, "No cache record found.");
            return null;
        }

        // Clear saved token since to minimize lifetime of Broker AT/RT on the client side.
        // These tokens are supposed to be one-time use.
        tokenCache.removeCredential(cacheRecord.getRefreshToken());
        tokenCache.removeCredential(cacheRecord.getAccessToken());

        if (cacheRecord.getRefreshToken() == null) {
            Logger.verbose(methodTag, "Refresh token record is empty.");
            return null;
        }

        return cacheRecord.getRefreshToken().getSecret();
    }

    private static ICacheRecord getCacheRecordForIdentifier(
            @NonNull final MsalOAuth2TokenCache tokenCache,
            @NonNull final String accountObjectId,
            @NonNull final String applicationIdentifier) throws MsalClientException {
        final AccountRecord localAccountRecord = tokenCache.getAccountByLocalAccountId(
                null,
                BROKER_CLIENT_ID,
                accountObjectId
        );

        // Check it's not null
        if (null == localAccountRecord) {
            // Unrecognized identifier, cannot supply a token.
            throw new MsalClientException(TOKEN_CACHE_ITEM_NOT_FOUND);
        }

        return tokenCache.load(
                BROKER_CLIENT_ID,
                applicationIdentifier,
                null,
                null,
                localAccountRecord,
                new BearerAuthenticationSchemeInternal() // Auth scheme is inconsequential - only using RT
        );
    }

    private static void throwIfNotValidBroker(final Context context) throws MsalClientException {
        final BrokerValidator brokerValidator = new BrokerValidator(context);
        if (!brokerValidator.isValidBrokerPackage(context.getPackageName())) {
            // package name not matched so this is not a valid broker.
            throw new MsalClientException(NOT_ELIGIBLE_TO_USE_BROKER, "This can only be invoked by Broker apps.");
        }
    }
}
