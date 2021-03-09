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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.exception.MsalClientException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

import java.util.Arrays;
import java.util.concurrent.Callable;

import static com.microsoft.identity.client.exception.MsalClientException.APP_MANIFEST_VALIDATION_ERROR;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class PublicClientApplicationConfigurationBrokerFlowTest {

    private interface TestBundle<T> {
        Callable<T> getInput();

        void validate(@NonNull final Callable<T> input);
    }

    private static class BasicExceptionValidator implements TestBundle<Void> {

        private final PublicClientApplicationConfiguration config = new PublicClientApplicationConfiguration();

        private final String mInput;

        BasicExceptionValidator(@Nullable final String input) {
            mInput = input;
        }

        @Override
        public Callable<Void> getInput() {
            return new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    config.setRedirectUri(null);
                    config.checkIntentFilterAddedToAppManifestForBrokerFlow();
                    return null;
                }
            };
        }

        @Override
        public void validate(@NonNull final Callable<Void> input) {
            try {
                input.call();
            } catch (final Exception e) {
                if (e instanceof MsalClientException) {
                    Assert.assertEquals(
                            APP_MANIFEST_VALIDATION_ERROR,
                            ((MsalClientException) e).getErrorCode()
                    );
                } else {
                    Assert.fail();
                }
            }
        }
    }

    @ParameterizedRobolectricTestRunner.Parameters
    public static Iterable<Object[]> testParams() {
        final PublicClientApplicationConfiguration config = new PublicClientApplicationConfiguration();
        return Arrays.asList(
                new Object[]{
                        new BasicExceptionValidator("")
                },
                new Object[]{
                        new BasicExceptionValidator(null)
                },
                new Object[]{
                        new BasicExceptionValidator("null")
                }
        );
    }

    private TestBundle<Void> mTestBundle;

    public PublicClientApplicationConfigurationBrokerFlowTest(TestBundle<Void> input) {
        mTestBundle = input;
    }

    @Test
    public void testInput() {
        mTestBundle.validate(mTestBundle.getInput());
    }

}
