package com.microsoft.identity.client.internal.commandfactories;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.common.internal.controllers.BaseCommand;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.CommandCallback;
import com.microsoft.identity.common.internal.controllers.LoadAccountCommand;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.request.generated.LoadAccountCommandContext;
import com.microsoft.identity.common.internal.request.generated.LoadAccountCommandParameters;

import java.util.List;

public class LoadAccountCommandFactory extends CommandFactory<LoadAccountCommandContext, LoadAccountCommandParameters> {
    @Override
    public BaseCommand createCommand(@NonNull final Object parameters,
                                     @NonNull final CommandCallback callback,
                                     @NonNull final PublicClientApplicationConfiguration config) throws MsalClientException {

        LoadAccountCommandContext context = createCommandContext();
        LoadAccountCommandParameters commmandParameters = createCommandParameters();
        List<BaseController> controllers = MSALControllerFactory.getAllControllers(config.getAppContext(), config.getDefaultAuthority(), config);
        return new LoadAccountCommand(context, commmandParameters, controllers, callback);

    }

    @Override
    protected LoadAccountCommandParameters createCommandParameters() {
        return LoadAccountCommandParameters.builder()
                .setRedirectUri("")
                .setClientId("")
                .build();
    }

    @Override
    protected LoadAccountCommandContext createCommandContext() {
        return LoadAccountCommandContext.builder()
                .setSdkVersion("")
                .setSdkType(SdkType.MSAL)
                .setRequiredBrokerProtocolVersion("")
                .setOAuth2TokenCache(null)
                .setCorrelationId("")
                .setApplicationVersion("")
                .setApplicationName("")
                .setAndroidApplicationContext(null)
                .build();
    }
}
