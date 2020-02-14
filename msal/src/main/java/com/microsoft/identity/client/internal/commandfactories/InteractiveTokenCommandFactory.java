package com.microsoft.identity.client.internal.commandfactories;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.common.internal.commands.BaseCommand;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.CommandCallback;
import com.microsoft.identity.common.internal.commands.InteractiveTokenCommand;
import com.microsoft.identity.common.internal.request.generated.InteractiveTokenCommandParameters;

public class InteractiveTokenCommandFactory extends CommandFactory<InteractiveTokenCommandContext, InteractiveTokenCommandParameters> {
    @Override
    public BaseCommand createCommand(@NonNull InteractiveTokenCommandParameters parameters, @NonNull CommandCallback callback, @NonNull ClientApplication application) throws MsalClientException {
        InteractiveTokenCommandContext context = createCommandContext(application);
        BaseController controller = MSALControllerFactory.getDefaultController(application.getConfiguration().getAppContext(),
                application.getConfiguration().getDefaultAuthority(),
                application.getConfiguration());
        return new InteractiveTokenCommand(context, parameters, controller, callback);
    }

    @Override
    protected InteractiveTokenCommandContext createCommandContext(ClientApplication application) {
        return null;
    }
}
