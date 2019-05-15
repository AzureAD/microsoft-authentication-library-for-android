package com.microsoft.identity.client;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.internal.controllers.BrokerMsalController;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.common.internal.dto.AccountRecord;

public class SingleAccountPublicClientApplication extends PublicClientApplication
    implements ISingleAccountPublicClientApplication {
    private static final String TAG = SingleAccountPublicClientApplication.class.getSimpleName();

    private AccountRecord mLocalAccountRecord;

    protected SingleAccountPublicClientApplication(@NonNull final Context context,
                                                   @NonNull final PublicClientApplicationConfiguration developerConfig) {
        super(context, developerConfig);
    }

    protected SingleAccountPublicClientApplication(@NonNull final Context context,
                                                   @NonNull final String clientId) {
        super(context, clientId);
    }

    protected SingleAccountPublicClientApplication(@NonNull final Context context,
                                                   @NonNull final String clientId,
                                                   @NonNull final String authority) {
        super(context, clientId, authority);
    }

    @Override
    public void getCurrentAccount(final CurrentAccountListener listener) throws MsalClientException {
        final String methodName = ":getCurrentAccount";
        final PublicClientApplicationConfiguration configuration = getConfiguration();

        if (!MSALControllerFactory.brokerEligible(
                configuration.getAppContext(),
                configuration.getDefaultAuthority(),
                configuration)) {
            final String errorMessage = "This request is not eligible to use the broker.";
            com.microsoft.identity.common.internal.logging.Logger.error(TAG + methodName, errorMessage, null);
            throw new MsalClientException(MsalClientException.BROKER_NOT_INSTALLED, errorMessage);
        }

        new BrokerMsalController().getCurrentAccount(
                configuration,
                new BrokerMsalController.GetCurrentAccountRecordFromBrokerCallback() {
                    @Override
                    public void onAccountLoaded(@Nullable final AccountRecord accountRecordInBroker) {
                        IAccount localAccount = mLocalAccountRecord == null ? null : AccountAdapter.adapt(mLocalAccountRecord);
                        IAccount accountInBroker = accountRecordInBroker == null ? null : AccountAdapter.adapt(accountRecordInBroker);

                        if (mLocalAccountRecord == null){
                            if (accountRecordInBroker != null) {
                                listener.onAccountChanged(null, accountInBroker);
                            }
                        } else if (!mLocalAccountRecord.equals(accountRecordInBroker)) {
                            listener.onAccountChanged(localAccount, accountInBroker);
                        }

                        mLocalAccountRecord = accountRecordInBroker;
                        listener.onAccountLoaded(accountInBroker);
                    }
                });
    }

    @Override
    public void removeCurrentAccount(final AccountRemovedListener callback) throws MsalClientException {
        final String methodName = ":removeCurrentAccount";
        final PublicClientApplicationConfiguration configuration = getConfiguration();

        if (!MSALControllerFactory.brokerEligible(
                configuration.getAppContext(),
                configuration.getDefaultAuthority(),
                configuration)) {
            final String errorMessage = "This request is not eligible to use the broker.";
            com.microsoft.identity.common.internal.logging.Logger.error(TAG + methodName, errorMessage, null);
            throw new MsalClientException(MsalClientException.BROKER_NOT_INSTALLED, errorMessage);
        }

        new BrokerMsalController().removeAccountFromSharedDevice(
                AccountAdapter.adapt(mLocalAccountRecord),
                configuration,
                new AccountRemovedListener() {
                    @Override
                    public void onAccountRemoved(Boolean isSuccess) {
                        if (isSuccess) {
                            mLocalAccountRecord = null;
                        }

                        callback.onAccountRemoved(isSuccess);
                    }
                });
    }
}
