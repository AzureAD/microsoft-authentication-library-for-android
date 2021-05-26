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
package com.microsoft.identity.client.internal;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;

import java.util.List;
import java.util.Map;

/**
 * Factory class for creating {@link AcquireTokenParameters} and
 * {@link AcquireTokenParameters.Builder} instances.
 */
public class AcquireTokenParametersFactory {

    private final HeaderParser mParser;

    /**
     * Creates a new AcquireTokenParametersFactory.
     *
     * @param parser The delegate {@link HeaderParser}.
     */
    public AcquireTokenParametersFactory(final HeaderParser parser) {
        mParser = parser;
    }

    /**
     * Creates a mutable {@link AcquireTokenParameters.Builder} for constructing token requests.
     *
     * @param headers The headers from which to build the params.
     * @return The builder.
     */
    public AcquireTokenParameters.Builder createParametersBuilderFromHeaders(final Map<String, List<String>> headers) {
        return mParser.createBuilderFromHeaders(headers);
    }

    /**
     * Creates an immutable {@link AcquireTokenParameters} object.
     *
     * @param headers The headers from which to build the params.
     * @return The {@link AcquireTokenParameters}.
     */
    public AcquireTokenParameters createParametersFromHeaders(final Map<String, List<String>> headers) {
        return createParametersBuilderFromHeaders(headers).build();
    }

    /**
     * Creates a mutable {@link AcquireTokenSilentParameters.Builder} for constructing silent
     * token requests.
     *
     * @param headers The headers from which to build the params.
     * @return The builder.
     */
    public AcquireTokenSilentParameters.Builder createSilentParametersBuilderFromHeaders(final Map<String, List<String>> headers) {
        return mParser.createSilentBuilderFromHeaders(headers);
    }

    /**
     * Creates an immutable {@link AcquireTokenParameters} object.
     *
     * @param headers The headers from which to build the params.
     * @return The parameters.
     */
    public AcquireTokenSilentParameters createSilentParametersFromHeaders(final Map<String, List<String>> headers) {
        return mParser.createSilentBuilderFromHeaders(headers).build();
    }

    /**
     * Extension point for providing delegate parsers.
     */
    public interface HeaderParser {

        /**
         * Creates a mutable {@link AcquireTokenParameters.Builder} instance for use in constructing
         * token requests.
         *
         * @param headers The input headers from which to generate the params.
         * @return The params builder.
         */
        AcquireTokenParameters.Builder createBuilderFromHeaders(Map<String, List<String>> headers);

        /**
         * Creates a mutable {@link AcquireTokenSilentParameters.Builder} instance for use in
         * constructing silent token requests.
         *
         * @param headers The input headers from which to generate the params.
         * @return The silent params builder.
         */
        AcquireTokenSilentParameters.Builder createSilentBuilderFromHeaders(Map<String, List<String>> headers);
    }
}
