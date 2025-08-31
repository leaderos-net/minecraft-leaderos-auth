package net.leaderos.auth.helpers;

import net.leaderos.auth.Bukkit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConsoleLogger extends AbstractFilter {
    /**
     * This determines if our filter runs <i>before</i> (if {@code true}) or <i>after</i> (if {@code false})
     * the string placeholders are replaced.
     *
     * Usually there's no need to change this.
     */
    private static final boolean USE_RAW_STRING = false;

    public ConsoleLogger() {
        // register this filter with the intention of filtering out any message that matches
        // this filter (DENY), else we don't have any particular opinion about it (NEUTRAL)
        //
        // super(onMatch, onMismatch)
        super(Filter.Result.DENY, Filter.Result.NEUTRAL);
    }

    /**
     * Here is where we decide if we want to filter out the message or not. Returning {@link Filter.Result#DENY}
     * (our {@link AbstractFilter#onMatch onMatch}) will filter out the message, and {@link Filter.Result#NEUTRAL}
     * (our {@link AbstractFilter#onMismatch onMismatch}) will leave it alone.
     */
    @NotNull
    private Result doFilter(@Nullable String message) {
        if (message == null) {
            return onMismatch;
        }

        // Check if message contains any hidden commands
        for (String command : Bukkit.getInstance().getAllowedCommands()) {
            if (message.contains("issued server command: /" + command)) {
                return onMatch;
            }
        }

        return onMismatch;
    }

    /**
     * Since this implementation inherits from an abstract filter class, we need to override these four
     * methods to make sure that our filter will be applied to all messages.<br><br>
     *
     * Almost all parameter of all methods below are <b>nullable</b>, you don't need to touch them since
     * we already delegate their results to our important, custom filter method above.
     */
    @Override
    public Result filter(LogEvent event) {
        Message msg = event == null ? null : event.getMessage();
        String message = msg == null ? null : (USE_RAW_STRING
                ? msg.getFormat()
                : msg.getFormattedMessage());
        return doFilter(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return doFilter(msg == null ? null : msg.toString());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        return doFilter(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        String message = msg == null ? null : (USE_RAW_STRING
                ? msg.getFormat()
                : msg.getFormattedMessage());
        return doFilter(message);
    }
}