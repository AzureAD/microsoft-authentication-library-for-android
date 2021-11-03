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

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the additional information that can be sent to an authorization server for a request claim in the claim request parameter
 *
 * @see <a href="https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter">https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter</a>
 */
public class RequestedClaimAdditionalInformation {

    public static final class SerializedNames {
        static final String ESSENTIAL = "essential";
        static final String VALUES = "values";
        static final String VALUE = "value";
    }

    @SerializedName(SerializedNames.ESSENTIAL)
    private Boolean mEssential = false;

    @SerializedName(SerializedNames.VALUES)
    private List<Object> mValues = new ArrayList<>();

    // CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RequestedClaimAdditionalInformation)) return false;

        RequestedClaimAdditionalInformation that = (RequestedClaimAdditionalInformation) o;

        if (mEssential != null ? !mEssential.equals(that.mEssential) : that.mEssential != null)
            return false;
        if (mValues != null ? !mValues.equals(that.mValues) : that.mValues != null) return false;
        return mValue != null ? mValue.equals(that.mValue) : that.mValue == null;
    }
    // CHECKSTYLE:ON

    // CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public int hashCode() {
        int result = mEssential != null ? mEssential.hashCode() : 0;
        result = 31 * result + (mValues != null ? mValues.hashCode() : 0);
        result = 31 * result + (mValue != null ? mValue.hashCode() : 0);
        return result;
    }
    // CHECKSTYLE:ON

    @SerializedName(SerializedNames.VALUE)
    private Object mValue = null;

    public void setEssential(Boolean essential) {
        mEssential = essential;
    }

    public Boolean getEssential() {
        return mEssential;
    }

    public List<Object> getValues() {
        return mValues;
    }

    public Object getValue() {
        return mValue;
    }

    public void setValue(Object value) {
        mValue = value;
    }

    public void setValues(List<Object> values) {
        mValues = values;
    }
}
