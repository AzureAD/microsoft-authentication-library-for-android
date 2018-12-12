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
 * Helper class for pulling any available claims directive out of the WWW-Authenticate header returned
 * by resource servers
 */
public class WWWAuthenticateHeader {

    static final String CLAIMS_DIRECTIVE = "claims=";
    static final char SINGLE_QUOTE = '\'';
    static final char DOUBLE_QUOTE = '"';
    static final char SPACE = ' ';
    static final char COMMA = ',';

    /**
     * Returns a claims request parameter that corresponds to the contents of the claims directive in a
     * WWW-Authenticate Header.
     *
     * @param headerValue - String content of the www-authenticate header
     * @return
     */
    public static ClaimsRequest getClaimsRequestFromWWWAuthenticateHeaderValue(String headerValue) {

        int claimsDirectiveIndex = headerValue.indexOf(CLAIMS_DIRECTIVE);
        String claimsDirectiveJsonString = null;

        if (claimsDirectiveIndex == -1) {
            //Not Found
            return null;
        }

        //Check if directive surrounded by quotes (single or double)
        int valueStartPosition = claimsDirectiveIndex + CLAIMS_DIRECTIVE.length();
        int valueEndPosition = 0;
        int valueEndComma = 0;
        char openQuote = headerValue.substring(valueStartPosition, valueStartPosition + 1).charAt(0);


        if (openQuote == SINGLE_QUOTE) {
            //Let's get the claims string
            valueEndPosition = headerValue.indexOf(SINGLE_QUOTE, valueStartPosition + 1);
            claimsDirectiveJsonString = headerValue.substring(valueStartPosition + 1, valueEndPosition);
        } else if (openQuote == DOUBLE_QUOTE) {
            valueEndPosition = headerValue.indexOf("}" + DOUBLE_QUOTE, valueStartPosition + 1);
            claimsDirectiveJsonString = headerValue.substring(valueStartPosition + 1, valueEndPosition + 1);
        } else {
            valueEndComma = headerValue.indexOf(COMMA, valueStartPosition);
            valueEndPosition = headerValue.indexOf(SPACE, valueStartPosition);
            if (valueEndComma != -1 || valueEndPosition != -1) {
                if (valueEndComma != -1) {
                    claimsDirectiveJsonString = headerValue.substring(valueStartPosition, valueEndComma);
                } else {
                    claimsDirectiveJsonString = headerValue.substring(valueStartPosition, valueEndPosition);
                }
            } else {
                claimsDirectiveJsonString = headerValue.substring(valueStartPosition);
            }
        }

        return ClaimsRequest.getClaimsRequestFromJsonString(claimsDirectiveJsonString);
    }


    /**
     * Checks if the WWW-Authenticate header value contains the claims directive
     *
     * @param headerValue
     * @return
     */
    public static Boolean hasClaimsDirective(String headerValue) {
        int claimsDirectiveIndex = headerValue.indexOf(CLAIMS_DIRECTIVE);

        if (claimsDirectiveIndex == -1) {
            return false;
        }

        return true;
    }

}
