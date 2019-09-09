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

/**
 * Encapsulates the parameters for the acquireTokenSilent method
 */
public class AcquireTokenSilentParameters extends TokenParameters {
    private Boolean mForceRefresh;

    public AcquireTokenSilentParameters(AcquireTokenSilentParameters.Builder builder) {
        super(builder);
        mForceRefresh = builder.mForceRefresh;
    }

    /**
     * Boolean.  Indicates whether MSAL should refresh the access token.  Default is false and
     * unless you have good reason to.  You should not use this parameter.
     *
     * @param forceRefresh
     */
    public void setForceRefresh(Boolean forceRefresh) {
        mForceRefresh = forceRefresh;
    }

    /**
     * Boolean.  Indicates whether MSAL should refresh the access token.  Default is false and
     * unless you have good reason to.  You should not use this parameter.
     *
     * @return Boolean
     */
    public Boolean getForceRefresh() {
        return mForceRefresh;
    }

    /**
     * Builder object for the acquiretokenSilent parameters object
     */
    public static class Builder extends TokenParameters.Builder<AcquireTokenSilentParameters.Builder> {

        private Boolean mForceRefresh;


        /**
         * <p>
         *     NOTE: In general you should not use this method.  MSAL will refresh the token automatically using
         *     it's own heuristic.
         * </p>
         * @param force Tells MSAL to refresh the token as part of this request
         * @return
         *
         */
        public AcquireTokenSilentParameters.Builder forceRefresh(Boolean force) {
            mForceRefresh = force;
            return self();
        }

        @Override
        public AcquireTokenSilentParameters.Builder self() {
            return this;
        }

        public AcquireTokenSilentParameters build() {
            return new AcquireTokenSilentParameters(this);
        }
    }

}
