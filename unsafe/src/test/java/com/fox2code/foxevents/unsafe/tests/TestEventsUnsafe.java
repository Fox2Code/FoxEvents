package com.fox2code.foxevents.unsafe.tests;

import com.fox2code.foxevents.EventHandler;
import com.fox2code.foxevents.EventHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestEventsUnsafe {
    private static final EventHolder<BasicEvent> BASIC_EVENT_HOLDER =
            EventHolder.getHolderFromEvent(BasicEvent.class);
    private static int staticEventCallCount = 0;
    private int eventCallCount = 0;

    static {
        TestUnsafeFoxEvents.INSTANCE.registerEvents(TestEventsUnsafe.class);
    }

    public TestEventsUnsafe() {
        TestUnsafeFoxEvents.INSTANCE.registerEvents(this);
    }

    @Test
    public synchronized void testAcceleratedOnUnsafe() {
        Assertions.assertTrue(TestUnsafeFoxEvents.INSTANCE.isUsingUnsafeAccelerationTesting());
    }

    @Test
    public synchronized void testRegisteringOnUnsafe() {
        Assertions.assertFalse(BASIC_EVENT_HOLDER.isEmpty());
    }

    @Test
    public synchronized void testBasicEventOnUnsafe() {
        this.eventCallCount = 0;
        staticEventCallCount = 0;
        new BasicEvent().callEvent();
        Assertions.assertEquals(1, this.eventCallCount);
        Assertions.assertEquals(1, staticEventCallCount);
    }

    @EventHandler
    public void onEvent(BasicEvent event) {
        this.eventCallCount++;
    }

    @EventHandler
    public static void onEventStatic(BasicEvent event) {
        staticEventCallCount++;
    }
}
