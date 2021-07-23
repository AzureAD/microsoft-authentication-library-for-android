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

package com.microsoft.identity.client.internal;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsService;

import com.microsoft.identity.client.BrowserTabActivity;
import com.microsoft.identity.client.CurrentTaskBrowserTabActivity;
import com.microsoft.identity.client.exception.MsalArgumentException;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.common.logging.Logger;
import com.microsoft.identity.common.internal.configuration.LibraryConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import java.util.concurrent.TimeUnit;

import static com.microsoft.identity.common.internal.util.StringUtil.convertSetToString;

/**
 * Internal Util class for MSAL.
 */
public final class MsalUtils {
    /**
     * The encoding scheme the sdk uses.
     */
    public static final String ENCODING_UTF8 = "UTF-8";

    /**
     * Default access token expiration time in seconds.
     */
    public static final int DEFAULT_EXPIRATION_TIME_SEC = 3600;

    private static final String TAG = MsalUtils.class.getSimpleName();

    private static final String TOKEN_HASH_ALGORITHM = "SHA256";

    public static final String CHROME_PACKAGE = "com.android.chrome";
    public static final String QUERY_STRING_SYMBOL = "?";
    public static final String QUERY_STRING_DELIMITER = "&";

    /**
     * Private constructor to prevent Util class from being initiated.
     */
    private MsalUtils() {
    }

    /**
     * To improve test-ability with local Junit. Android.jar used for local Junit doesn't have a default implementation
     * for {@link android.text.TextUtils}.
     */
    @SuppressWarnings("PMD.InefficientEmptyStringCheck")
    public static boolean isEmpty(final String message) {
        return message == null || message.trim().length() == 0;
    }

    /**
     * Throws IllegalArgumentException if the argument is null.
     */
    public static void validateNonNullArgument(@Nullable final Object o,
                                               @NonNull final String argName) {
        if (null == o
                || (o instanceof CharSequence) && TextUtils.isEmpty((CharSequence) o)) {
            throw new IllegalArgumentException(
                    argName
                            + " cannot be null or empty"
            );
        }
    }

    /**
     * Throws MsalArgumentException if the argument is null or empty
     *
     * @param o
     * @param argName
     * @throws MsalArgumentException
     */
    public static void validateNonNullArg(@Nullable final Object o,
                                          @NonNull final String argName) throws MsalArgumentException {
        if (null == o
                || (o instanceof CharSequence) && TextUtils.isEmpty((CharSequence) o)
                || (o instanceof List) && ((List) o).isEmpty()) {
            throw new MsalArgumentException(argName, argName + " cannot be null or empty");
        }
    }

    /**
     * Translate the given string into the application/x-www-form-urlencoded using the utf_8 encoding scheme(The World
     * Wide Web Consortium Recommendation states that UTF-8 should be used. Not doing so may introduce incompatibilites.).
     *
     * @param stringToEncode The String to encode.
     * @return The url encoded string.
     * @throws UnsupportedEncodingException If the named encoding is not supported.
     */
    public static String urlFormEncode(final String stringToEncode) throws UnsupportedEncodingException {
        if (isEmpty(stringToEncode)) {
            return "";
        }

        return URLEncoder.encode(stringToEncode, ENCODING_UTF8);
    }

    /**
     * Perform URL decode on the given source.
     *
     * @param source The String to decode for.
     * @return The decoded string.
     * @throws UnsupportedEncodingException If encoding is not supported.
     */
    public static String urlFormDecode(final String source) throws UnsupportedEncodingException {
        if (isEmpty(source)) {
            return "";
        }

        return URLDecoder.decode(source, ENCODING_UTF8);
    }

    /**
     * Return the unmodifiable Map of response items.
     * If the input jsonString is empty or blank, it't not in the correct json format, JsonException will be thrown.
     */
    public static Map<String, String> extractJsonObjectIntoMap(final String jsonString)
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
     *
     * @param expiresIn The given expires in that is used to calculate the expires on.
     * @return The date that the token will be expired.
     */
    public static Date calculateExpiresOn(final String expiresIn) {
        final Calendar expires = new GregorianCalendar();
        // Compute token expiration
        expires.add(Calendar.SECOND, getExpiryOrDefault(expiresIn));

        return expires.getTime();
    }

    public static int getExpiryOrDefault(String expiresIn) {
        return isEmpty(expiresIn) ? DEFAULT_EXPIRATION_TIME_SEC : Integer.parseInt(expiresIn);
    }

