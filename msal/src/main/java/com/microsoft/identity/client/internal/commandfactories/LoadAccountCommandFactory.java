package com.microsoft.identity.client.internal.commandfactories;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.common.internal.commands.BaseCommand;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.CommandCallback;
import com.microsoft.identity.common.internal.commands.LoadAccountCommand;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.request.generated.LoadAccountCommandContext;
import com.microsoft.identity.common.internal.request.generated.LoadAccountCommandParameters;

import java.util.List;

public class LoadAccountCommandFactory extends CommandFactory<LoadAccountCommandContext, LoadAccountCommandParameters> {
    @Override
    public BaseCommand createCommand(@NonNull final LoadAccountCommandParameters parameters,
                                     @NonNull final CommandCallback callback,
                                     @NonNull final ClientApplication application) throws MsalClientException {
        LoadAccountCommandContext context = createCommandContext(application);
        List<BaseController> controllers = MSALControllerFactory.getAllControllers(application.getConfiguration().getAppContext(),
                application.getConfiguration().getDefaultAuthority(),
                application.getConfiguration());
        return new LoadAccountCommand(context, parameters, controllers, callback);
    }

    @Override
    protected LoadAccountCommandContext createCommandContext(
            @NonNull final ClientApplication application) {
        return LoadAccountCommandContext.builder()
                .setSdkVersion(PublicClientApplication.getSdkVersion())
                .setSdkType(SdkType.MSAL)
                .setRequiredBrokerProtocolVersion(application.getConfiguration().getRequiredBrokerProtocolVersion())
                .setOAuth2TokenCache(application.getTokenCache())
                .setCorrelationId("") //Need to add this to the parameters
                .setApplicationVersion(getPackageVersion(application.getConfiguration().getAppContext()))
                .setApplicationName(application.getConfiguration().getAppContext().getPackageName())
                .setAndroidApplicationContext(application.getConfiguration().getAppContext())
                .build();
    }
}
