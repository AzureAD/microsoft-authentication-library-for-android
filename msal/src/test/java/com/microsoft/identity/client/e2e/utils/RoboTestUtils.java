package com.microsoft.identity.client.e2e.utils;

import androidx.annotation.NonNull;

import org.robolectric.RuntimeEnvironment;

public class RoboTestUtils {

    public static void flushScheduler() {
        // wait until all runnable(s) have finished executing
        while (!RuntimeEnvironment.getMasterScheduler().advanceToLastPostedRunnable()) ;
    }

    public static void flushSchedulerWithDelay(@NonNull final long sleepTime) {
        try {
            // just wait a little for runnable(s) to enter the queue
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // if there are no runnable(s) after the delay, then we can just return
        if (RuntimeEnvironment.getMasterScheduler().size() == 0) {
            return;
        }

        flushScheduler();
    }
}
