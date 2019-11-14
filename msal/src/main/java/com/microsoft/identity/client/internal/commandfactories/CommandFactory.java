package com.microsoft.identity.client.internal.commandfactories;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.common.internal.controllers.BaseCommand;
import com.microsoft.identity.common.internal.controllers.CommandCallback;
import com.microsoft.identity.common.internal.controllers.LoadAccountCommand;
import com.microsoft.identity.common.internal.request.generated.CommandContext;
import com.microsoft.identity.common.internal.request.generated.CommandParameters;

/**
 * Abstract factory definition for creating commands.  Based on the parameters object provided as well as the
 * Library configuration file we will:
 *
 * - Create command context
 * - Select controllers (MSALControllerFactory)
 * - Create command parameters
 * - Return a newly minted command object
 * @param <GenericCommandContext>
 * @param <GenericCommandParameters>
 */
public abstract class CommandFactory<
        GenericCommandContext extends CommandContext,
        GenericCommandParameters extends CommandParameters> {

    public abstract BaseCommand createCommand(@NonNull final Object parameters,
                                              @NonNull final CommandCallback callback,
                                              @NonNull final PublicClientApplicationConfiguration config) throws MsalClientException;
    protected abstract GenericCommandParameters createCommandParameters();
    protected abstract GenericCommandContext createCommandContext();

    public static BaseCommand createCommand(@NonNull final Class clz,
                                            @NonNull final Object parameters,
                                            @NonNull final CommandCallback callback,
                                            @NonNull final PublicClientApplicationConfiguration config){
        throw new UnsupportedOperationException();

    }

}
