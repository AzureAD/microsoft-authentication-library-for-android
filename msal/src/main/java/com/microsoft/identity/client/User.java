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

/**
 * Contains the detailed info to identify a user. Sign out functionality is provided at the {@link User} level.
 */
public class User {
    private String mDisplayableId;
    private String mName;
    private String mIdentityProvider;
    private String mUid;
    private String mUtid;

    /**
     * Internal constructor to create {@link User} from the {@link IdToken}.
     * User will be created with both {@link IdToken} and {@link ClientInfo}.
     */
    User(final String displayableId, final String name, final String identityProvider, final String uid, final String uTid) {
        mDisplayableId = displayableId;
        mName = name;
        mIdentityProvider = identityProvider;
        mUid = uid;
        mUtid = uTid;
    }

    static User create(final IdToken idToken, final ClientInfo clientInfo) {
        final String uid;
        final String uTid;
        if (clientInfo == null) {
            uid = "";
            uTid = "";
        } else {
            uid = clientInfo.getUniqueIdentifier();
            uTid = clientInfo.getUniqueTenantIdentifier();
        }

        return new User(idToken.getPreferredName(), idToken.getName(), idToken.getIssuer(), uid, uTid);
    }

    /**
     * @return The displayable value in the UserPrincipleName(UPN) format. Can be null if not returned from the service.
     */
    public String getDisplayableId() {
        return mDisplayableId;
    }

    /**
     * @return The given name of the user. Can be null if not returned from the service.
     */
    public String getName() {
        return mName;
    }

    /**
     * @return The identity provider of the user authenticated. Can be null if not returned from the service.
     */
    public String getIdentityProvider() {
        return mIdentityProvider;
    }

    /**
     * @return The unique identifier of the user, which is across tenant.
     */
    public String getUserIdentifier() {
        return MsalUtils.getUniqueUserIdentifier(mUid, mUtid);
    }

    // internal methods provided

    /**
     * Sets the displayableId of a {@link User} when making acquire token API call.
     *
     * @param displayableId
     */
    void setDisplayableId(final String displayableId) {
        mDisplayableId = displayableId;
    }

    String getUid() {
        return mUid;
    }

    void setUid(final String uid) {
        mUid = uid;
    }

    void setUtid(final String uTid) {
        mUtid = uTid;
    }

    String getUtid() {
        return mUtid;
    }
}