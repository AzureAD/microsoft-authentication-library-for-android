package com.microsoft.identity.client.controllers;

import java.util.ArrayList;
import java.util.List;

public class MSALControllerFactory {

    public MSALController getAcquireTokenController(){
        //TODO: Add code to check if broker available and current app eligible to use the broker;
        //If Broker eligible return the BrokerMSALController
        return new LocalMSALController();
    }

    public List<MSALController> getAcquireTokenSilentControllers(){
        //TODO: Add code to check if broker available and current app eligible to use the broker;
        //If Broker eligible return both controller with local first then broker
        List<MSALController> controllers = new ArrayList<MSALController>();
        controllers.add(new LocalMSALController());
        //controllers.add(new BrokerMSALController());

        return controllers;

    }
}
