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
package com.microsoft.identity.client.exception;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.common.java.exception.ErrorStrings;

import java.util.List;

/**
 * Exception class to indicate that one or more requested scopes have been declined by the server.
 *
 * Developers can opt to continue acquiring token by passing the silentParametersForGrantedScopes and calling
 * acquireTokenSilent call on this error.
 */
public class MsalDeclinedScopeException extends MsalException {

    private List<String> mGrantedScopes;

    private List<String> mDeclinedScopes;

    private AcquireTokenSilentParameters mSilentParametersForGrantedScopes;

    public MsalDeclinedScopeException(
            @NonNull final List<String> grantedScopes,
            @NonNull final List<String> declinedScopes,
            @NonNull final AcquireTokenSilentParameters silentParametersForGrantedScopes) {
        super(ErrorStrings.DECLINED_SCOPE_ERROR_CODE, ErrorStrings.DECLINED_SCOPE_ERROR_MESSAGE);
        mGrantedScopes = grantedScopes;
        mDeclinedScopes = declinedScopes;
        mSilentParametersForGrantedScopes = silentParametersForGrantedScopes;
    }

    /**
     * List of scopes granted by the server.
     *
     * @return List
     */
    public List<String> getGrantedScopes() {
        return mGrantedScopes;
    }

    /**
     * List of scopes declined by the server. This can happen due to multiple reasons.
     *
     * * Requested scope is not supported
     * * Requested scope is not recognized (According to OIDC, any scope values used that are not understood by an implementation should be ignored.)
     * * Requested scope is not supported for a particular account (Organizational scopes when it is a consumer account)
     *
     * @return List
     */
    public List<String> getDeclinedScopes() {
        return mDeclinedScopes;
    }

    /**
     * Returns pre configured {@link AcquireTokenSilentParameters} from the original request
     * to make a subsequent silent request for granted scopes.
     *
     * @return AcquireTokenSilentParameters
     */
    public AcquireTokenSilentParameters getSilentParametersForGrantedScopes() {
        return mSilentParametersForGrantedScopes;
    }
}
