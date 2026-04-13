package io.droidevs.mclub.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventControllerCreateEventTest {

    private static String toIsoLocalDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        return value.length() == 16 ? (value + ":00") : value;
    }

    @Test
    void datetimeLocal_withoutSeconds_isNormalizedToSeconds() {
        assertEquals("2026-04-13T12:30:00", toIsoLocalDateTime("2026-04-13T12:30"));
    }

    @Test
    void datetimeLocal_withSeconds_isLeftAsIs() {
        assertEquals("2026-04-13T12:30:45", toIsoLocalDateTime("2026-04-13T12:30:45"));
    }

    @Test
    void blankValue_becomesNull() {
        assertNull(toIsoLocalDateTime(""));
        assertNull(toIsoLocalDateTime("  "));
    }
}
