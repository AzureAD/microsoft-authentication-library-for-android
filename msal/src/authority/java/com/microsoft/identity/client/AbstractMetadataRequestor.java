package com.microsoft.identity.client;

import java.util.UUID;

abstract class AbstractMetadataRequestor<MetadataType, MetadataRequestOptions> {

    private UUID mCorrelationId;

    public final void setCorrelationId(final UUID requestCorrelationId) {
        mCorrelationId = requestCorrelationId;
    }

    public final UUID getCorrelationId() {
        return mCorrelationId;
    }

    /**
     * Requests the specified {@link MetadataType}.
     *
     * @param options parameters used for this request
     * @return the metadata
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
}
