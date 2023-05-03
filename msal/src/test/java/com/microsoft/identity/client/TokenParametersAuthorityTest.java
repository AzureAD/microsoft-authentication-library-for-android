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

import android.app.Activity;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.internal.testutils.TestUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.ParameterizedRobolectricTestRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.USER_READ_SCOPE;

/**
 * This class tests the fromAuthority builder of {@link TokenParameters} using audiences
 * other than AzureADMyOrg. The tests for AzureADMyOrg are located in {@link TokenParametersAuthorityMyOrgTest}
 */
@Ignore
@RunWith(ParameterizedRobolectricTestRunner.class)
public class TokenParametersAuthorityTest {

    private Context mContext;
    private Activity mActivity;
    private final List<String> SCOPES = Arrays.asList(USER_READ_SCOPE);

    private AzureCloudInstance azureCloudInstance;
    private AadAuthorityAudience aadAuthorityAudience;
    private String expectedAuthorityUrl;

    private static final String testTenant = UUID.randomUUID().toString();

    public TokenParametersAuthorityTest(final AzureCloudInstance azureCloudInstance,
                                        final AadAuthorityAudience aadAuthorityAudience,
                                        final String expectedAuthorityUrl) {
        this.azureCloudInstance = azureCloudInstance;
        this.aadAuthorityAudience = aadAuthorityAudience;
        this.expectedAuthorityUrl = expectedAuthorityUrl;
    }

    @ParameterizedRobolectricTestRunner.Parameters(name = "cloud = {0}, audience = {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {AzureCloudInstance.AzurePublic, AadAuthorityAudience.AzureAdAndPersonalMicrosoftAccount, "https://login.microsoftonline.com/common"},
                {AzureCloudInstance.AzurePublic, AadAuthorityAudience.AzureAdMultipleOrgs, "https://login.microsoftonline.com/organizations"},
                {AzureCloudInstance.AzurePublic, AadAuthorityAudience.PersonalMicrosoftAccount, "https://login.microsoftonline.com/consumers"},
                {AzureCloudInstance.AzureChina, AadAuthorityAudience.AzureAdAndPersonalMicrosoftAccount, "https://login.partner.microsoftonline.cn/common"},
                {AzureCloudInstance.AzureChina, AadAuthorityAudience.AzureAdMultipleOrgs, "https://login.partner.microsoftonline.cn/organizations"},
                {AzureCloudInstance.AzureChina, AadAuthorityAudience.PersonalMicrosoftAccount, "https://login.partner.microsoftonline.cn/consumers"},
                {AzureCloudInstance.AzureGermany, AadAuthorityAudience.AzureAdAndPersonalMicrosoftAccount, "https://login.microsoftonline.de/common"},
                {AzureCloudInstance.AzureGermany, AadAuthorityAudience.AzureAdMultipleOrgs, "https://login.microsoftonline.de/organizations"},
                {AzureCloudInstance.AzureGermany, AadAuthorityAudience.PersonalMicrosoftAccount, "https://login.microsoftonline.de/consumers"},
                {AzureCloudInstance.AzureUsGov, AadAuthorityAudience.AzureAdAndPersonalMicrosoftAccount, "https://login.microsoftonline.us/common"},
                {AzureCloudInstance.AzureUsGov, AadAuthorityAudience.AzureAdMultipleOrgs, "https://login.microsoftonline.us/organizations"},
                {AzureCloudInstance.AzureUsGov, AadAuthorityAudience.PersonalMicrosoftAccount, "https://login.microsoftonline.us/consumers"},

        });
    }

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mActivity = Mockito.mock(Activity.class);
        Mockito.when(mActivity.getApplicationContext()).thenReturn(mContext);
    }

    @Test
    public void testCreateAuthorityFromUrl() {
        final AcquireTokenParameters tokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(SCOPES)
                .fromAuthority(expectedAuthorityUrl)
                .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                .build();

        Assert.assertEquals(expectedAuthorityUrl, tokenParameters.getAuthority());
    }

    @Test
    public void testCreateAuthorityFromCloudAndAudience() {
        final AcquireTokenParameters tokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(SCOPES)
                .fromAuthority(azureCloudInstance, aadAuthorityAudience)
                .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                .build();

        Assert.assertEquals(expectedAuthorityUrl, tokenParameters.getAuthority());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateAuthorityFromCloudAndAudienceFailsIfTenantSpecifiedAndAudienceNotMyOrg() {
        final AcquireTokenParameters tokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(SCOPES)
                .fromAuthority(azureCloudInstance, aadAuthorityAudience, testTenant)
                .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                .build();

        Assert.assertEquals(expectedAuthorityUrl, tokenParameters.getAuthority());
    }
}
