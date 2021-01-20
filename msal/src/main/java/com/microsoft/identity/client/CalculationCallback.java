package com.microsoft.identity.client;

import com.microsoft.identity.common.internal.commands.CommandCallback;

public interface CalculationCallback extends CommandCallback<String, Exception> {

    void onError(final Exception exception);
}
