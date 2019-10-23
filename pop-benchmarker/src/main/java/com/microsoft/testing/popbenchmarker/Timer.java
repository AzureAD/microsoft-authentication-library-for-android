package com.microsoft.testing.popbenchmarker;

import androidx.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class Timer {

    static class TimerResult<T> {
        long mDuration;
        T mResult;
    }

    private static ExecutorService sExecutorService = Executors.newCachedThreadPool();

    static <T> TimerResult<T> execute(@NonNull final Callable<T> callable) {
        try {
            final TimerResult<T> result = new TimerResult<>();

            // Start timer
            final long startTime = System.currentTimeMillis();

            // Execute command
            final Future<T> future = sExecutorService.submit(callable);

            // Get the result
            result.mResult = future.get();

            // Package result and return
            result.mDuration = System.currentTimeMillis() - startTime;

            return result;
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
