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

package com.microsoft.identity.client;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Internal Util class for MSAL.
 */
final class MSALUtils {
    public static final String ENCODING_UTF8 = "UTF_8";
    /** Default access token expiration time in seconds. */
    public static final int DEFAULT_EXPIRATION_TIME_SEC = 3600;

    private static final String CUSTOM_TABS_SERVICE_ACTION =
            "android.support.customtabs.action.CustomTabsService";

    private static final String[] CHROME_PACKAGES = {
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
    };

    /**
     * Private constructor to prevent Util class from being initiated.
     */
    private MSALUtils() { }

    /**
     * To improve test-ability with local Junit. Android.jar used for local Junit doesn't have a default implementation
     * for {@link android.text.TextUtils}.
     */
    static boolean isEmpty(final String message) {
        return message == null || message.trim().length() == 0;
    }

    static String urlEncode(final String stringToEncode) throws UnsupportedEncodingException {
        return URLEncoder.encode(stringToEncode, ENCODING_UTF8);
    }

    /**
     * Return the unmodifiable Map of response items.
     */
    static Map<String, String> extractJsonObjectIntoMap(final String jsonString)
            throws JSONException{
        final JSONObject jsonObject = new JSONObject(jsonString);
        final Iterator<String> keyIterator = jsonObject.keys();

        final Map<String, String> responseItems = new HashMap<>();
        while (keyIterator.hasNext()) {
            final String key = keyIterator.next();
            responseItems.put(key, jsonObject.getString(key));
        }

        return Collections.unmodifiableMap(responseItems);
    }

    static Date calculateExpiresOn(final String expiresIn) {
        final Calendar expires = new GregorianCalendar();

        // Compute token expiration
        expires.add(
                Calendar.SECOND,
                expiresIn == null || expiresIn.isEmpty() ? DEFAULT_EXPIRATION_TIME_SEC
                        : Integer.parseInt(expiresIn));

        return expires.getTime();
    }

    static Set<String> getScopesAsSet(final String scopes) {
        if (MSALUtils.isEmpty(scopes)) {
            return new HashSet<>();
        }

        final String[] scopeArray = scopes.toLowerCase(Locale.US).split(" ");
        return new HashSet<>(Arrays.asList(scopeArray));
    }

    static boolean hasCustomTwbRedirectActivity(final Context context, final String redirectUri) {
        final PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            return false;
        }

        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(redirectUri));
        final List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent,
                PackageManager.GET_RESOLVED_FILTER);
        if (resolveInfos == null) {
            return false;
        }

        for (final ResolveInfo resolveInfo : resolveInfos) {
            final ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (!activityInfo.name.equals(CustomTabActivity.class.getName())) {
                // TODO: add logs
                // Another application is listening for this url scheme, don't open custom tabs in this case for
                // security reason.
                return false;
            }
        }

        return true;
    }

    static String getChromePackages(final Context context) {
        final Intent customTabServiceIntent = new Intent(CUSTOM_TABS_SERVICE_ACTION);
        final List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentServices(
                customTabServiceIntent, 0);

        if (resolveInfos == null) {
            // TODO: add logs
            return null;
        }

        final Set<String> chromePackage = new HashSet<>(Arrays.asList(CHROME_PACKAGES));
        for (final ResolveInfo resolveInfo : resolveInfos) {
            final ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if (serviceInfo != null && chromePackage.contains(serviceInfo.packageName)) {
                return serviceInfo.packageName;
            }
        }

        return null;
    }

    static Map<String, String> decodeUrlToMap(final String url, final String delimiter) {
        final Map<String, String> decodedUrlMap = new HashMap<>();

        if (MSALUtils.isEmpty(url)) {
            return decodedUrlMap;
        }

        final StringTokenizer tokenizer = new StringTokenizer(url, delimiter);
        while (tokenizer.hasMoreTokens()) {
            final String pair = tokenizer.nextToken();
            final String[] elements = pair.split("=");

            if (elements.length != 2) {
                continue;
            }

            try {
                final String key = urlDecodeString(elements[0]);
                final String value = urlDecodeString(elements[1]);

                if (!MSALUtils.isEmpty(key) && !MSALUtils.isEmpty(value)) {
                    decodedUrlMap.put(key, value);
                }
            } catch (final UnsupportedEncodingException e) {
                // TODO: log here.
            }
        }

        return decodedUrlMap;
    }

    static String urlDecodeString(final String source) throws UnsupportedEncodingException {
        return URLDecoder.decode(source, ENCODING_UTF8);
    }

    static String convertSetToString(final Set<String> inputSet, final String delimiter) {
        if (inputSet == null || inputSet.isEmpty()) {
            return "";
        }

        final StringBuilder stringBuilder = new StringBuilder();
        final Iterator<String> iterator = inputSet.iterator();
        stringBuilder.append(iterator.next());

        while (iterator.hasNext()) {
            stringBuilder.append(" ");
            stringBuilder.append(iterator.next());
        }

        return stringBuilder.toString();
    }
}
