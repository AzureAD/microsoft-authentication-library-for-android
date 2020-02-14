package com.microsoft.identity.client.internal.commandfactories;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.common.internal.commands.BaseCommand;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.CommandCallback;
import com.microsoft.identity.common.internal.commands.SilentTokenCommand;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.request.generated.SilentTokenCommandContext;
import com.microsoft.identity.common.internal.request.generated.SilentTokenCommandParameters;

import java.util.List;

public class SilentTokenCommandFactory extends CommandFactory<SilentTokenCommandContext, SilentTokenCommandParameters> {
    @Override
    public BaseCommand createCommand(@NonNull SilentTokenCommandParameters parameters, @NonNull CommandCallback callback, @NonNull ClientApplication application) throws MsalClientException {
        SilentTokenCommandContext context = createCommandContext(application);
        List<BaseController> controllers = MSALControllerFactory.getAllControllers(application.getConfiguration().getAppContext(),
                application.getConfiguration().getDefaultAuthority(),
                application.getConfiguration());
        return new SilentTokenCommand(context, parameters, controllers, callback);
    }

    @Override
    protected SilentTokenCommandContext createCommandContext(ClientApplication application) {
        return SilentTokenCommandContext.builder()
                .setAndroidApplicationContext(application.getConfiguration().getAppContext())
                .setApplicationName(application.getConfiguration().getAppContext().getPackageName())
                .setApplicationVersion(getPackageVersion(application.getConfiguration().getAppContext()))
                .setCorrelationId("")
                .setOAuth2TokenCache(application.getTokenCache())
                .setRequiredBrokerProtocolVersion(application.getConfiguration().getRequiredBrokerProtocolVersion())
                .setSdkType(SdkType.MSAL)
                .setSdkVersion(PublicClientApplication.getSdkVersion())
                .build();
    }
}
