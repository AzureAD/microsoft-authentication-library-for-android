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
package com.microsoft.identity.client;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.tokensharing.ITokenShareResultInternal;
import com.microsoft.identity.common.adal.internal.tokensharing.TokenShareResultInternal;

/**
 * Refresh Token Related Metadata for consumption by TSL.
 */
public class TokenShareResult extends TokenShareResultInternal {

    /**
     * The format of the refresh token in this result payload.
     */
    public static class TokenShareExportFormat {

        /**
         * Used for ORG_ID accounts. Legacy format used by ADAL.
         */
        public static final String SSO_STATE_SERIALIZER_BLOB =
                TokenShareExportFormatInternal.SSO_STATE_SERIALIZER_BLOB;

        /**
         * Raw RT String. Used by MSA format.
         */
        public static final String RAW = TokenShareExportFormatInternal.RAW;
    }

    TokenShareResult(@NonNull final ITokenShareResultInternal resultInternal) {
        super(
                resultInternal.getCacheRecord(),
                resultInternal.getRefreshToken(),
                resultInternal.getFormat());
    }

    /**
     * {@inheritDoc}
     *
     * @see TokenShareExportFormat
     */
    @Override
    public String getFormat() {
        return super.getFormat();
    }
}
