package com.microsoft.aad.automation.testapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.microsoft.testapp.R;

import static com.microsoft.testapp.R.id.acquireToken;
import static com.microsoft.testapp.R.id.acquireTokenSilent;
import static com.microsoft.testapp.R.id.clearCache;
import static com.microsoft.testapp.R.id.expireAccessToken;
import static com.microsoft.testapp.R.id.invalidateFamilyRefreshToken;
import static com.microsoft.testapp.R.id.invalidateRefreshToken;
import static com.microsoft.testapp.R.id.readCache;

public class MainActivity extends AppCompatActivity {

    static final int[] sButtonIds = {
            acquireToken,
            acquireTokenSilent,
            expireAccessToken,
            invalidateRefreshToken,
            invalidateFamilyRefreshToken,
            readCache,
            clearCache
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (int viewId : sButtonIds) {
            findViewById(viewId).setOnClickListener(mClickListener);
        }
    }

    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case acquireToken:
                    acquireToken();
                    break;
                case acquireTokenSilent:
                    acquireTokenSilent();
                    break;
                case expireAccessToken:
                    expireAccessToken();
                    break;
                case invalidateRefreshToken:
                    invalidateRefreshToken();
                    break;
                case invalidateFamilyRefreshToken:
                    invalidateFamilyRefreshToken();
                    break;
                case readCache:
                    readCache();
                    break;
                case clearCache:
                    clearCache();
                    break;
            }
        }
    };

    private void acquireToken() {
        // TODO
    }

    private void acquireTokenSilent() {
        // TODO
    }

    private void expireAccessToken() {
        // TODO
    }

    private void invalidateRefreshToken() {
        // TODO
    }

    private void invalidateFamilyRefreshToken() {
        // TODO
    }

    private void readCache() {
        // TODO
    }

    private void clearCache() {
        // TODO
    }
}
