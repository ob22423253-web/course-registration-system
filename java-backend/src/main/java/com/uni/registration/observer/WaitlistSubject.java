package com.uni.registration.observer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer subject for waitlist promotions. Uses CopyOnWriteArrayList so adding
 * new listeners at startup is safe even if a fire() is racing with registration.
 * Spring injects every WaitlistObserver bean automatically via the List<>.
 */
@Component
public class WaitlistSubject {

    private final List<WaitlistObserver> observers;

    @Autowired
    public WaitlistSubject(List<WaitlistObserver> discovered) {
        this.observers = new CopyOnWriteArrayList<>(discovered);
    }

    public void register(WaitlistObserver observer) {
        observers.add(observer);
    }

    public void unregister(WaitlistObserver observer) {
        observers.remove(observer);
    }

    public void fire(WaitlistEvent event) {
        for (WaitlistObserver o : observers) {
            o.onSeatPromoted(event);
        }
    }
}
