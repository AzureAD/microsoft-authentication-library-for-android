package com.microsoft.identity.client;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.microsoft.identity.client.EventConstants.EventProperty.LOGIN_HINT;
import static com.microsoft.identity.client.EventConstants.EventProperty.TENANT_ID;
import static com.microsoft.identity.client.EventConstants.EventProperty.USER_ID;

final class TelemetryUtils {

    static final Set<String> GDPR_FILTERED_FIELDS = new HashSet<>();

    static {
        initializeGdprFilteredFields();
    }

    private TelemetryUtils() {
        // Intentionally left blank.
    }

    private static void initializeGdprFilteredFields() {
        GDPR_FILTERED_FIELDS.addAll(
                Arrays.asList(
                        LOGIN_HINT,
                        USER_ID,
                        TENANT_ID
                )
        );
    }

}
