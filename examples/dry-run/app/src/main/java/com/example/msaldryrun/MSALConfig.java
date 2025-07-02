package com.example.msaldryrun;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MSALConfig {
    private static final String TAG = MSALConfig.class.getSimpleName();
    private static final String CONFIG_FILE = "auth_config.json";

    private String clientId;
    private String authority;
    private String redirectUri;
    private String accountMode;

    private static MSALConfig INSTANCE;

    private MSALConfig() {}

    public static synchronized MSALConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MSALConfig();
        }
        return INSTANCE;
    }

    public boolean load(Context context) {
        try {
            String jsonString = readConfigFile(context);
            JSONObject config = new JSONObject(jsonString);

            clientId = config.getString("client_id");
            authority = config.getString("authority");
            redirectUri = config.getString("redirect_uri");
            accountMode = config.getString("account_mode");

            return true;
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed to load config file: " + e.getMessage());
            return false;
        }
    }

    private String readConfigFile(Context context) throws IOException {
        String json;
        try (InputStream is = context.getAssets().open(CONFIG_FILE)) {
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            json = new String(buffer, StandardCharsets.UTF_8);
        }
        return json;
    }

    // Getters
    public String getClientId() {
        return clientId;
    }

    public String getAuthority() {
        return authority;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getAccountMode() {
        return accountMode;
    }

    public boolean isSingleAccountMode() {
        return "SINGLE".equalsIgnoreCase(accountMode);
    }
}
