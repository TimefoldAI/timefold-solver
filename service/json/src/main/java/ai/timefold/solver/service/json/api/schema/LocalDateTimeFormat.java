package ai.timefold.solver.service.json.api.schema;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import com.networknt.schema.format.AbstractFormat;

/**
 * This Format implementation verifies the LocalDateTime (without offset) value.
 * The Schema needs to specify the custom format <code>"local-date-time"</code>.
 */
public class LocalDateTimeFormat extends AbstractFormat {

    public static final String FORMAT = "local-date-time";

    public LocalDateTimeFormat() {
        super(FORMAT, "must be a valid ISO-8601 local date and time");
    }

    @Override
    public boolean matches(String value) {
        try {
            LocalDateTime.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}