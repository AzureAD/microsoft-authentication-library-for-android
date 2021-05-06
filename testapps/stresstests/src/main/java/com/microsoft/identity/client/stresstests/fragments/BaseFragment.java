package com.microsoft.identity.client.stresstests.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.stresstests.R;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static android.os.Looper.getMainLooper;

public abstract class BaseFragment extends Fragment {

    private IPublicClientApplication mApp;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_base, container, false);
    }


    public abstract @LayoutRes
    int getLayoutResource();

    public abstract void runTest();


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showMessage("Something went wrong");
    }

    private void createApplication() {
        // Provide secret key for token encryption
        try {
            if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
                SecretKeyFactory keyFactory = SecretKeyFactory
                        .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
                SecretKey tempKey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                        "UTF-8".getBytes("abcdedfdfd"), 100, 256));
                SecretKey secretKey = new SecretKeySpec(tempKey.getEncoded(), "AES");
                AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | UnsupportedEncodingException e) {
            showMessage("Failed to generate secret key: " + e.getMessage());
        }

        PublicClientApplication.create(getContext(), R.raw.msal_config_default, new IPublicClientApplication.ApplicationCreatedListener() {
            @Override
            public void onCreated(IPublicClientApplication application) {
                mApp = application;
            }

            @Override
            public void onError(MsalException exception) {
                showMessage("Failed to load MSAL Application: " + exception.getMessage());
            }
        });
    }

    private void showMessage(final String msg) {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(getView(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
