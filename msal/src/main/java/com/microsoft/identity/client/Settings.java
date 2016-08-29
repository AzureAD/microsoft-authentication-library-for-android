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
 * Settings that could be customized per PublicClientApplication. To perform the customization per application base, developer needs
 * to get the reference from the {@link PublicClientApplication} and call the individual setter. The class doesn't expose the
 * constructor.
 */
public final class Settings {

    private boolean mEnableHardwareAcceleration = true;

    private boolean mDisableCustomTab = false;

    /**
     * Internal constructor to prevent the class from being instantiated externally.
     */
    Settings() { }

    /**
     * Enable/Disable hardware acceleration at the View level during runtime. By default, it's enabled.
     * @param enableHardwareAcceleration True if enabling hardware acceleration at View level, false otherwise.
     */
    public void setEnableHardwareAcceleration(final boolean enableHardwareAcceleration) {
        mEnableHardwareAcceleration = enableHardwareAcceleration;
    }

    /**
     * @return True if enabling hardware acceleration at View level, false otherwise.
     */
    boolean getEnableHardwareAcceleration() {
        return mEnableHardwareAcceleration;
    }

    /**
     * Disable the custom tab loading. TODO: remove, for testing purpose. Keep for now.
     * @param disableCustomTab True if disable the custom tab, false otherwise.
     */
    public void setDisableCustomTab(final boolean disableCustomTab) {
        mDisableCustomTab = disableCustomTab;
    }

    boolean getDisableCustomTab() {
        return mDisableCustomTab;
    }
}