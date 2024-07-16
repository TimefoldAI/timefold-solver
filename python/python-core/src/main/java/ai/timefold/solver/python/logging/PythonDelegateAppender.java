package ai.timefold.solver.python.logging;

import java.util.function.Consumer;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class PythonDelegateAppender extends AppenderBase<ILoggingEvent> {
    private static Consumer<PythonLoggingEvent> logEventConsumer;

    public static void setLogEventConsumer(Consumer<PythonLoggingEvent> logEventConsumer) {
        PythonDelegateAppender.logEventConsumer = logEventConsumer;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        logEventConsumer.accept(
                new PythonLoggingEvent(
                        PythonLogLevel.fromJavaLevel(eventObject.getLevel()),
                        eventObject.getFormattedMessage()));
    }
}
