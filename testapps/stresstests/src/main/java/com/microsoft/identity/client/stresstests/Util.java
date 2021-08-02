// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.stresstests;

public class Util {

    /**
     * Produces a human-readable representation of the number of seconds.
     *
     * @param seconds the time in seconds
     *
     * @return a string denoting the hours/minutes/seconds
     */
    public static String timeString(long seconds) {
        long hours = seconds / (3600);
        long min = (seconds / 60) % 60;
        long sec = seconds % 60;

        StringBuilder stringBuilder = new StringBuilder();
        if (hours > 0) {
            stringBuilder.append(hours == 1 ? "1 hour " : hours + " hours ");
        }
        if (min > 0) {
            stringBuilder.append(min == 1 ? "1 minute " : min + " minutes ");
        }

        if (sec != 0 || !(min > 0 || hours > 0)) {
            stringBuilder.append(sec == 1 ? "1 second " : sec + " seconds. ");
        }

        return stringBuilder.toString();
    }
}
