package com.microsoft.identity.client;

import java.util.UUID;

/**
 * MSAL internal class for representing the request context. It contains correlation id and
 * component name.
 */

final class RequestContext {
    private final UUID mCorrelationId;
    private final String mComponent;

    RequestContext(final UUID correlationId, final String component) {
        mCorrelationId = correlationId;
        mComponent = component;
    }

    UUID getCorrelationId() {
        return mCorrelationId;
    }

    String getComponent() {
        return mComponent;
    }
}
