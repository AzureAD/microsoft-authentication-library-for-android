package com.microsoft.identity.client.stresstests.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.stresstests.INotifyOperationResultCallback;
import com.microsoft.identity.client.stresstests.R;
import com.microsoft.identity.common.internal.result.ResultFuture;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AcquireTokenTestsFragment extends StressTestsFragment<IAccount, IAuthenticationResult> {

    private static final String TAG = AcquireTokenTestsFragment.class.getSimpleName();

    private static final String[] scopes = new String[]{"user.read"};

    @Override
    public String getTitle() {
        return "AcquireTokenSilent Stress Tests";
    }

    private void showAccountPicker(final List<IAccount> accounts, final INotifyOperationResultCallback<IAccount> callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_menu_manage)
                .setTitle("Choose Account");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.select_dialog_item);
        for (IAccount account : accounts) {
            arrayAdapter.add(account.getUsername());
        }
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                callback.onError("Did not choose an account.");
            }
        });

        builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.onSuccess(accounts.get(which));
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private IAccount getExistingAccount(IPublicClientApplication application) throws ExecutionException, InterruptedException {
        final String methodName = ":getExistingAccount";

        final ResultFuture<IAccount> resultFuture = new ResultFuture<>();

        if (application instanceof ISingleAccountPublicClientApplication) {
            ((ISingleAccountPublicClientApplication) application).getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
                @Override
                public void onAccountLoaded(@Nullable IAccount activeAccount) {
                    resultFuture.setResult(activeAccount);
                }

                @Override
                public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
                    Log.e(TAG + methodName, "signed-in account is changed from "
                            + (null == priorAccount ? "null" : priorAccount.getUsername())
                            + " to "
                            + (null == currentAccount ? "null" : currentAccount.getUsername()));
                    resultFuture.setResult(null);
                }

                @Override
                public void onError(@NonNull MsalException exception) {
                    resultFuture.setResult(null);
                    Log.e(TAG + methodName, exception.getMessage(), exception);
                }
            });
        } else {
            ((IMultipleAccountPublicClientApplication) application).getAccounts(new IPublicClientApplication.LoadAccountsCallback() {
                @Override
                public void onTaskCompleted(List<IAccount> result) {
                    showAccountPicker(result, new INotifyOperationResultCallback<IAccount>() {
                        @Override
                        public void onSuccess(IAccount result) {
                            resultFuture.setResult(result);
                        }

                        @Override
                        public void onError(String message) {
                            resultFuture.setResult(null);
                            Log.d(TAG + methodName, message);
                        }
                    });
                }

                @Override
                public void onError(MsalException exception) {
                    resultFuture.setResult(null);
                    Log.e(TAG + methodName, exception.getMessage(), exception);
                }
            });
        }

        return resultFuture.get();
    }

    @Override
    public void prepare(IPublicClientApplication application, final INotifyOperationResultCallback<IAccount> callback) {
        final String methodName = ":prepare";

        IAccount account = null;

        try {
            account = getExistingAccount(application);
        } catch (ExecutionException | InterruptedException exception) {
            Log.e(TAG + methodName, exception.getMessage(), exception);
        }

        if (account == null) {
            application.acquireToken(getActivity(), scopes, new AuthenticationCallback() {
                @Override
                public void onCancel() {
                    callback.onError("User cancelled the flow.");
                }

                @Override
                public void onSuccess(IAuthenticationResult authenticationResult) {
                    callback.onSuccess(authenticationResult.getAccount());
                }

                @Override
                public void onError(MsalException exception) {
                    callback.onError("Error acquiring token: " + exception.getMessage());
                }
            });
        } else {
            callback.onSuccess(account);
        }
    }

    @Override
    public void run(IAccount account, IPublicClientApplication application, final INotifyOperationResultCallback<IAuthenticationResult> callback) {
        AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(account.getAuthority())
                .withScopes(Arrays.asList(scopes))
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
        // Run these tests for 6 hours.
        return 6 * 60;
    }
}
