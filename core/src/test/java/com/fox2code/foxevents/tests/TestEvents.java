package com.fox2code.foxevents.tests;

import com.fox2code.foxevents.EventHandler;
import com.fox2code.foxevents.EventHolder;
import com.fox2code.foxevents.FoxEvents;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestSkippedException;

public class TestEvents {
    private static final EventHolder<BasicEvent> BASIC_EVENT_HOLDER =
            EventHolder.getHolderFromEvent(BasicEvent.class);
    private static final EventHolder<BasicDelegateEvent> BASIC_DELEGATE_EVENT_HOLDER =
            EventHolder.getHolderFromEvent(BasicDelegateEvent.class);
    private static final EventHolder<BasicSecondaryEvent> BASIC_SECONDARY_EVENT_HOLDER =
            EventHolder.getHolderFromEvent(BasicSecondaryEvent.class);
    private static final EventHolder<CancellableEvent> CANCELLABLE_EVENT_HOLDER =
            EventHolder.getHolderFromEvent(CancellableEvent.class);
    private static boolean basicStatic = false;

    static {
        FoxEvents.getFoxEvents().registerEvents(TestEvents.class);
    }

    public TestEvents() {
        FoxEvents.getFoxEvents().registerEvents(this);
        if (BASIC_EVENT_HOLDER.isEmpty()) {
            throw new TestSkippedException("Event registration failed");
        }
        if (BASIC_DELEGATE_EVENT_HOLDER.isEmpty()) {
            throw new TestSkippedException("Event registration failed");
        }
        if (BASIC_SECONDARY_EVENT_HOLDER.isEmpty()) {
            throw new TestSkippedException("Event registration failed");
        }
        if (CANCELLABLE_EVENT_HOLDER.isEmpty()) {
            throw new TestSkippedException("Event registration failed");
        }
    }

    boolean basic, delegate, secondary, cancellable, cancellableIgnoreCancelled;
    boolean shouldCancelCancellable, isCancellableCancelled;
    int basicCallCount;

    public void reset() {
        if (BASIC_EVENT_HOLDER.isEmpty()) {
            throw new TestSkippedException("Event registration failed");
        }
        this.basic = false;
        this.delegate = false;
        this.secondary = false;
        this.cancellable = false;
        this.cancellableIgnoreCancelled = false;
        this.shouldCancelCancellable = false;
        this.isCancellableCancelled = false;
        this.basicCallCount = 0;
    }

    public void assertState(boolean basic,boolean delegate, boolean singleton,boolean cancellableSingleton,
                            boolean cancellableSingletonIC, boolean shouldCancelCancellable,
                            boolean isSingletonCancelled, int basicCallCount) {
        Assertions.assertEquals(basic, this.basic, "this.basic != basic");
        Assertions.assertEquals(delegate, this.delegate, "this.delegate != delegate");
        Assertions.assertEquals(singleton, this.secondary, "this.singleton != singleton");
        Assertions.assertEquals(cancellableSingleton, this.cancellable,
                "this.cancellableSingleton != cancellableSingleton");
        Assertions.assertEquals(cancellableSingletonIC, this.cancellableIgnoreCancelled,
                "this.cancellableSingletonIC != cancellableSingletonIC");
        Assertions.assertEquals(shouldCancelCancellable, this.shouldCancelCancellable,
                "this.shouldCancelCancellable != shouldCancelCancellable");
        Assertions.assertEquals(isSingletonCancelled, this.isCancellableCancelled,
                "this.isSingletonCancelled != isSingletonCancelled");
        Assertions.assertEquals(basicCallCount, this.basicCallCount,
                "this.basicCallCount != basicCallCount");
    }

    @Test
    public synchronized void testRegistering() {
        Assertions.assertFalse(BASIC_EVENT_HOLDER.isEmpty());
    }

    @Test
    public synchronized void testBasicEvent() {
        this.reset();
        basicStatic = false;
        BasicEvent basicEvent = new BasicEvent();
        basicEvent.callEvent();
        this.assertState(true, false, false, false, false, false, false, 1);
        Assertions.assertTrue(basicStatic);
        this.reset();
        basicEvent.callEvent();
        this.assertState(true, false, false, false, false, false, false, 1);
        basicEvent.callEvent();
        this.assertState(true, false, false, false, false, false, false, 2);
    }

    @Test
    public synchronized void testBasicDelegateEvent() {
        this.reset();
        new BasicDelegateEvent().callEvent();
        this.assertState(true, true, false, false, false, false, false, 1);
    }

    @Test
    public synchronized void testBasicSingletonEvent() {
        this.reset();
        new BasicSecondaryEvent().callEvent();
        this.assertState(false, false, true, false, false, false, false, 0);
    }

    @Test
    public synchronized void testCancellableSingletonEvent() {
        this.reset();
        new CancellableEvent().callEvent();
        this.assertState(false, false, false, true, true, false, false, 0);
        this.reset();
        this.shouldCancelCancellable = true;
        new CancellableEvent().callEvent();
        this.assertState(false, false, false, true, true, true, true, 0);
        this.reset();
        CancellableEvent event =
                new CancellableEvent();
        event.setCancelled(true);
        event.callEvent();

        this.assertState(false, false, false, false, true, false, true, 0);
    }

    @Test
    public synchronized void testEventHolderForEach() {
        final int[] counter = new int[]{0};
        EventHolder.forEachEventHolder(eventHolder -> counter[0]++);
        // This is good enough for now
        Assertions.assertEquals(4, counter[0]);
    }

    @EventHandler
    public void onBasicEvent(BasicEvent basicEvent) {
        this.basic = true;
        this.basicCallCount++;
    }

    @EventHandler
    public void onBasicDelegateEvent(BasicDelegateEvent basicDelegateEvent) {
        this.delegate = true;
    }


    @EventHandler
    public void onBasicSingletonEvent(BasicSecondaryEvent basicSecondaryEvent) {
        this.secondary = true;
    }

    @EventHandler
    public void onCancellableSingletonEvent(CancellableEvent cancellableEvent) {
        this.cancellable = true;
        if (this.shouldCancelCancellable) {
            cancellableEvent.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = 999)
    public void onCancellableSingletonEventIC(CancellableEvent cancellableEvent) {
        this.cancellableIgnoreCancelled = true;
        this.isCancellableCancelled = cancellableEvent.isCancelled();
    }

    @EventHandler
    public static void onBasicEventStatic(BasicEvent basicEvent) {
        basicStatic = true;
    }
}
