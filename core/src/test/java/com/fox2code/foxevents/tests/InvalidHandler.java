package com.fox2code.foxevents.tests;

import com.fox2code.foxevents.EventHandler;

import java.lang.invoke.MethodHandles;

class InvalidHandler {
    public static MethodHandles.Lookup SELF_LOOKUP = MethodHandles.lookup();

    @EventHandler
    public static void onMethodCalled(BasicEvent event) {

    }
}
