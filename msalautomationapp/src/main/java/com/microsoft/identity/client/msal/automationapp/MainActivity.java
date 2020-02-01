package com.microsoft.identity.client.msal.automationapp;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        Runtime runtime = Runtime.getRuntime();
//        try {
//            runtime.exec("pm clear " + this.getApplicationContext().getPackageName());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public void acquireToken(View view) {
        final Activity activity = this;
        PublicClientApplication.create(getApplicationContext(), R.raw.msal_automation_config, new IPublicClientApplication.ApplicationCreatedListener() {
            @Override
            public void onCreated(IPublicClientApplication application) {
                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withScopes(Arrays.asList(new String[]{"User.read"}))
                        .withCallback(successfulInteractiveCallback(activity.getApplicationContext()))
                        .build();

                application.acquireToken(parameters);
            }

            @Override
            public void onError(MsalException exception) {

            }
        });
    }

    public static AuthenticationCallback successfulInteractiveCallback(final Context context) {
        AuthenticationCallback callback = new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                // do nothing
                Toast.makeText(context, authenticationResult.getAccessToken(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(MsalException exception) {
                // do nothing
            }

            @Override
            public void onCancel() {
                // do nothing
            }
        };

        return callback;
    }
}
