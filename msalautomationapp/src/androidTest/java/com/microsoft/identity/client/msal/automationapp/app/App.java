package com.microsoft.identity.client.msal.automationapp.app;

import com.microsoft.identity.client.msal.automationapp.utils.CommonUtils;
import com.microsoft.identity.client.msal.automationapp.utils.PlayStoreUtils;

import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class App implements IApp {

    private String packageName;

    @Setter
    private String appName;

    public App(String packageName) {
        this.packageName = packageName;
    }

    public App(String packageName, String appName) {
        this.packageName = packageName;
        this.appName = appName;
    }

    @Override
    public void install() {
        PlayStoreUtils.installApp(appName != null ? appName : packageName);
    }

    @Override
    public void launch() {
        CommonUtils.launchApp(packageName);
    }

    @Override
    public void clear() {
        CommonUtils.clearApp(packageName);
    }

    @Override
    public void uninstall() {
        CommonUtils.removeApp(packageName);
    }
}
