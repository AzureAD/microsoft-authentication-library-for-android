package com.microsoft.identity.client.internal.commandfactories;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.common.internal.controllers.BaseCommand;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.CommandCallback;
import com.microsoft.identity.common.internal.controllers.RemoveCurrentAccountCommand;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.request.generated.RemoveCurrentAccountCommandContext;
import com.microsoft.identity.common.internal.request.generated.RemoveCurrentAccountCommandParameters;

public class RemoveCurrentAccountCommandFactory extends CommandFactory<RemoveCurrentAccountCommandContext, RemoveCurrentAccountCommandParameters> {
    @Override
    public BaseCommand createCommand(@NonNull RemoveCurrentAccountCommandParameters parameters, @NonNull CommandCallback callback, @NonNull ClientApplication application) throws MsalClientException {
        RemoveCurrentAccountCommandContext context = createCommandContext(application);
        BaseController controller = MSALControllerFactory.getDefaultController(application.getConfiguration().getAppContext(),
                application.getConfiguration().getDefaultAuthority(),
                application.getConfiguration());
        return new RemoveCurrentAccountCommand(context, parameters, controller, callback);
    }

    @Override
    protected RemoveCurrentAccountCommandContext createCommandContext(ClientApplication application) {
        return RemoveCurrentAccountCommandContext.builder()
                .setAndroidApplicationContext(application.getConfiguration().getAppContext())
                .setApplicationName(application.getConfiguration().getAppContext().getPackageName())
                .setApplicationVersion(getPackageVersion(application.getConfiguration().getAppContext()))
                .setSdkType(SdkType.MSAL)
                .setSdkVersion(PublicClientApplication.getSdkVersion())
                .setCorrelationId("")
                .setRequiredBrokerProtocolVersion(application.getConfiguration().getRequiredBrokerProtocolVersion())
                .build();
    }
}
