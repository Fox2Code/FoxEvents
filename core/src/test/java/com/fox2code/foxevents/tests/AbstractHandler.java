package com.fox2code.foxevents.tests;

import com.fox2code.foxevents.EventHandler;

public abstract class AbstractHandler {
    @EventHandler
    public static void onMethodCalled(BasicEvent event) {}
}
