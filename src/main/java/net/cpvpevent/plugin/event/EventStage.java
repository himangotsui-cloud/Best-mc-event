package net.cpvpevent.plugin.event;

/**
 * One configured step of an Automatic-mode event sequence.
 */
public class EventStage {

    private final EventStageType type;
    private final long delaySeconds;
    private final String message;
    private final int distance;
    private final int timeSeconds;
    private final String dropMode;

    public EventStage(EventStageType type, long delaySeconds, String message,
                       int distance, int timeSeconds, String dropMode) {
        this.type = type;
        this.delaySeconds = delaySeconds;
        this.message = message;
        this.distance = distance;
        this.timeSeconds = timeSeconds;
        this.dropMode = dropMode;
    }

    public EventStageType type() {
        return type;
    }

    public long delaySeconds() {
        return delaySeconds;
    }

    public String message() {
        return message;
    }

    public int distance() {
        return distance;
    }

    public int timeSeconds() {
        return timeSeconds;
    }

    public String dropMode() {
        return dropMode;
    }
}
