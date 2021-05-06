package com.microsoft.identity.client.stresstests.fragments;

import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.stresstests.INotifyOperationResultCallback;

import java.util.Arrays;

public class AcquireTokenTestsFragment extends StressTestsFragment<IAuthenticationResult, IAuthenticationResult> {

    @Override
    public String getTitle() {
        return "AcquireTokenSilent Stress Tests";
    }

    @Override
    public void prepare(IPublicClientApplication application, final INotifyOperationResultCallback<IAuthenticationResult> callback) {
        application.acquireToken(getActivity(), new String[]{"user.read"}, new AuthenticationCallback() {
            @Override
            public void onCancel() {
                callback.onError("User cancelled the flow.");
            }

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                callback.onSuccess(authenticationResult);
            }

            @Override
            public void onError(MsalException exception) {
                callback.onError("Error acquiring token: " + exception.getMessage());
            }
        });
    }

    @Override
    public void run(IAuthenticationResult prerequisites, IPublicClientApplication application, final INotifyOperationResultCallback<IAuthenticationResult> callback) {
        AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(prerequisites.getAccount())
                .withScopes(Arrays.asList(prerequisites.getScope()))
                .withCallback(new SilentAuthenticationCallback() {
                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        callback.onSuccess(authenticationResult);
                    }

                    @Override
                    public void onError(MsalException exception) {
                        callback.onError("Error acquiring token silent: " + exception.getMessage());
                    }
                }).build();

        application.acquireTokenSilentAsync(parameters);
    }

    @Override
    public int getNumberOfThreads() {
        return 10;
    }

    @Override
    public int getTimeLimit() {
        return 6 * 60;
    }
}