    /**
     * Converts the given string of scopes into set. The input String of scopes is delimited by " ".
     *
     * @param scopes The scopes in the format of string, delimited by " ".
     * @return Converted scopes in the format of set.
     */
    public static Set<String> getScopesAsSet(final String scopes) {
        if (MsalUtils.isEmpty(scopes)) {
            return new HashSet<>();
        }

        final String[] scopeArray = scopes.toLowerCase(Locale.US).split(" ");
        final Set<String> resultSet = new HashSet<>();
        for (int i = 0; i < scopeArray.length; i++) {
            if (!MsalUtils.isEmpty(scopeArray[i])) {
                resultSet.add(scopeArray[i]);
            }
        }

        return resultSet;
    }

    /**
     * Ensures that the developer has properly configured their
     * AndroidManifest to expose the BrowserTabActivity.
     *
     * @param context the context of the application
     * @param url     the redirect uri of the app
     * @return a boolean indicating if BrowserTabActivity is configured or not
     */
    @Deprecated
    public static boolean hasCustomTabRedirectActivity(@NonNull final Context context,
                                                       @NonNull final String url) {
        final PackageManager packageManager = context.getPackageManager();

        if (packageManager == null) {
            return false;
        }

        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setDataAndNormalize(Uri.parse(url));

        final List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(
                intent,
                PackageManager.GET_RESOLVED_FILTER
        );

        // resolve info list will never be null, if no matching activities are found, empty list will be returned.
        boolean hasActivity = false;


        //Current default activity for use with authorization agent "DEFAULT" or "BROWSER"
        String activityClassName = BrowserTabActivity.class.getName();

        //If we're using authorization in current task... then we need to look for that activity
        if(LibraryConfiguration.getInstance().isAuthorizationInCurrentTask()){
            activityClassName = CurrentTaskBrowserTabActivity.class.getName();
        }

        for (final ResolveInfo info : resolveInfoList) {
            final ActivityInfo activityInfo = info.activityInfo;

            if (activityInfo.name.equals(activityClassName))  {
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
     *
     * @param context The app {@link Context} to check for the package existence.
     * @return The available package name for chrome. Will return null if no chrome package existed on the device.
     */
    public static String getChromePackageWithCustomTabSupport(final Context context) {
        if (context.getPackageManager() == null) {
            com.microsoft.identity.common.internal.logging.Logger.warn(
                    TAG,
                    "getPackageManager() returned null."
            );
            return null;
        }

        final Intent customTabServiceIntent = new Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
        final List<ResolveInfo> resolveInfoList = context.getPackageManager().queryIntentServices(
                customTabServiceIntent, 0);

        // queryIntentServices could return null or an empty list if no matching service existed.
        if (resolveInfoList == null || resolveInfoList.isEmpty()) {
            com.microsoft.identity.common.internal.logging.Logger.warn(
                    TAG,
                    "No Service responded to Intent: " + CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
            );
            return null;
        }

        for (final ResolveInfo resolveInfo : resolveInfoList) {
            final ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if (serviceInfo != null && CHROME_PACKAGE.equals(serviceInfo.packageName)) {
                return serviceInfo.packageName;
            }
        }

        com.microsoft.identity.common.internal.logging.Logger.warn(
                TAG,
                "No pkg with CustomTab support found."
        );

        return null;
    }

    /**
     * CHROME_PACKAGE array contains all the chrome packages that is currently available on play store, we will only support
     * chrome stable.
     *
     * @param context The app context that is used to check the chrome packages.
     * @return The chrome package name that exists on the device.
     */
    public static String getChromePackage(final Context context) {
        final PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            return null;
        }

        String installedChromePackage = null;
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(CHROME_PACKAGE, PackageManager.GET_ACTIVITIES);
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            if (applicationInfo != null && applicationInfo.enabled) {
                installedChromePackage = CHROME_PACKAGE;
            }
        } catch (final PackageManager.NameNotFoundException e) {
            // swallow this exception. If the package is not existed, the exception will be thrown.
            com.microsoft.identity.common.internal.logging.Logger.error(
                    TAG,
                    "Failed to retrieve chrome package info.",
                    e
            );
        }

        return installedChromePackage;
    }

    /**
     * Decode the given url, and convert it into map with the given delimiter.
     *
     * @param url       The url to decode for.
     * @param delimiter The delimiter used to parse the url string.
     * @return The Map of the items decoded with the given delimiter.
     */
    public static Map<String, String> decodeUrlToMap(final String url, final String delimiter) {
        final Map<String, String> decodedUrlMap = new HashMap<>();

        // delimiter can be " "
        if (MsalUtils.isEmpty(url) || delimiter == null) {
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
                final String key = urlFormDecode(elements[0]);
                final String value = urlFormDecode(elements[1]);

                if (!MsalUtils.isEmpty(key) && !MsalUtils.isEmpty(value)) {
                    decodedUrlMap.put(key, value);
                }
            } catch (final UnsupportedEncodingException e) {
                com.microsoft.identity.common.internal.logging.Logger.errorPII(
                        TAG,
                        "URL form decode failed.",
                        e
                );
            }
        }

        return decodedUrlMap;
    }

