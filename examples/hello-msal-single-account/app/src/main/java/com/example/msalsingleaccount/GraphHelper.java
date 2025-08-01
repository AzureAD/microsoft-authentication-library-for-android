//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.example.msalsingleaccount;

import android.util.Log;
import androidx.annotation.NonNull;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * GraphHelper is a utility class to interact with Microsoft Graph API.
 * It provides a method to call the Graph API and retrieve user information.
 */
public class GraphHelper {
    private static final String TAG = GraphHelper.class.getSimpleName();

    // The URL for the Microsoft Graph API endpoint to get user information
    private static final String GRAPH_URL = "https://graph.microsoft.com/v1.0/me";

    /**
     * Callback interface for handling Graph API responses.
     */
    public interface GraphCallback {
        void onSuccess(JSONObject data);
        void onError(String error);
    }

    /**
     * Calls the Microsoft Graph API to retrieve user information.
     *
     * @param accessToken The OAuth 2.0 access token for authentication.
     * @param callback    The callback to handle the response or error.
     */
    public static void callGraphAPI(@NonNull String accessToken, @NonNull GraphCallback callback) {
        CompletableFuture.runAsync(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(GRAPH_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                connection.setRequestProperty("Accept", "application/json");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    callback.onSuccess(jsonResponse);
                } else {
                    throw new IOException("Error calling Graph API: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calling Graph API", e);
                callback.onError(e.getMessage());
            }

            // Ensure the connection is closed
            if (connection != null) {
                connection.disconnect();
            }
        });
    }
}
