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

package com.microsoft.identity.nativeauth.utils

import com.microsoft.identity.common.java.result.FinalizableResultFuture
import com.microsoft.identity.common.java.util.StringUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible

internal suspend fun <T> FinalizableResultFuture<T>.getCancellable(): T {
    return try {
        runInterruptible(Dispatchers.IO) { get() }
    } catch (exception: CancellationException) {
        cancelSignal()
        throw exception
    }
}

/**
 * Launches [block] in this [CoroutineScope], guaranteeing that [passwordSnapshot] is cleared once
 * the launched job completes -- including when the job is cancelled before [block] ever starts
 * running (for example, because this scope was already cancelled at the time of the call).
 *
 * [block] remains responsible for clearing [passwordSnapshot] itself once it has consumed it
 * (typically via a `try`/`finally` around command submission). This function is purely a safety
 * net for the case where [block] never runs at all: [StringUtil.overwriteWithNull] is idempotent
 * and null-tolerant, so invoking it again here after [block] already cleared the array is
 * harmless.
 */
internal fun CoroutineScope.launchOwningPasswordSnapshot(
    passwordSnapshot: CharArray?,
    block: suspend CoroutineScope.() -> Unit
): Job {
    val job = launch(block = block)
    job.invokeOnCompletion {
        StringUtil.overwriteWithNull(passwordSnapshot)
    }
    return job
}
