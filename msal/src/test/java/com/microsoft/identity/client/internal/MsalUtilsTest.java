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

import com.microsoft.identity.client.exception.MsalArgumentException;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class MsalUtilsTest {

    @Test
    public void isEmpty_nullBlankAndNonBlank() {
        Assert.assertTrue(MsalUtils.isEmpty(null));
        Assert.assertTrue(MsalUtils.isEmpty(""));
        Assert.assertTrue(MsalUtils.isEmpty("   "));
        Assert.assertFalse(MsalUtils.isEmpty("value"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateNonNullArgument_throwsOnNull() {
        MsalUtils.validateNonNullArgument(null, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateNonNullArgument_throwsOnEmptyCharSequence() {
        MsalUtils.validateNonNullArgument("", "arg");
    }

    @Test
    public void validateNonNullArgument_passesOnNonEmpty() {
        MsalUtils.validateNonNullArgument("value", "arg");
    }

    @Test(expected = MsalArgumentException.class)
    public void validateNonNullArg_throwsOnEmptyList() throws MsalArgumentException {
        MsalUtils.validateNonNullArg(Collections.emptyList(), "arg");
    }

    @Test(expected = MsalArgumentException.class)
    public void validateNonNullArg_throwsOnEmptyMap() throws MsalArgumentException {
        MsalUtils.validateNonNullArg(new HashMap<>(), "arg");
    }

    @Test
    public void validateNonNullArg_passesOnNonEmptyList() throws MsalArgumentException {
        MsalUtils.validateNonNullArg(Arrays.asList("a"), "arg");
    }

    @Test
    public void urlFormEncodeDecode_roundTripAndEmpty() throws Exception {
        Assert.assertEquals("", MsalUtils.urlFormEncode(""));
        Assert.assertEquals("", MsalUtils.urlFormDecode(""));
        final String raw = "a b&c=d";
        Assert.assertEquals(raw, MsalUtils.urlFormDecode(MsalUtils.urlFormEncode(raw)));
    }

    @Test
    public void extractJsonObjectIntoMap_parsesEntries() throws JSONException {
        final Map<String, String> map =
                MsalUtils.extractJsonObjectIntoMap("{\"a\":\"1\",\"b\":\"2\"}");
        Assert.assertEquals("1", map.get("a"));
        Assert.assertEquals("2", map.get("b"));
        Assert.assertEquals(2, map.size());
    }

    @Test
    public void getExpiryOrDefault_emptyReturnsDefault() {
        Assert.assertEquals(MsalUtils.DEFAULT_EXPIRATION_TIME_SEC, MsalUtils.getExpiryOrDefault(""));
        Assert.assertEquals(120, MsalUtils.getExpiryOrDefault("120"));
    }

    @Test
    public void calculateExpiresOn_isInTheFuture() {
        final Date expires = MsalUtils.calculateExpiresOn("60");
        Assert.assertTrue(expires.getTime() > System.currentTimeMillis());
    }

    @Test
    public void getScopesAsSet_splitsLowercasesAndDropsEmpty() {
        final Set<String> scopes = MsalUtils.getScopesAsSet("User.Read  Mail.Send");
        Assert.assertTrue(scopes.contains("user.read"));
        Assert.assertTrue(scopes.contains("mail.send"));
        Assert.assertEquals(2, scopes.size());
        Assert.assertTrue(MsalUtils.getScopesAsSet(null).isEmpty());
    }

    @Test
    public void decodeUrlToMap_parsesDelimitedPairs() {
        final Map<String, String> map = MsalUtils.decodeUrlToMap("a=1&b=2", "&");
        Assert.assertEquals("1", map.get("a"));
        Assert.assertEquals("2", map.get("b"));
        Assert.assertTrue(MsalUtils.decodeUrlToMap(null, "&").isEmpty());
        Assert.assertTrue(MsalUtils.decodeUrlToMap("a=1", null).isEmpty());
    }

    @Test
    public void appendQueryParameterToUrl_appendsAndHandlesEmptyParams() throws Exception {
        final Map<String, String> params = new HashMap<>();
        Assert.assertEquals("https://host", MsalUtils.appendQueryParameterToUrl("https://host", params));

        params.put("k", "v");
        final String result = MsalUtils.appendQueryParameterToUrl("https://host", params);
        Assert.assertEquals("https://host?k=v", result);

        final String appended = MsalUtils.appendQueryParameterToUrl("https://host?x=y", params);
        Assert.assertTrue(appended.startsWith("https://host?x=y&"));
        Assert.assertTrue(appended.contains("k=v"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void appendQueryParameterToUrl_emptyUrlThrows() throws Exception {
        MsalUtils.appendQueryParameterToUrl("", new HashMap<String, String>());
    }

    @Test
    public void isScopeIntersects_detectsOverlap() {
        final Set<String> a = new HashSet<>(Arrays.asList("s1", "s2"));
        Assert.assertTrue(MsalUtils.isScopeIntersects(a, new HashSet<>(Arrays.asList("s2", "s3"))));
        Assert.assertFalse(MsalUtils.isScopeIntersects(a, new HashSet<>(Arrays.asList("s9"))));
    }

    @Test
    public void base64UrlEncodeToString_nonEmpty() {
        Assert.assertNotNull(MsalUtils.base64UrlEncodeToString("message"));
    }

    @Test
    public void createHash_nonEmptyAndPassthrough() throws Exception {
        Assert.assertNotNull(MsalUtils.createHash("value"));
        Assert.assertEquals("", MsalUtils.createHash(""));
    }

    @Test
    public void getUniqueUserIdentifier_joinsEncodedParts() {
        final String id = MsalUtils.getUniqueUserIdentifier("uid", "utid");
        Assert.assertTrue(id.contains("."));
    }

    @Test
    public void getExpiresOn_addsExpiresInToNow() {
        final long nowSecs = System.currentTimeMillis() / 1000;
        final long expiresOn = MsalUtils.getExpiresOn(100);
        Assert.assertTrue(expiresOn >= nowSecs + 100 - 2);
    }

    @Test
    public void convertArrayToSet_dropsEmptyAndHandlesNull() {
        final Set<String> set = MsalUtils.convertArrayToSet(new String[]{"a", "", "b"});
        Assert.assertEquals(2, set.size());
        Assert.assertTrue(MsalUtils.convertArrayToSet(null).isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void throwOnMainThread_throwsOnMainLooper() {
        // Robolectric runs the test body on the main looper by default.
        MsalUtils.throwOnMainThread("someBackgroundMethod");
    }
}
