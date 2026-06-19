package com.uni.registration.util;

import java.util.UUID;

/**
 * Single place to mint IDs. Wrapping UUID lets us swap in a sortable id (KSUID/ULID)
 * later without touching every call site.
 */
public final class Ids {
    private Ids() {}

    public static String newId() {
        return UUID.randomUUID().toString();
    }
}
