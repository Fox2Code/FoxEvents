package com.fox2code.foxevents.tests;

import com.fox2code.foxevents.Event;

public class CancellableEvent extends Event implements Event.Cancellable {
    public CancellableEvent() {}
}
