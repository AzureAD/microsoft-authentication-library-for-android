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

public class AcquireTokenSilentParameters extends TokenParameters {
    private Boolean mForceRefresh;

    public AcquireTokenSilentParameters(AcquireTokenSilentParameters.Builder builder){
        super(builder);
        mForceRefresh = builder.mForceRefresh;
    }

    /**
     * Boolean.  Indicates whether MSAL should refresh the access token.  Default is false and
     * unless you have good reason to.  You should not use this parameter.
     * @param forceRefresh
     */
    public void setForceRefresh(Boolean forceRefresh){
        mForceRefresh = forceRefresh;
    }

    /**
     * Boolean.  Indicates whether MSAL should refresh the access token.  Default is false and
     * unless you have good reason to.  You should not use this parameter.
     * @return  Boolean
     */
    public Boolean getForceRefresh(){
        return mForceRefresh;
    }

    public static class Builder extends TokenParameters.Builder<AcquireTokenSilentParameters.Builder> {

        private Boolean mForceRefresh;

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
