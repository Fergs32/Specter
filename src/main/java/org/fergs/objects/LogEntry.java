package org.fergs.objects;

import lombok.Getter;
import lombok.Setter;

import java.util.logging.Level;

/**
 * Internal class to represent a log entry
 */
@Getter @Setter
public final class LogEntry {
    public final Level level;
    public final String message;
    public final Throwable throwable;
    public final Object[] params;

    public LogEntry(Level level, String message, Throwable throwable, Object[] params) {
        this.level = level;
        this.message = message;
        this.throwable = throwable;
        this.params = params;
    }
}
