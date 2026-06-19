package com.uni.registration.observer;

/**
 * Concrete observers implement this — abstract base below adds shared logging
 * so subclasses only describe their specific notification channel.
 */
public interface WaitlistObserver {
    void onSeatPromoted(WaitlistEvent event);
}
