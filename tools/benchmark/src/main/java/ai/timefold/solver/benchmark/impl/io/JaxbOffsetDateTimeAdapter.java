package ai.timefold.solver.benchmark.impl.io;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class JaxbOffsetDateTimeAdapter extends XmlAdapter<String, OffsetDateTime> {

    private final DateTimeFormatter formatter;

    public JaxbOffsetDateTimeAdapter() {
        formatter = new DateTimeFormatterBuilder()
                .appendPattern("uuuu-MM-dd'T'HH:mm:ss")
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .appendOffsetId()
                .toFormatter();
    }

    @Override
    public @Nullable OffsetDateTime unmarshal(@Nullable String offsetDateTimeString) {
        if (offsetDateTimeString == null) {
            return null;
        }
        try {
            return OffsetDateTime.from(formatter.parse(offsetDateTimeString));
        } catch (DateTimeException e) {
            throw new IllegalStateException("Failed to convert string (" + offsetDateTimeString + ") to type ("
                    + OffsetDateTime.class.getName() + ").");
        }
    }

    @Override
    public @Nullable String marshal(@Nullable OffsetDateTime offsetDateTimeObject) {
        if (offsetDateTimeObject == null) {
            return null;
        }
        return formatter.format(offsetDateTimeObject);
    }
}
