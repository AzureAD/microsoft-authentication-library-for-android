package com.microsoft.identity.common.internal.controllers;

public class CommandDispatcherHelper {

    public static void clear(){
        CommandDispatcher.clearCommandCache();
    }
}