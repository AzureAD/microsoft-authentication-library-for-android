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
package com.microsoft.identity.client.claims;

/**
 * Represents an individual requested claims that's part of a complete claims request parameter
 *
 * @see <a href="https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter">https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter</a>
 */
public class RequestedClaim {

    private String mName;
    private RequestedClaimAdditionalInformation mInformation;

    /**
     * Returns the name of the claim being requested
     *
     * @return
     */
    public String getName() {
        return mName;
    }

    // CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RequestedClaim)) return false;

        RequestedClaim that = (RequestedClaim) o;

        if (!mName.equals(that.mName)) return false;
        return mInformation != null
                ? mInformation.equals(that.mInformation)
                : that.mInformation == null;
    }
    // CHECKSTYLE:ON

    // CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public int hashCode() {
        int result = mName.hashCode();
        result = 31 * result + (mInformation != null ? mInformation.hashCode() : 0);
        return result;
    }
    // CHECKSTYLE:ON

    /**
     * Sets the name of the claim being requested
     *
     * @param name
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * Returns additional information that can be optionally sent to the authorization server (default is null) for a particular requested claim
     *
     * @return
     */
    public RequestedClaimAdditionalInformation getAdditionalInformation() {
        return mInformation;
    }

    /**
     * Sets additional information that can be optionally sent to the authorization server (default is null) for a particular requested claim
     *
     * @return
     */
    public void setAdditionalInformation(RequestedClaimAdditionalInformation information) {
        mInformation = information;
    }
}
