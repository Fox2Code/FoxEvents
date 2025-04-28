package com.fox2code.foxevents.tests;

import com.fox2code.foxevents.EventRegistrationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestHandlers {

    @Test
    public synchronized void testRegisterInvalidHandler() {
        try {
            TextFoxEvents.INSTANCE.registerEvents(InvalidHandler.class);
            Assertions.fail("Event registration didn't throw EventRegistrationException");
        } catch (EventRegistrationException ignored) {}
    }

    @Test
    public synchronized void testRegisterInvalidHandlerIgnoreInvalid() {
        int handlerCount = TextFoxEvents.INSTANCE.registerEventsIgnoreInvalid(InvalidHandler.class);
        Assertions.assertEquals(0, handlerCount, "Multiples handler registered?");
    }

    @Test
    public synchronized void testRegisterInvalidHandlerNowValidWithLookup() {
        TextFoxEvents.INSTANCE.registerEventsWithLookup(InvalidHandler.class, InvalidHandler.SELF_LOOKUP);
    }

    @Test
    public synchronized void testRegisterAbstractHandler() {
        TextFoxEvents.INSTANCE.registerEvents(AbstractHandler.class);
    }
}
