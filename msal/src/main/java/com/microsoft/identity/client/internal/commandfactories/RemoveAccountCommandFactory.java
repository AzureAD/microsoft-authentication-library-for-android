package com.microsoft.identity.client.internal.commandfactories;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.common.internal.controllers.BaseCommand;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.CommandCallback;
import com.microsoft.identity.common.internal.controllers.RemoveAccountCommand;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.request.generated.RemoveAccountCommandContext;
import com.microsoft.identity.common.internal.request.generated.RemoveAccountCommandParameters;

import java.util.List;

public class RemoveAccountCommandFactory extends CommandFactory<RemoveAccountCommandContext, RemoveAccountCommandParameters> {


    @Override
    public BaseCommand createCommand(@NonNull RemoveAccountCommandParameters parameters, @NonNull CommandCallback callback, @NonNull ClientApplication application) throws MsalClientException {
        RemoveAccountCommandContext context = createCommandContext(application);
        List<BaseController> controllers = MSALControllerFactory.getAllControllers(application.getConfiguration().getAppContext(),
                application.getConfiguration().getDefaultAuthority(),
                application.getConfiguration());
        return new RemoveAccountCommand(context, parameters, controllers, callback);
    }

    @Override
    protected RemoveAccountCommandContext createCommandContext(ClientApplication application) {
        return RemoveAccountCommandContext.builder()
                .setRequiredBrokerProtocolVersion(application.getConfiguration().getRequiredBrokerProtocolVersion())
                .setSdkVersion(PublicClientApplication.getSdkVersion())
                .setSdkType(SdkType.MSAL)
                .setCorrelationId("")
                .setOAuth2TokenCache(application.getTokenCache())
                .setApplicationVersion(getPackageVersion(application.getConfiguration().getAppContext()))
                .setApplicationName(application.getConfiguration().getAppContext().getPackageName())
                .setAndroidApplicationContext(application.getConfiguration().getAppContext())
                .build();
    }
}
