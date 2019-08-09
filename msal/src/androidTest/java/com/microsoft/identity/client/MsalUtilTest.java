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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.util.Base64;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.client.internal.MsalUtils;
import com.microsoft.identity.common.internal.util.StringUtil;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link MsalUtils}.
 */
@RunWith(AndroidJUnit4.class)
public final class MsalUtilTest {

    static final int EXPECTED_SINGLE_SCOPE_SIZE = 1;
    static final int EXPECTED_MULTI_SCOPE_SIZE = 3;
    static final int EXPECTED_SET_SIZE = 3;

    @Before
    public void setUp() {
        System.setProperty(
                "dexmaker.dexcache",
                androidx.test.platform.app.InstrumentationRegistry
                        .getInstrumentation()
                        .getTargetContext()
                        .getCacheDir()
                        .getPath()
        );

        System.setProperty(
                "org.mockito.android.target",
                ApplicationProvider
                        .getApplicationContext()
                        .getCacheDir()
                        .getPath()
        );
    }

    @Test
    public void testNullMessage() {
        Assert.assertTrue(MsalUtils.isEmpty(null));
    }

    @Test
    public void testEmptyMessage() {
        Assert.assertTrue(MsalUtils.isEmpty(""));
    }

    @Test
    public void testBlankMessage() {
        Assert.assertTrue(MsalUtils.isEmpty("  "));
    }

    @Test
    public void testNonEmptyMessage() {
        Assert.assertFalse(MsalUtils.isEmpty("Test"));
    }

    @Test
    public void testEmptyEncodeDecodeString() throws UnsupportedEncodingException {
        Assert.assertTrue(MsalUtils.urlFormEncode("").equals(""));
        Assert.assertTrue(MsalUtils.urlFormDecode("").equals(""));
    }

    @Test
    public void testEncodeDecodeString() throws UnsupportedEncodingException {
        Assert.assertTrue(MsalUtils.urlFormEncode("1 $%&=").equals("1+%24%25%26%3D"));
        Assert.assertTrue(MsalUtils.urlFormDecode("+%24%25%26%3D").equals(" $%&="));
    }

    @Test
    public void testExtractJsonObjectIntoMapNullJsonFormat() {
        try {
            MsalUtils.extractJsonObjectIntoMap("test");
            Assert.fail("Expect Json exception");
            //CHECKSTYLE:OFF: checkstyle:EmptyBlock
        } catch (final JSONException e) {
            //CHECKSTYLE:ON: checkstyle:EmptyBlock
        }
    }

    @Test
    public void testExtractJsonObjectIntoMapHappyPath() {
        try {
            final Map<String, String> result = MsalUtils.extractJsonObjectIntoMap("{\"JsonKey\":\"JsonValue\"}");
            Assert.assertNotNull(result);
            Assert.assertTrue(result.get("JsonKey").equals("JsonValue"));
        } catch (JSONException e) {
            Assert.fail("Unexpected exception.");
        }
    }

    @Test
    public void testCalculateExpiresOnNullAndEmptyExpiresIn() {
        final Date expiresOnWithNullExpiresIn = MsalUtils.calculateExpiresOn(null);
        final Date expiresOnWithEmptyExpiresIn = MsalUtils.calculateExpiresOn("");

        final Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.SECOND, MsalUtils.DEFAULT_EXPIRATION_TIME_SEC);

        Assert.assertNotNull(expiresOnWithNullExpiresIn);
        Assert.assertFalse(expiresOnWithNullExpiresIn.after(calendar.getTime()));

