package com.microsoft.identity.client.internal.commandfactories;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.PublicClientApplication;
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

import java.util.HashMap;
import java.util.List;

public class LoadAccountCommandFactory extends CommandFactory<LoadAccountCommandContext, LoadAccountCommandParameters, HashMap> {
    @Override
    public BaseCommand createCommand(@NonNull final HashMap parameters,
                                     @NonNull final CommandCallback callback,
                                     @NonNull final PublicClientApplicationConfiguration config) throws MsalClientException {

        validateUnTypedParameters(parameters);
        HashMap<String, String> parametersMap = (HashMap<String, String>)parameters;
        LoadAccountCommandContext context = createCommandContext(config);
        LoadAccountCommandParameters commmandParameters = createCommandParameters(parametersMap);
        List<BaseController> controllers = MSALControllerFactory.getAllControllers(config.getAppContext(), config.getDefaultAuthority(), config);
        return new LoadAccountCommand(context, commmandParameters, controllers, callback);

    }

    private void validateUnTypedParameters(Object parameters){
        if(!(parameters instanceof HashMap)){
            throw new IllegalArgumentException();
        }
    }

    @Override
    protected LoadAccountCommandParameters createCommandParameters(HashMap parameters) {
        return LoadAccountCommandParameters.builder()
                .setRedirectUri("")
                .setClientId("")
                .build();
    }

    @Override
    protected LoadAccountCommandContext createCommandContext(
            @NonNull final PublicClientApplicationConfiguration config) {
        return LoadAccountCommandContext.builder()
                .setSdkVersion(PublicClientApplication.getSdkVersion())
                .setSdkType(SdkType.MSAL)
                .setRequiredBrokerProtocolVersion(config.getRequiredBrokerProtocolVersion())
                .setOAuth2TokenCache(null)
                .setCorrelationId("")
                .setApplicationVersion("")
                .setApplicationName("")
                .setAndroidApplicationContext(null)
                .build();
    }
}
