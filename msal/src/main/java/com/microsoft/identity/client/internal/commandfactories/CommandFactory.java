package com.microsoft.identity.client.internal.commandfactories;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.common.internal.commands.BaseCommand;
import com.microsoft.identity.common.internal.controllers.CommandCallback;
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
        GenericCommandParameters extends CommandParameters>{

    public abstract BaseCommand createCommand(@NonNull final GenericCommandParameters parameters,
                                              @NonNull final CommandCallback callback,
                                              @NonNull final ClientApplication application) throws MsalClientException;

    protected abstract GenericCommandContext createCommandContext(ClientApplication application);

    public static BaseCommand createCommand(@NonNull final Class clz,
                                            @NonNull final Object parameters,
                                            @NonNull final CommandCallback callback,
                                            @NonNull final ClientApplication application){
        throw new UnsupportedOperationException();

    }

    protected String getPackageVersion(@NonNull final Context context) {
        final String packageName = context.getPackageName();
        try {
            final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
