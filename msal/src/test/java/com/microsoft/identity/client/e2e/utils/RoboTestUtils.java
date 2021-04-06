package com.microsoft.identity.client.e2e.utils;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.util.ThreadUtils;

import org.robolectric.RuntimeEnvironment;

public class RoboTestUtils {

    public static void flushScheduler() {
        // wait until all runnable(s) have finished executing
        while (!RuntimeEnvironment.getMasterScheduler().advanceToLastPostedRunnable()) ;
    }

    public static void flushSchedulerWithDelay(@NonNull final long sleepTime) {
        ThreadUtils.sleepSafely((int) sleepTime, "RoboTestUtils:flushSchedulerWithDelay", "Interrupted");

        // if there are no runnable(s) after the delay, then we can just return
        if (RuntimeEnvironment.getMasterScheduler().size() == 0) {
            return;
        }

        flushScheduler();
    }
}
