package com.microsoft.identity.common.internal.controllers;

import com.microsoft.identity.common.java.controllers.CommandDispatcher;

public class CommandDispatcherHelper {

    public static void clear(){
        CommandDispatcher.clearCommandCache();
    }
}