    /**
     * Append parameter to the url. If the no query parameters, return the url originally passed in.
     */
    public static String appendQueryParameterToUrl(final String url, final Map<String, String> requestParams)
            throws UnsupportedEncodingException {
        if (MsalUtils.isEmpty(url)) {
            throw new IllegalArgumentException("Empty authority string");
        }

        if (requestParams.isEmpty()) {
            return url;
        }

        final Set<String> queryParamsSet = new HashSet<>();
        for (Map.Entry<String, String> entry : requestParams.entrySet()) {
            queryParamsSet.add(entry.getKey() + "=" + urlFormEncode(entry.getValue()));
        }

        final String queryString = convertSetToString(queryParamsSet, QUERY_STRING_DELIMITER);
        final String queryStringFormat;
        if (url.contains(QUERY_STRING_SYMBOL)) {
            queryStringFormat = url.endsWith(QUERY_STRING_DELIMITER) ? "%s%s" : "%s" + QUERY_STRING_DELIMITER + "%s";
        } else {
            queryStringFormat = "%s" + QUERY_STRING_SYMBOL + "%s";
        }

        return String.format(queryStringFormat, url, queryString);
    }

    public static String base64UrlEncodeToString(final String message) {
        return Base64.encodeToString(message.getBytes(Charset.forName(ENCODING_UTF8)), Base64.URL_SAFE | Base64.NO_WRAP);
    }

    /**
     * @return True if there is an intersection between the scopes stored in the token cache key and the request scopes.
     */
    public static boolean isScopeIntersects(final Set<String> scopes, final Set<String> otherScopes) {
        for (final String scope : otherScopes) {
            if (scopes.contains(scope)) {
                return true;
            }
        }

        return false;
    }

    public static String createHash(String msg) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        if (!isEmpty(msg)) {
            MessageDigest digester = MessageDigest.getInstance(TOKEN_HASH_ALGORITHM);
            final byte[] msgInBytes = msg.getBytes(ENCODING_UTF8);
            return new String(Base64.encode(digester.digest(msgInBytes), Base64.NO_WRAP), ENCODING_UTF8);
        }
        return msg;
    }

    /**
     * create url from given endpoint. return null if format is not right.
     *
     * @param endpoint url as a string
     * @return URL object for this string
     */
    public static URL getUrl(String endpoint) {
        URL url = null;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e1) {
            com.microsoft.identity.common.internal.logging.Logger.errorPII(
                    TAG,
                    "Url is invalid",
                    e1
            );
        }

        return url;
    }

    public static String getUniqueUserIdentifier(final String uid, final String utid) {
        return base64UrlEncodeToString(uid) + "." + base64UrlEncodeToString(utid);
    }

    public static long getExpiresOn(long expiresIn) {
        final long currentTimeMillis = System.currentTimeMillis();
        final long currentTimeSecs = TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis);
        return currentTimeSecs + expiresIn;
    }

    public static ApplicationInfo getApplicationInfo(final Context context) {
        final ApplicationInfo applicationInfo;
        try {
            applicationInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("Unable to find the package info, unable to proceed");
        }

        return applicationInfo;
    }

    public static Set<String> convertArrayToSet(final String[] values) {
        final Set<String> convertedSet = new HashSet<>();
        if (values == null) {
            return convertedSet;
        }

        for (int i = 0; i < values.length; i++) {
            if (!MsalUtils.isEmpty(values[i])) {
                convertedSet.add(values[i]);
            }
        }

        return convertedSet;
    }

    /**
     * @param methodName
     */
    public static void throwOnMainThread(final String methodName) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("method: " + methodName + " may not be called from main thread.");
        }
    }
}
