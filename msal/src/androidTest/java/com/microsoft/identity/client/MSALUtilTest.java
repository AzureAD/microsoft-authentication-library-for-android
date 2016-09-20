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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Base64;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link MSALUtils}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class MSALUtilTest {
    static final int EXPECTED_SINGLE_SCOPE_SIZE = 1;
    static final int EXPECTED_MULTI_SCOPE_SIZE = 3;
    @Test
    public void testNullMessage() {
        Assert.assertTrue(MSALUtils.isEmpty(null));
    }

    @Test
    public void testEmptyMessage() {
        Assert.assertTrue(MSALUtils.isEmpty(""));
    }

    @Test
    public void testBlankMessage() {
        Assert.assertTrue(MSALUtils.isEmpty("  "));
    }

    @Test
    public void testNonEmptyMessage() {
        Assert.assertFalse(MSALUtils.isEmpty("Test"));
    }

    @Test
    public void testEmptyEncodeDecodeString() throws UnsupportedEncodingException {
        Assert.assertTrue(MSALUtils.urlEncode("").equals(""));
        Assert.assertTrue(MSALUtils.urlDecode("").equals(""));
    }

    @Test
    public void testEncodeDecodeString() throws UnsupportedEncodingException {
        Assert.assertTrue(MSALUtils.urlEncode("1 $%&=").equals("1+%24%25%26%3D"));
        Assert.assertTrue(MSALUtils.urlDecode("+%24%25%26%3D").equals(" $%&="));
    }

    @Test
    public void testExtractJsonObjectIntoMapNullJsonFormat() {
        try {
            MSALUtils.extractJsonObjectIntoMap("test");
            Assert.fail("Expect Json exception");
            //CHECKSTYLE:OFF: checkstyle:EmptyBlock
        } catch (final JSONException e) {
            //CHECKSTYLE:ON: checkstyle:EmptyBlock
        }
    }

    @Test
    public void testExtractJsonObjectIntoMapHappyPath() {
        try {
            final Map<String, String> result = MSALUtils.extractJsonObjectIntoMap("{\"JsonKey\":\"JsonValue\"}");
            Assert.assertNotNull(result);
            Assert.assertTrue(result.get("JsonKey").equals("JsonValue"));
        } catch (JSONException e) {
            Assert.fail("Unexpected exception.");
        }
    }

    @Test
    public void testCalculateExpiresOnNullAndEmptyExpiresIn() {
        final Date expiresOnWithNullExpiresIn = MSALUtils.calculateExpiresOn(null);
        final Date expiresOnWithEmptyExpiresIn = MSALUtils.calculateExpiresOn("");

        final Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.SECOND, MSALUtils.DEFAULT_EXPIRATION_TIME_SEC);

        Assert.assertNotNull(expiresOnWithNullExpiresIn);
        Assert.assertFalse(expiresOnWithNullExpiresIn.after(calendar.getTime()));

        Assert.assertNotNull(expiresOnWithEmptyExpiresIn);
        Assert.assertFalse(expiresOnWithEmptyExpiresIn.after(calendar.getTime()));
    }

    @Test
    public void testCalculateExpiresOnWithExpiresIn() {
        final String expiresIn = "1000";
        final Date expiresOn = MSALUtils.calculateExpiresOn(expiresIn);

        final Calendar expectedDate = new GregorianCalendar();
        expectedDate.add(Calendar.SECOND, Integer.parseInt(expiresIn));

        Assert.assertNotNull(expiresOn);
        Assert.assertFalse(expiresOn.after(expectedDate.getTime()));
    }

    @Test
    public void testGetScopeAsSetEmptyAndNullScopes() {
        Assert.assertNotNull(MSALUtils.getScopesAsSet("").isEmpty());
        Assert.assertTrue(MSALUtils.getScopesAsSet(null).isEmpty());
    }

    @Test
    public void testGetScopesAsSet() {
        // Verify that if the input scope array only contains one scope, it's correctly converted into the set.
        final String singleScope = "scope";
        final Set<String> singleScopeSet = MSALUtils.getScopesAsSet(singleScope);
        Assert.assertNotNull(singleScopeSet);
        Assert.assertTrue(singleScopeSet.size() == EXPECTED_SINGLE_SCOPE_SIZE);
        Assert.assertTrue(singleScopeSet.contains(singleScope));

        // Verify if the scopes array has multiple space in the input string, it's corretly converted into the set.
        final String singleScopeWithTrailingSpace = singleScope + "   ";
        final Set<String> singleScopeSetWithTrailingSpace = MSALUtils.getScopesAsSet(singleScopeWithTrailingSpace);
        Assert.assertNotNull(singleScopeSetWithTrailingSpace);
        Assert.assertTrue(singleScopeSetWithTrailingSpace.size() == EXPECTED_SINGLE_SCOPE_SIZE);
        Assert.assertTrue(singleScopeSetWithTrailingSpace.contains(singleScope));

        final String multipleScopesInput = "scope1 scope2  scope3  ";
        final Set<String> multipleScopesSet = MSALUtils.getScopesAsSet(multipleScopesInput);
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
        Assert.assertFalse(MSALUtils.hasCustomTabRedirectActivity(mockedContext, url));

        // resolveInfo list is empty
        final PackageManager mockedPackageManager = Mockito.mock(PackageManager.class);
        Mockito.when(mockedContext.getPackageManager()).thenReturn(mockedPackageManager);
        Mockito.when(mockedPackageManager.queryIntentActivities(Matchers.any(Intent.class),
                Matchers.eq(PackageManager.GET_RESOLVED_FILTER))).thenReturn(Collections.<ResolveInfo>emptyList());
        Assert.assertFalse(MSALUtils.hasCustomTabRedirectActivity(mockedContext, url));

        // resolve info list contains single item, and the activity name is BrowserTabActivity class name.
        final List<ResolveInfo> resolveInfos = new ArrayList<>();
        Mockito.when(mockedPackageManager.queryIntentActivities(Matchers.any(Intent.class),
                Matchers.eq(PackageManager.GET_RESOLVED_FILTER))).thenReturn(resolveInfos);

        final ResolveInfo mockedResolveInfo1 = Mockito.mock(ResolveInfo.class);
        final ActivityInfo mockedActivityInfo1 = Mockito.mock(ActivityInfo.class);
        mockedActivityInfo1.name = BrowserTabActivity.class.getName();
        mockedResolveInfo1.activityInfo = mockedActivityInfo1;
        resolveInfos.add(mockedResolveInfo1);
        Assert.assertTrue(MSALUtils.hasCustomTabRedirectActivity(mockedContext, url));

        // resolve info list contains multiple items
        final ResolveInfo mockedResolveInfo2 = Mockito.mock(ResolveInfo.class);
        final ActivityInfo mockedActivityInfo2 = Mockito.mock(ActivityInfo.class);
        mockedActivityInfo2.name = "some other class";
        mockedResolveInfo2.activityInfo = mockedActivityInfo2;
        resolveInfos.add(mockedResolveInfo2);
        Assert.assertFalse(MSALUtils.hasCustomTabRedirectActivity(mockedContext, url));
    }

    @Test
    public void getChromePackageWithCustomTabSupport() {
        final Context mockedContext = Mockito.mock(Context.class);

        // If package manager is null
        Mockito.when(mockedContext.getPackageManager()).thenReturn(null);
        Assert.assertNull(MSALUtils.getChromePackageWithCustomTabSupport(mockedContext));

        final PackageManager mockedPackageManager = Mockito.mock(PackageManager.class);

        // if not custom tab service exists
        Mockito.when(mockedPackageManager.queryIntentServices(Matchers.any(Intent.class),
                Matchers.eq(0))).thenReturn(null);
        Assert.assertNull(MSALUtils.getChromePackageWithCustomTabSupport(mockedContext));

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
        Assert.assertNull(MSALUtils.getChromePackageWithCustomTabSupport(mockedContext));

        // If multiple packages have custom tab support, and chrome package also has the support
        final ResolveInfo mockedResolveInfoForChrome = Mockito.mock(ResolveInfo.class);
        final ServiceInfo mockedServiceInfoForChrome = Mockito.mock(ServiceInfo.class);
        mockedServiceInfoForChrome.packageName = MSALUtils.CHROME_PACKAGES[0];
        mockedResolveInfoForChrome.serviceInfo = mockedServiceInfoForChrome;
        resolvedInfos.add(mockedResolveInfoForChrome);
        final String chromePackageNameWithCustomTabSupport = MSALUtils.getChromePackageWithCustomTabSupport(mockedContext);
        Assert.assertNotNull(chromePackageNameWithCustomTabSupport);
        Assert.assertTrue(chromePackageNameWithCustomTabSupport.equals(MSALUtils.CHROME_PACKAGES[0]));
    }

    @Test
    public void testGetChromePackage() throws PackageManager.NameNotFoundException {
        final Context mockedContext = Mockito.mock(Context.class);

        // package manager is null
        Mockito.when(mockedContext.getPackageManager()).thenReturn(null);
        Assert.assertNull(MSALUtils.getChromePackageWithCustomTabSupport(mockedContext));

        // no chrome package exists
        final PackageManager mockedPackageManager = Mockito.mock(PackageManager.class);
        Mockito.when(mockedContext.getPackageManager()).thenReturn(mockedPackageManager);
        for (int i = 0; i < MSALUtils.CHROME_PACKAGES.length; i++) {
            Mockito.when(mockedPackageManager.getPackageInfo(Matchers.refEq(MSALUtils.CHROME_PACKAGES[i]),
                    Matchers.eq(PackageManager.GET_ACTIVITIES))).thenThrow(PackageManager.NameNotFoundException.class);
        }
        Assert.assertNull(MSALUtils.getChromePackage(mockedContext));

        // The three chrome package all exists on the device, return the stable chrome package name.
        for (int i = 0; i < MSALUtils.CHROME_PACKAGES.length; i++) {
            Mockito.when(mockedPackageManager.getPackageInfo(Matchers.refEq(MSALUtils.CHROME_PACKAGES[i]),
                    Matchers.eq(PackageManager.GET_ACTIVITIES))).thenReturn(Mockito.mock(PackageInfo.class));
        }
        Assert.assertTrue(MSALUtils.getChromePackage(mockedContext).equals(MSALUtils.CHROME_PACKAGES[0]));
    }

    @Test
    public void testDecodeUrlToMap() {
        // url is null or empty
        Assert.assertTrue(MSALUtils.decodeUrlToMap(null, " ").isEmpty());
        Assert.assertTrue(MSALUtils.decodeUrlToMap("", "").isEmpty());

        // delimiter is null
        Assert.assertTrue(MSALUtils.decodeUrlToMap("some url", null).isEmpty());

        final String urlToDecode = "a=b&c=d";
        final Map<String, String> result = MSALUtils.decodeUrlToMap(urlToDecode, "&");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.size() == 2);
        Assert.assertTrue(result.get("a").equals("b"));
        Assert.assertTrue(result.get("c").equals("d"));
    }

    @Test
    public void testConvertSetToStringEmptyOrNullSetOrDelimiter() {
        Assert.assertTrue(MSALUtils.convertSetToString(null, " ").equals(""));
        Assert.assertTrue(MSALUtils.convertSetToString(Collections.EMPTY_SET, " ").equals(""));

        final Set<String> set = new HashSet<>();
        set.add("some string");
        Assert.assertTrue(MSALUtils.convertSetToString(set, null).equals(""));
    }

    @Test
    public void testConvertSetToStringHappyPath() {
        final List<String> input = new ArrayList<>();
        input.add("scope1");
        input.add("scope2");
        final String result = MSALUtils.convertSetToString(new HashSet<>(input), " ");

        Assert.assertTrue(result.equals("scope1 scope2"));
    }

    @Test
    public void testBase64Encode() throws UnsupportedEncodingException {
        String stringToEncode = "a+b@c.com";
        Assert.assertTrue(base64Decode(MSALUtils.base64EncodeToString(stringToEncode)).equals(stringToEncode));

        stringToEncode = "a$c@b.com";
        Assert.assertTrue(base64Decode(MSALUtils.base64EncodeToString(stringToEncode)).equals(stringToEncode));
    }

    private String base64Decode(final String encodedString) throws UnsupportedEncodingException {
        return new String(Base64.decode(encodedString.getBytes(MSALUtils.ENCODING_UTF8), Base64.NO_PADDING | Base64.URL_SAFE));
    }
}
