package ai.timefold.solver.tools.maven.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.plugin.logging.Log;

public class InMemoryMojoLog implements Log {

    public enum Level {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    public static class Event {
        public final Level level;
        public final String message;
        public final Throwable throwable;

        public Event(Level level, String message, Throwable throwable) {
            this.level = level;
            this.message = message;
            this.throwable = throwable;
        }

        @Override
        public String toString() {
            return "[" + level + "] " + message + (throwable != null ? (" - " + throwable) : "");
        }
    }

    private final List<Event> events = new CopyOnWriteArrayList<>();

    private void emit(Event e) {
        events.add(e);
        // also print to System.out for easier debugging during tests
        try {
            System.out.println(e.toString());
            if (e.throwable != null) {
                e.throwable.printStackTrace(System.out);
            }
        } catch (Throwable t) {
            // avoid any logging side-effect from breaking tests
            // swallow
        }
    }

    @Override
    public void debug(CharSequence content) {
        emit(new Event(Level.DEBUG, String.valueOf(content), null));
    }

    @Override
    public void debug(CharSequence content, Throwable t) {
        emit(new Event(Level.DEBUG, String.valueOf(content), t));
    }

    @Override
    public void debug(Throwable t) {
        emit(new Event(Level.DEBUG, t == null ? null : t.toString(), t));
    }

    @Override
    public void info(CharSequence content) {
        emit(new Event(Level.INFO, String.valueOf(content), null));
    }

    @Override
    public void info(CharSequence content, Throwable t) {
        emit(new Event(Level.INFO, String.valueOf(content), t));
    }

    @Override
    public void info(Throwable t) {
        emit(new Event(Level.INFO, t == null ? null : t.toString(), t));
    }

    @Override
    public void warn(CharSequence content) {
        emit(new Event(Level.WARN, String.valueOf(content), null));
    }

    @Override
    public void warn(CharSequence content, Throwable t) {
        emit(new Event(Level.WARN, String.valueOf(content), t));
    }

    @Override
    public void warn(Throwable t) {
        emit(new Event(Level.WARN, t == null ? null : t.toString(), t));
    }

    @Override
    public void error(CharSequence content) {
        emit(new Event(Level.ERROR, String.valueOf(content), null));
    }

    @Override
    public void error(CharSequence content, Throwable t) {
        emit(new Event(Level.ERROR, String.valueOf(content), t));
    }

    @Override
    public void error(Throwable t) {
        emit(new Event(Level.ERROR, t == null ? null : t.toString(), t));
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    public List<Event> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public List<String> messages() {
        return events.stream().map(e -> e.message).collect(Collectors.toList());
    }

    public boolean contains(String regex, Level level) {
        Objects.requireNonNull(regex);
        Objects.requireNonNull(level);
        Pattern p = Pattern.compile(regex);
        return events.stream().anyMatch(e -> e.level == level && e.message != null && p.matcher(e.message).find());
    }

    public void assertContains(String regex, Level level) {
        Objects.requireNonNull(regex, "regex must not be null");
        Objects.requireNonNull(level, "level must not be null");
        if (contains(regex, level)) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Expected log to contain regex '").append(regex).append("' at level ").append(level)
                .append(", but it did not.\nCaptured events:\n");
        for (Event e : events) {
            sb.append(e.toString()).append('\n');
        }
        throw new AssertionError(sb.toString());
    }

    public void clear() {
        events.clear();
    }
}