        Assert.assertNotNull(expiresOnWithEmptyExpiresIn);
        Assert.assertFalse(expiresOnWithEmptyExpiresIn.after(calendar.getTime()));
    }

    @Test
    public void testCalculateExpiresOnWithExpiresIn() {
        final String expiresIn = "1000";
        final Date expiresOn = MsalUtils.calculateExpiresOn(expiresIn);

        final Calendar expectedDate = new GregorianCalendar();
        expectedDate.add(Calendar.SECOND, Integer.parseInt(expiresIn));

        Assert.assertNotNull(expiresOn);
        Assert.assertFalse(expiresOn.after(expectedDate.getTime()));
    }

    @Test
    public void testGetScopeAsSetEmptyAndNullScopes() {
        Assert.assertNotNull(MsalUtils.getScopesAsSet("").isEmpty());
        Assert.assertTrue(MsalUtils.getScopesAsSet(null).isEmpty());
    }

    @Test
    public void testGetScopesAsSet() {
        // Verify that if the input scope array only contains one scope, it's correctly converted into the set.
        final String singleScope = "scope";
        final Set<String> singleScopeSet = MsalUtils.getScopesAsSet(singleScope);
        Assert.assertNotNull(singleScopeSet);
        Assert.assertTrue(singleScopeSet.size() == EXPECTED_SINGLE_SCOPE_SIZE);
        Assert.assertTrue(singleScopeSet.contains(singleScope));

        // Verify if the scopes array has multiple space in the input string, it's corretly converted into the set.
        final String singleScopeWithTrailingSpace = singleScope + "   ";
        final Set<String> singleScopeSetWithTrailingSpace = MsalUtils.getScopesAsSet(singleScopeWithTrailingSpace);
        Assert.assertNotNull(singleScopeSetWithTrailingSpace);
        Assert.assertTrue(singleScopeSetWithTrailingSpace.size() == EXPECTED_SINGLE_SCOPE_SIZE);
        Assert.assertTrue(singleScopeSetWithTrailingSpace.contains(singleScope));

        final String multipleScopesInput = "scope1 scope2  scope3  ";
        final Set<String> multipleScopesSet = MsalUtils.getScopesAsSet(multipleScopesInput);
        Assert.assertNotNull(multipleScopesSet);
        Assert.assertTrue(multipleScopesSet.size() == EXPECTED_MULTI_SCOPE_SIZE);
        Assert.assertTrue(multipleScopesSet.contains("scope1"));
        Assert.assertTrue(multipleScopesSet.contains("scope2"));
        Assert.assertTrue(multipleScopesSet.contains("scope3"));
    }

    @Test
    public void testHasCustomTabRedirectActivity() {
        final Context mockedContext = Mockito.mock(Context.class);
        final String url = "https://login.microsoftonline.com";

        // If package manager is null
        Mockito.when(mockedContext.getPackageManager()).thenReturn(null);
        Assert.assertFalse(MsalUtils.hasCustomTabRedirectActivity(mockedContext, url));

        // resolveInfo list is empty
        final PackageManager mockedPackageManager = Mockito.mock(PackageManager.class);
        Mockito.when(mockedContext.getPackageManager()).thenReturn(mockedPackageManager);
        Mockito.when(mockedPackageManager.queryIntentActivities(Matchers.any(Intent.class),
                Matchers.eq(PackageManager.GET_RESOLVED_FILTER))).thenReturn(Collections.<ResolveInfo>emptyList());
        Assert.assertFalse(MsalUtils.hasCustomTabRedirectActivity(mockedContext, url));

        // resolve info list contains single item, and the activity name is BrowserTabActivity class name.
        final List<ResolveInfo> resolveInfos = new ArrayList<>();
        Mockito.when(mockedPackageManager.queryIntentActivities(Matchers.any(Intent.class),
                Matchers.eq(PackageManager.GET_RESOLVED_FILTER))).thenReturn(resolveInfos);

        final ResolveInfo mockedResolveInfo1 = Mockito.mock(ResolveInfo.class);
        final ActivityInfo mockedActivityInfo1 = Mockito.mock(ActivityInfo.class);
        mockedActivityInfo1.name = BrowserTabActivity.class.getName();
        mockedResolveInfo1.activityInfo = mockedActivityInfo1;
        resolveInfos.add(mockedResolveInfo1);
        Assert.assertTrue(MsalUtils.hasCustomTabRedirectActivity(mockedContext, url));

        // resolve info list contains multiple items
        final ResolveInfo mockedResolveInfo2 = Mockito.mock(ResolveInfo.class);
        final ActivityInfo mockedActivityInfo2 = Mockito.mock(ActivityInfo.class);
        mockedActivityInfo2.name = "some other class";
        mockedResolveInfo2.activityInfo = mockedActivityInfo2;
        resolveInfos.add(mockedResolveInfo2);
        Assert.assertFalse(MsalUtils.hasCustomTabRedirectActivity(mockedContext, url));
    }

    @Test
    public void getChromePackageWithCustomTabSupport() {
        final Context mockedContext = Mockito.mock(Context.class);

        // If package manager is null
        Mockito.when(mockedContext.getPackageManager()).thenReturn(null);
        Assert.assertNull(MsalUtils.getChromePackageWithCustomTabSupport(mockedContext));

        final PackageManager mockedPackageManager = Mockito.mock(PackageManager.class);

        // if not custom tab service exists
        Mockito.when(mockedPackageManager.queryIntentServices(Matchers.any(Intent.class),
                Matchers.eq(0))).thenReturn(null);
        Assert.assertNull(MsalUtils.getChromePackageWithCustomTabSupport(mockedContext));

        final List<ResolveInfo> resolvedInfos = new ArrayList<>();
        Mockito.when(mockedContext.getPackageManager()).thenReturn(mockedPackageManager);
        Mockito.when(mockedPackageManager.queryIntentServices(Matchers.any(Intent.class),
                Matchers.eq(0))).thenReturn(resolvedInfos);

        // If custom tab service exists, but it's not belonging to chrome package
        final ResolveInfo mockedResolveInfo = Mockito.mock(ResolveInfo.class);
        final ServiceInfo mockedServiceInfo = Mockito.mock(ServiceInfo.class);
        mockedServiceInfo.packageName = "some package but not chrome";
        mockedResolveInfo.serviceInfo = mockedServiceInfo;
        resolvedInfos.add(mockedResolveInfo);
        Assert.assertNull(MsalUtils.getChromePackageWithCustomTabSupport(mockedContext));

        // If multiple packages have custom tab support, and chrome package also has the support
        final ResolveInfo mockedResolveInfoForChrome = Mockito.mock(ResolveInfo.class);
        final ServiceInfo mockedServiceInfoForChrome = Mockito.mock(ServiceInfo.class);
        mockedServiceInfoForChrome.packageName = MsalUtils.CHROME_PACKAGE;
        mockedResolveInfoForChrome.serviceInfo = mockedServiceInfoForChrome;
        resolvedInfos.add(mockedResolveInfoForChrome);
        final String chromePackageNameWithCustomTabSupport = MsalUtils.getChromePackageWithCustomTabSupport(mockedContext);
        Assert.assertNotNull(chromePackageNameWithCustomTabSupport);
        Assert.assertTrue(chromePackageNameWithCustomTabSupport.equals(MsalUtils.CHROME_PACKAGE));
    }

    @Test
    public void testGetChromePackage() throws PackageManager.NameNotFoundException {
        final Context mockedContext = Mockito.mock(Context.class);

        // package manager is null
        Mockito.when(mockedContext.getPackageManager()).thenReturn(null);
        Assert.assertNull(MsalUtils.getChromePackageWithCustomTabSupport(mockedContext));

        // no chrome package exists
        final PackageManager mockedPackageManager = Mockito.mock(PackageManager.class);
        Mockito.when(mockedContext.getPackageManager()).thenReturn(mockedPackageManager);
        Mockito.when(mockedPackageManager.getPackageInfo(Matchers.refEq(MsalUtils.CHROME_PACKAGE),
                Matchers.eq(PackageManager.GET_ACTIVITIES))).thenThrow(PackageManager.NameNotFoundException.class);

        Assert.assertNull(MsalUtils.getChromePackage(mockedContext));

        //Chrome package exists in the device but is disabled
        final PackageInfo mockedPackageInfo = Mockito.mock(PackageInfo.class);
        final ApplicationInfo mockedApplicationInfo = Mockito.mock(ApplicationInfo.class);
        Mockito.when(mockedPackageManager.getPackageInfo(Matchers.refEq(MsalUtils.CHROME_PACKAGE),
                Matchers.eq(PackageManager.GET_ACTIVITIES))).thenReturn(mockedPackageInfo);

        mockedPackageInfo.applicationInfo = mockedApplicationInfo;
        mockedApplicationInfo.enabled = false;
        Assert.assertNull(MsalUtils.getChromePackage(mockedContext));

        // The three chrome package all exists on the device, return the stable chrome package name.
        mockedApplicationInfo.enabled = true;
        Assert.assertTrue(MsalUtils.getChromePackage(mockedContext).equals(MsalUtils.CHROME_PACKAGE));
    }

    @Test
    public void testDecodeUrlToMap() {
        // url is null or empty
        Assert.assertTrue(MsalUtils.decodeUrlToMap(null, " ").isEmpty());
        Assert.assertTrue(MsalUtils.decodeUrlToMap("", "").isEmpty());

        // delimiter is null
        Assert.assertTrue(MsalUtils.decodeUrlToMap("some url", null).isEmpty());

        final String urlToDecode = "a=b&c=d";
        final Map<String, String> result = MsalUtils.decodeUrlToMap(urlToDecode, "&");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.size() == 2);
        Assert.assertTrue(result.get("a").equals("b"));
        Assert.assertTrue(result.get("c").equals("d"));
    }

    @Test
    public void testConvertSetToStringEmptyOrNullSetOrDelimiter() {
        Assert.assertTrue(StringUtil.convertSetToString(null, " ").equals(""));
        Assert.assertTrue(StringUtil.convertSetToString(Collections.EMPTY_SET, " ").equals(""));

        final Set<String> set = new HashSet<>();
        set.add("some string");
        Assert.assertTrue(StringUtil.convertSetToString(set, null).equals(""));
    }

    @Test
    public void testConvertSetToStringHappyPath() {
        final List<String> input = new ArrayList<>();
        input.add("scope1");
        input.add("scope2");
        final String result = StringUtil.convertSetToString(new HashSet<>(input), " ");

        Assert.assertTrue(result.equals("scope1 scope2"));
    }

    @Test
    public void testConvertStringToSet() {
        final String scope1 = "scope1";
        final String[] scopes = new String[]{" ", scope1, "   "};
        final Set<String> convertedScope = MsalUtils.convertArrayToSet(scopes);

        Assert.assertNotNull(convertedScope);
        Assert.assertTrue(convertedScope.size() == 1);
        Assert.assertTrue(convertedScope.iterator().next().equalsIgnoreCase(scope1));

        final String scope2 = "scope2";
        final String scope3 = "scope3";
        final String[] scopesTest2 = new String[]{scope1, scope2, scope3};
        final Set<String> convertedScope2 = MsalUtils.convertArrayToSet(scopesTest2);

        Assert.assertNotNull(convertedScope2);
        Assert.assertTrue(convertedScope2.size() == EXPECTED_SET_SIZE);
        Assert.assertTrue(convertedScope2.contains(scope1));
        Assert.assertTrue(convertedScope2.contains(scope2));
        Assert.assertTrue(convertedScope2.contains(scope3));
    }

    @Test
    public void testBase64Encode() {
        String stringToEncode = "a+b@c.com";
        Assert.assertTrue(base64Decode(MsalUtils.base64UrlEncodeToString(stringToEncode)).equals(stringToEncode));

        stringToEncode = "a$c@b.com";
        Assert.assertTrue(base64Decode(MsalUtils.base64UrlEncodeToString(stringToEncode)).equals(stringToEncode));
    }

    @Test
    public void testAppendQueryParam() throws UnsupportedEncodingException {
        final String authorityUrl = "https://login.microsoftonline.com/common/v2/authorize?p=testpolicy";
        final Map<String, String> queryParamteters = new HashMap<>();
        queryParamteters.put("qp1", "someqp1");

        final String appendedAuthority = MsalUtils.appendQueryParameterToUrl(authorityUrl, queryParamteters);
        Assert.assertTrue(appendedAuthority.equals(
                "https://login.microsoftonline.com/common/v2/authorize?p=testpolicy&qp1=someqp1"));
    }

    @Test
    public void testAppendQueryParamWithQueryStringDelimiter() throws UnsupportedEncodingException {
        final String authorityUrl = "https://login.microsoftonline.com/common/v2/authorize?p=testpolicy&";
        final Map<String, String> queryParamteters = new HashMap<>();
        queryParamteters.put("qp1", "someqp1");

        final String appendedAuthority = MsalUtils.appendQueryParameterToUrl(authorityUrl, queryParamteters);
        Assert.assertTrue(appendedAuthority.equals(
                "https://login.microsoftonline.com/common/v2/authorize?p=testpolicy&qp1=someqp1"));

    }

    private String base64Decode(final String encodedString) {
        return new String(Base64.decode(encodedString.getBytes(Charset.forName(MsalUtils.ENCODING_UTF8)), Base64.NO_PADDING | Base64.URL_SAFE));
    }
}
