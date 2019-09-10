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
package com.microsoft.identity.client.internal.controllers;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.TokenParameters;
import com.microsoft.identity.client.TokenParametersAdapter;
import com.microsoft.identity.client.exception.MsalArgumentException;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalDeclinedScopeException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalIntuneAppProtectionPolicyRequiredException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;
import com.microsoft.identity.client.exception.MsalUserCancelException;
import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.IntuneAppProtectionPolicyRequiredException;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.exception.UiRequiredException;
import com.microsoft.identity.common.exception.UserCancelException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.result.ILocalAuthenticationResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;

public class MsalExceptionAdapter {

    private static final String TAG = MsalExceptionAdapter.class.getName();

    public static MsalException msalExceptionFromBaseException(final BaseException e) {
        MsalException msalException = null;

        if (e instanceof ClientException) {
            final ClientException clientException = ((ClientException) e);
            msalException = new MsalClientException(
                    clientException.getErrorCode(),
                    clientException.getMessage(),
                    clientException
            );
        } else if (e instanceof ArgumentException) {
            final ArgumentException argumentException = ((ArgumentException) e);
            msalException = new MsalArgumentException(
                    argumentException.getArgumentName(),
                    argumentException.getOperationName(),
                    argumentException.getMessage(),
                    argumentException
            );
        } else if (e instanceof UiRequiredException) {
            final UiRequiredException uiRequiredException = ((UiRequiredException) e);
            msalException = new MsalUiRequiredException(uiRequiredException.getErrorCode(), uiRequiredException.getMessage());
        } else if (e instanceof IntuneAppProtectionPolicyRequiredException){
            msalException = new MsalIntuneAppProtectionPolicyRequiredException(
                    (IntuneAppProtectionPolicyRequiredException)e
            );
        }else if (e instanceof ServiceException) {
            final ServiceException serviceException = ((ServiceException) e);
            msalException = new MsalServiceException(
                    serviceException.getErrorCode(),
                    serviceException.getMessage(),
                    serviceException.getHttpStatusCode(),
                    serviceException
            );
        } else if (e instanceof UserCancelException) {
            msalException = new MsalUserCancelException();
        }
        if (msalException == null) {
            msalException = new MsalClientException(MsalClientException.UNKNOWN_ERROR, e.getMessage(), e);
        }

        return msalException;

    }

    /**
     * Helper method which returns if any requestes scopes are declined by the server.
     */
    public static boolean areScopeDeclinedByServer(@NonNull final List<String> requestScopes,
                                                   @NonNull final String[] responseScopes){
        final String methodName = ":areScopeDeclinedByServer";
        final Set<String> grantedScopes = new HashSet<>(Arrays.asList(responseScopes));
        for(String scope : requestScopes){
            if(!grantedScopes.contains(scope)){
                Logger.info(TAG + methodName, "Request scope not in scopes granted by server " + scope);
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method which retuns a {@link MsalDeclinedScopeException} from {@link ILocalAuthenticationResult}
     * @param localAuthenticationResult : input ILocalAuthenticationResult
     * @param requestParameters : request Token parameters.
     * @return MsalDeclinedScopeException
     */
    public static MsalDeclinedScopeException declinedScopeExceptionFromResult(@NonNull final ILocalAuthenticationResult localAuthenticationResult,
                                                                              @NonNull final TokenParameters requestParameters){
        final String methodName = ":declinedScopeExceptionFromResult";
        final List<String> grantedScopes = Arrays.asList(localAuthenticationResult.getScope());
        final List<String> declinedScopes = getDeclinedScopes(grantedScopes, requestParameters.getScopes());
        Logger.info(TAG + methodName,
                "Returning DeclinedScopeException as not all requested scopes are granted," +
                        " Requested scopes: " + requestParameters.getScopes().toString()
                        + " Granted scopes:" + grantedScopes.toString());

        AcquireTokenSilentParameters silentParameters;
        if(requestParameters instanceof AcquireTokenSilentParameters){
            silentParameters = (AcquireTokenSilentParameters) requestParameters;
        }else {
            silentParameters = TokenParametersAdapter.silentParametersFromInteractive(
                    (AcquireTokenParameters) requestParameters,
                    localAuthenticationResult
            );
        }
        // Set the granted scopes as request scopes.
        silentParameters.setScopes(grantedScopes);

        return new MsalDeclinedScopeException(grantedScopes, declinedScopes, silentParameters);
    }

    private static List<String> getDeclinedScopes(@NonNull final List<String> grantedScopes,
                                                  @NonNull final List<String> requestedScopes){

        final Set<String> grantedScopesSet = new HashSet<>(grantedScopes);
        final List<String> declinedScopes = new ArrayList<>();
        for(final String requestedScope : requestedScopes){
            if(!grantedScopesSet.contains(requestedScope)){
                declinedScopes.add(requestedScope);
            }
        }
        return declinedScopes;
    }
}
