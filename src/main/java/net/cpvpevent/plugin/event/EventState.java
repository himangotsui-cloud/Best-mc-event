package net.cpvpevent.plugin.event;

/**
 * The lifecycle state of the event state machine.
 */
public enum EventState {
    IDLE,
    COUNTDOWN,
    RUNNING,
    PAUSED,
    ENDING
}
