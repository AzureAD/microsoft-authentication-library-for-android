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

import java.util.ArrayList;
import java.util.List;

/**
 * Data container for WebFinger responses.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7033">RFC-7033</a>
 */
final class WebFingerMetadata {

    static final String JSON_KEY_SUBJECT = "subject";

    static final String JSON_KEY_LINKS = "links";

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.1">RFC-7033</a>
     */
    private String mSubject;

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4">RFC-7033</a>
     */
    private List<Link> mLinks = new ArrayList<>();

    /**
     * Gets the subject.
     *
     * @return the subject
     */
    String getSubject() {
        return mSubject;
    }

    /**
     * Sets the subject.
     *
     * @param subject the subject to set
     */
    void setSubject(final String subject) {
        this.mSubject = subject;
    }

    /**
     * Gets the links.
     *
     * @return the links
     */
    List<Link> getLinks() {
        return mLinks;
    }

    /**
     * Sets the links.
     *
     * @param links the links to set
     */
    void setLinks(final List<Link> links) {
        this.mLinks = links;
    }
}
