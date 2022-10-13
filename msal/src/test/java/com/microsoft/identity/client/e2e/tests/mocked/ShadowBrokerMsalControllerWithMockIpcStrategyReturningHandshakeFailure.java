package com.microsoft.identity.client.e2e.tests.mocked;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.HELLO_ERROR_CODE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.HELLO_ERROR_MESSAGE;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle;
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy;
import com.microsoft.identity.common.internal.controllers.BrokerMsalController;
import com.microsoft.identity.common.java.exception.ErrorStrings;

import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.List;

@Implements(BrokerMsalController.class)
public class ShadowBrokerMsalControllerWithMockIpcStrategyReturningHandshakeFailure {

    public static final String MOCK_ACTIVE_BROKER_NAME = "MOCK_BROKER";

    @VisibleForTesting
    public String getActiveBrokerPackageName() {
        return MOCK_ACTIVE_BROKER_NAME;
    }

    @NonNull
    protected List<IIpcStrategy> getIpcStrategies(final Context applicationContext, final String activeBrokerPackageName) {
        final List<IIpcStrategy> strategies = new ArrayList<>();
        strategies.add(new IIpcStrategy() {
            @Override
            public Bundle communicateToBroker(@NonNull BrokerOperationBundle bundle) throws BrokerCommunicationException {
                final Bundle errorBundle = new Bundle();
                errorBundle.putString(HELLO_ERROR_CODE, ErrorStrings.UNSUPPORTED_BROKER_VERSION_ERROR_CODE);
                errorBundle.putString(HELLO_ERROR_MESSAGE, ErrorStrings.UNSUPPORTED_BROKER_VERSION_ERROR_MESSAGE);
                return errorBundle;
            }

            @Override
            public Type getType() {
                return Type.CONTENT_PROVIDER;
            }
        });

        return strategies;
    }
}
