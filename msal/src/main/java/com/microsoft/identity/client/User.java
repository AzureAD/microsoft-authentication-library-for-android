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
 * Contains the detailed info to identify an user. Signout functionality is provided at the user level.
 */
public class User {
    private String mUniqueId;
    private String mDisplayableId;
    private String mName;
    private String mIdentityProvider;
    private String mHomeObjectId;

    /**
     * Internal constructor to create {@link User} from the {@link IdToken}.
     *
     * @param idToken
     */
    User(final IdToken idToken) {
        mDisplayableId = idToken.getPreferredName();
        // TODO: home object id is returned in client info.
        if (!MSALUtils.isEmpty(idToken.getObjectId())) {
            mUniqueId = idToken.getObjectId();
        } else {
            mUniqueId = idToken.getSubject();
        }
        mHomeObjectId = MSALUtils.isEmpty(idToken.getHomeObjectId()) ? mUniqueId : idToken.getHomeObjectId();
        //
        mName = idToken.getName();
        mIdentityProvider = idToken.getIssuer();
    }

    /**
     * @return The unique identifier of the user authenticated during token acquisition. Can be null if not returned
     * from the service.
     */
    String getUniqueId() {
        return mUniqueId;
    }

    /**
     * @return The displayable value in UserPrincipleName(UPN) format. Can be null if not returned from the service.
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
     * Sign out the user from the application. TODO: from all application or the single one?
     */
    // TODO: For preview, signout will only be support regarding to delete token for the user in the cache.
    public void signOut() {
        // TODO: provide the signout function. Will clear the token cache for the particular user.
    }

    // internal methods provided

    /**
     * Used by developer to set the User object when making acquire token API call.
     *
     * @param displayableId
     */
    void setDisplayableId(final String displayableId) {
        mDisplayableId = displayableId;
    }

    void setHomeObjectId(final String homeObjectId) {
        mHomeObjectId = homeObjectId;
    }

    String getHomeObjectId() {
        return mHomeObjectId;
    }
}