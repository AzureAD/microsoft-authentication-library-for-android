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
 * Data container for Link elements in {@link WebFingerMetadata}.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4">RFC-7033</a>
 */
final class Link {

    static final String JSON_KEY_REL = "rel";

    static final String JSON_KEY_HREF = "href";

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4">RFC-7033</a>
     */
    private String mRel;

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4">RFC-7033</a>
     */
    private String mHref;

    /**
     * Gets the rel.
     *
     * @return the rel
     */
    String getRel() {
        return mRel;
    }

    /**
     * Sets the rel.
     *
     * @param rel the rel to set
     */
    void setRel(final String rel) {
        this.mRel = rel;
    }

    /**
     * Gets the href.
     *
     * @return the href
     */
    String getHref() {
        return mHref;
    }

    /**
     * Sets the href.
     *
     * @param href the href to set
     */
    void setHref(final String href) {
        this.mHref = href;
    }
}
