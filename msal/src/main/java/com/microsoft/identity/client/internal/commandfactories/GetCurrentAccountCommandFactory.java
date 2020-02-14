package com.microsoft.identity.client.internal.commandfactories;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.common.internal.commands.BaseCommand;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.CommandCallback;
import com.microsoft.identity.common.internal.commands.GetCurrentAccountCommand;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.request.generated.GetCurrentAccountCommandContext;
import com.microsoft.identity.common.internal.request.generated.GetCurrentAccountCommandParameters;

public class GetCurrentAccountCommandFactory extends CommandFactory<GetCurrentAccountCommandContext, GetCurrentAccountCommandParameters> {
    @Override
    public BaseCommand createCommand(@NonNull GetCurrentAccountCommandParameters parameters, @NonNull CommandCallback callback, @NonNull ClientApplication application) throws MsalClientException {
        GetCurrentAccountCommandContext context = createCommandContext(application);
        BaseController controller = MSALControllerFactory.getDefaultController(application.getConfiguration().getAppContext(),
                application.getConfiguration().getDefaultAuthority(),
                application.getConfiguration());
        return new GetCurrentAccountCommand(context, parameters, controller, callback);
    }

    @Override
    protected GetCurrentAccountCommandContext createCommandContext(ClientApplication application) {
        return GetCurrentAccountCommandContext.builder()
                .setAndroidApplicationContext(application.getConfiguration().getAppContext())
                .setApplicationName(application.getConfiguration().getAppContext().getPackageName())
                .setApplicationVersion(getPackageVersion(application.getConfiguration().getAppContext()))
                .setCorrelationId("")
                .setIsSharedDevice(application.getConfiguration().getIsSharedDevice())
                .setRequiredBrokerProtocolVersion(application.getConfiguration().getRequiredBrokerProtocolVersion())
                .setTokenCache(application.getTokenCache())
                .setSdkType(SdkType.MSAL)
                .setSdkVersion(PublicClientApplication.getSdkVersion())
                .build();
    }
}
