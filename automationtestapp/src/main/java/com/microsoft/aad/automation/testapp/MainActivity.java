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
                    onAcquireTokenClicked();
                    break;
                case acquireTokenSilent:
                    onAcquireTokenSilentClicked();
                    break;
                case expireAccessToken:
                    onExpireAccessTokenClicked();
                    break;
                case invalidateRefreshToken:
                    onInvalidateRefreshTokenClicked();
                    break;
                case invalidateFamilyRefreshToken:
                    onInvalidateFamilyRefreshTokenClicked();
                    break;
                case readCache:
                    onReadCacheClicked();
                    break;
                case clearCache:
                    onClearCacheClicked();
                    break;
            }
        }
    };

    private void onAcquireTokenClicked() {
        // TODO
    }

    private void onAcquireTokenSilentClicked() {
        // TODO
    }

    private void onExpireAccessTokenClicked() {
        // TODO
    }

    private void onInvalidateRefreshTokenClicked() {
        // TODO
    }

    private void onInvalidateFamilyRefreshTokenClicked() {
        // TODO
    }

    private void onReadCacheClicked() {
        // TODO
    }

    private void onClearCacheClicked() {
        // TODO
    }
}
