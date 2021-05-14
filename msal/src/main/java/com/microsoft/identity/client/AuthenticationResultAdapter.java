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

import androidx.annotation.NonNull;

import com.microsoft.identity.client.exception.MsalDeclinedScopeException;
import com.microsoft.identity.common.internal.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

class AuthenticationResultAdapter {

    private static final String TAG = AuthenticationResultAdapter.class.getName();

    static IAuthenticationResult adapt(@NonNull final ILocalAuthenticationResult localAuthenticationResult) {
        final IAuthenticationResult authenticationResult = new AuthenticationResult(
                localAuthenticationResult.getCacheRecordWithTenantProfileData(),
                localAuthenticationResult.getCorrelationId()
        );
        return authenticationResult;
    }


    /**
     * Helper method which retuns a {@link MsalDeclinedScopeException} from {@link ILocalAuthenticationResult}
     *
     * @param localAuthenticationResult : input ILocalAuthenticationResult
     * @param requestParameters         : request Token parameters.
     * @return MsalDeclinedScopeException
     */
    static MsalDeclinedScopeException declinedScopeExceptionFromResult(@NonNull final ILocalAuthenticationResult localAuthenticationResult,
                                                                       @NonNull final List<String> declinedScopes,
                                                                       @NonNull final TokenParameters requestParameters) {
        final String methodName = ":declinedScopeExceptionFromResult";
        final List<String> grantedScopes = Arrays.asList(localAuthenticationResult.getScope());
        Logger.warn(TAG + methodName,
                "Returning DeclinedScopeException as not all requested scopes are granted," +
                        " Requested scopes: " + requestParameters.getScopes().toString()
                        + " Granted scopes:" + grantedScopes.toString());

        AcquireTokenSilentParameters silentParameters;

        if (requestParameters instanceof AcquireTokenSilentParameters) {
            silentParameters = (AcquireTokenSilentParameters) requestParameters;
        } else {
            silentParameters = TokenParametersAdapter.silentParametersFromInteractive(
                    (AcquireTokenParameters) requestParameters,
                    localAuthenticationResult
            );
        }

        // Set the granted scopes as request scopes.
        silentParameters.setScopes(grantedScopes);

        return new MsalDeclinedScopeException(grantedScopes, declinedScopes, silentParameters);
    }

    static List<String> getDeclinedScopes(@NonNull final List<String> grantedScopes,
                                          @NonNull final List<String> requestedScopes) {
        final Set<String> grantedScopesSet = new HashSet<>();

        // Add each granted scope to the Set
        for (final String grantedScope : grantedScopes) {
            grantedScopesSet.add(grantedScope.toLowerCase(Locale.ROOT));
        }

        final Set<String> requestedScopesSet = new HashSet<>();

        // Add each requested scope to the Set
        for (final String requestedScope : requestedScopes) {
            requestedScopesSet.add(requestedScope.toLowerCase(Locale.ROOT));
        }

        final List<String> declinedScopes = new ArrayList<>();

        // Iterate over the requested scopes, determining which were declined
        for (final String requestedScope : requestedScopesSet) {
            if (!grantedScopesSet.contains(requestedScope)) {
                declinedScopes.add(requestedScope);
            }
        }

        return declinedScopes;
    }
}
