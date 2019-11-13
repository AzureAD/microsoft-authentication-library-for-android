package com.microsoft.identity.client.internal.CommandFactories;

import com.microsoft.identity.common.internal.controllers.BaseCommand;
import com.microsoft.identity.common.internal.request.generated.CommandContext;
import com.microsoft.identity.common.internal.request.generated.CommandParameters;

public abstract class CommandFactory {

    public abstract BaseCommand createCommand();
    protected abstract CommandParameters createCommandParameters();
    protected abstract CommandContext createCommandContext();


}
