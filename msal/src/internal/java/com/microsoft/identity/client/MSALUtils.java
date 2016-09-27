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
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
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
    /**
     * The encoding scheme the sdk uses.
     */
    public static final String ENCODING_UTF8 = "UTF_8";

    /** Default access token expiration time in seconds. */
    public static final int DEFAULT_EXPIRATION_TIME_SEC = 3600;

    private static final String CUSTOM_TABS_SERVICE_ACTION =
            "android.support.customtabs.action.CustomTabsService";

    static final String[] CHROME_PACKAGES = {
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

    /**
     * Translate the given string into the application/x-www-form-urlencoded using the utf_8 encoding scheme(The World
     * Wide Web Consortium Recommendation states that UTF-8 should be used. Not doing so may introduce incompatibilites.).
     * @param stringToEncode The String to encode.
     * @return The url encoded string.
     * @throws UnsupportedEncodingException If the named encoding is not supported.
     */
    static String urlEncode(final String stringToEncode) throws UnsupportedEncodingException {
        if (isEmpty(stringToEncode)) {
            return "";
        }

        return URLEncoder.encode(stringToEncode, ENCODING_UTF8);
    }

    /**
     * Perform URL decode on the given source.
     * @param source The String to decode for.
     * @return The decoded string.
     * @throws UnsupportedEncodingException If encoding is not supported.
     */
    static String urlDecode(final String source) throws UnsupportedEncodingException {
        if (isEmpty(source)) {
            return "";
        }

        return URLDecoder.decode(source, ENCODING_UTF8);
    }

    /**
     * Return the unmodifiable Map of response items.
     * If the input jsonString is empty or blank, it't not in the correct json format, JsonException will be thrown.
     */
    static Map<String, String> extractJsonObjectIntoMap(final String jsonString)
            throws JSONException {

        final JSONObject jsonObject = new JSONObject(jsonString);
        final Iterator<String> keyIterator = jsonObject.keys();

        final Map<String, String> responseItems = new HashMap<>();
        while (keyIterator.hasNext()) {
            final String key = keyIterator.next();
            responseItems.put(key, jsonObject.getString(key));
        }

        return Collections.unmodifiableMap(responseItems);
    }

    /**
     * Calculate expires on based on given exipres in. Data will hold date in milliseconds.
     * @param expiresIn The given expires in that is used to calculate the expires on.
     * @return The date that the token will be expired.
     */
    static Date calculateExpiresOn(final String expiresIn) {
        final Calendar expires = new GregorianCalendar();
        // Compute token expiration
        expires.add(Calendar.SECOND, isEmpty(expiresIn) ? DEFAULT_EXPIRATION_TIME_SEC : Integer.parseInt(expiresIn));

        return expires.getTime();
    }

    /**
     * Converts the given string of scopes into set. The input String of scopes is delimited by " ".
     * @param scopes The scopes in the format of string, delimited by " ".
     * @return Converted scopes in the format of set.
     */
    static Set<String> getScopesAsSet(final String scopes) {
        if (MSALUtils.isEmpty(scopes)) {
            return new HashSet<>();
        }

        final String[] scopeArray = scopes.toLowerCase(Locale.US).split(" ");
        final Set<String> resultSet = new HashSet<>();
        for (int i = 0; i < scopeArray.length; i++) {
            if (!MSALUtils.isEmpty(scopeArray[i])) {
                resultSet.add(scopeArray[i]);
            }
        }

        return resultSet;
    }

    static boolean hasCustomTabRedirectActivity(final Context context, final String url) {
        final PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            return false;
        }

        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setDataAndNormalize(Uri.parse(url));
        final List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent,
                PackageManager.GET_RESOLVED_FILTER);

        // resolve info list will never be null, if no matching activities are found, empty list will be returned.
        boolean hasActivity = false;
        for (ResolveInfo info : resolveInfoList) {
            ActivityInfo activityInfo = info.activityInfo;
            if (activityInfo.name.equals(BrowserTabActivity.class.getName())) {
                hasActivity = true;
            } else {
                // another application is listening for this url scheme, don't open
                // Custom Tab for security reasons
                return false;
            }
        }

        return hasActivity;
    }

    /**
     * Check if the chrome package with custom tab support is available on the device, and return the package name if
     * available.
     * @param context The app {@link Context} to check for the package existence.
     * @return The available package name for chrome. Will return null if no chrome package existed on the device.
     */
    static String getChromePackageWithCustomTabSupport(final Context context) {
        if (context.getPackageManager() == null) {
            return null;
        }

        final Intent customTabServiceIntent = new Intent(CUSTOM_TABS_SERVICE_ACTION);
        final List<ResolveInfo> resolveInfoList = context.getPackageManager().queryIntentServices(
                customTabServiceIntent, 0);

        // queryIntentServices could return null or an empty list if no matching service existed.
        if (resolveInfoList == null || resolveInfoList.isEmpty()) {
            // TODO: add logs
            return null;
        }

        final Set<String> chromePackage = new HashSet<>(Arrays.asList(CHROME_PACKAGES));
        for (final ResolveInfo resolveInfo : resolveInfoList) {
            final ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if (serviceInfo != null && chromePackage.contains(serviceInfo.packageName)) {
                return serviceInfo.packageName;
            }
        }

        return null;
    }

    /**
     * CHROME_PACKAGES array contains all the chrome packages that is currently available on play store, we always check
     * the chrome packages in the order of 1)the currently stable one com.android.chrome 2) beta version com.chrome.beta
     * 3) the dev version com.chrome.dev.
     * @param context The app context that is used to check the chrome packages.
     * @return The chrome package name that exists on the device.
     */
    static String getChromePackage(final Context context) {
        final PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            return null;
        }

        String installedChromePackage = null;
        for (int i = 0; i < CHROME_PACKAGES.length; i++) {
            try {
                packageManager.getPackageInfo(CHROME_PACKAGES[i], PackageManager.GET_ACTIVITIES);
                installedChromePackage = CHROME_PACKAGES[i];
                break;
                //CHECKSTYLE:OFF: checkstyle:EmptyBlock
            } catch (final PackageManager.NameNotFoundException e) {
                //CHECKSTYLE:ON: checkstyle:EmptyBlock
                // swallow this exception. If the package is not existed, the exception will be thrown.
            }
        }

        return installedChromePackage;
    }

    /**
     * Decode the given url, and convert it into map with the given delimiter.
     * @param url The url to decode for.
     * @param delimiter The delimiter used to parse the url string.
     * @return The Map of the items decoded with the given delimiter.
     */
    static Map<String, String> decodeUrlToMap(final String url, final String delimiter) {
        final Map<String, String> decodedUrlMap = new HashMap<>();

        // delimiter can be " "
        if (MSALUtils.isEmpty(url) || delimiter == null) {
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
                final String key = urlDecode(elements[0]);
                final String value = urlDecode(elements[1]);

                if (!MSALUtils.isEmpty(key) && !MSALUtils.isEmpty(value)) {
                    decodedUrlMap.put(key, value);
                }
                //CHECKSTYLE:OFF: checkstyle:EmptyBlock
            } catch (final UnsupportedEncodingException e) {
                //CHECKSTYLE:ON: checkstyle:EmptyBlock
                // TODO: log here.
            }
        }

        return decodedUrlMap;
    }

    /**
     * Convert the given set of scopes into the string with the provided delimiter.
     * @param inputSet The Set of scopes to convert.
     * @param delimiter The delimiter used to construct the scopes in the format of String.
     * @return The converted scopes in the format of String.
     */
    static String convertSetToString(final Set<String> inputSet, final String delimiter) {
        if (inputSet == null || inputSet.isEmpty() || delimiter == null) {
            return "";
        }

        final StringBuilder stringBuilder = new StringBuilder();
        final Iterator<String> iterator = inputSet.iterator();
        stringBuilder.append(iterator.next());

        while (iterator.hasNext()) {
            stringBuilder.append(delimiter);
            stringBuilder.append(iterator.next());
        }

        return stringBuilder.toString();
    }

    static String appendQueryParameterToUrl(final String url,
                                            final Map<String, String> requestParams)
            throws UnsupportedEncodingException {
        final Set<String> queryParamsSet = new HashSet<>();
        for (Map.Entry<String, String> entry : requestParams.entrySet()) {
            queryParamsSet.add(entry.getKey() + "=" + urlEncode(entry.getValue()));
        }

        final String queryString = queryParamsSet.isEmpty() ? ""
                : convertSetToString(queryParamsSet, "&");

        return String.format("%s?%s", url, queryString);
    }

    static String base64EncodeToString(final String message) {
        return  Base64.encodeToString(message.getBytes(Charset.forName(ENCODING_UTF8)), Base64.NO_PADDING);
    }
}
