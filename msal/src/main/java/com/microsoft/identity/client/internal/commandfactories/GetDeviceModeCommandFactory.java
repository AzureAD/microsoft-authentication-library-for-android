package com.microsoft.identity.client.internal.commandfactories;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.common.internal.commands.BaseCommand;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.CommandCallback;
import com.microsoft.identity.common.internal.commands.GetDeviceModeCommand;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.request.generated.GetDeviceModeCommandParameters;

public class GetDeviceModeCommandFactory extends CommandFactory<GetDeviceModeCommandContext, GetDeviceModeCommandParameters> {
    @Override
    public BaseCommand createCommand(@NonNull GetDeviceModeCommandParameters parameters, @NonNull CommandCallback callback, @NonNull ClientApplication application) throws MsalClientException {
        GetDeviceModeCommandContext context = createCommandContext(application);
        BaseController controller = MSALControllerFactory.getDefaultController(application.getConfiguration().getAppContext(),
                application.getConfiguration().getDefaultAuthority(),
                application.getConfiguration());
        return new GetDeviceModeCommand(context, parameters, controller, callback);
    }

    @Override
    protected GetDeviceModeCommandContext createCommandContext(ClientApplication application) {
        return GetDeviceModeCommandContext.builder()
                .setAndroidApplicationContext(application.getConfiguration().getAppContext())
                .setApplicationName(application.getConfiguration().getAppContext().getPackageName())
                .setApplicationVersion(getPackageVersion(application.getConfiguration().getAppContext()))
                .setCorrelationId("")
                .setRequiredBrokerProtocolVersion(application.getConfiguration().getRequiredBrokerProtocolVersion())
                .setSdkType(SdkType.MSAL)
                .setSdkVersion(PublicClientApplication.getSdkVersion())
                .build();
    }
}
