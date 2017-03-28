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

abstract class AbstractMetadataRequestor<MetadataType, MetadataRequestOptions> {

    private RequestContext mRequestContext;

    AbstractMetadataRequestor(final RequestContext requestContext) {
        mRequestContext = requestContext;
    }

    /**
     * Requests the specified {@link MetadataType}.
     *
     * @param options parameters used for this request
     * @return <MetadataType> metadata of the parameterized type
     * @throws Exception if the metadata fails to load/deserialize
     */
    abstract MetadataType requestMetadata(MetadataRequestOptions options) throws Exception;

    /**
     * Deserializes {@link HttpResponse} objects into the specified {@link MetadataType}.
     *
     * @param response the response to deserialize
     * @return the metadata
     * @throws Exception if the metadata fails to deserialize
     */
    abstract MetadataType parseMetadata(HttpResponse response) throws Exception;

    /**
     * Gets the {@link RequestContext}.
     *
     * @return the RequestContext
     */
    public RequestContext getRequestContext() {
        return mRequestContext;
    }
}
