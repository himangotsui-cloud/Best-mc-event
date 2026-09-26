package net.cpvpevent.plugin.event;

/**
 * Determines how an event progresses once started.
 */
public enum EventMode {
    AUTOMATIC,
    HIGH_PLAYERS,
    LOW_PLAYERS,
    MANUALLY;

    public static EventMode fromString(String value) {
        if (value == null) return AUTOMATIC;
        try {
            return EventMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return switch (value.trim().toLowerCase()) {
                case "manual" -> MANUALLY;
                default -> AUTOMATIC;
            };
        }
    }
}
