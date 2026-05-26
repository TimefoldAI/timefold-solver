package ai.timefold.solver.model.json.api.schema;

import com.networknt.schema.format.AbstractFormat;

/**
 * This Format implementation verifies the OffsetDateTime (with offset) value.
 * The Schema needs to specify the standard format <code>"date-time"</code>.
 * <p>
 * The motivation is to refer to ISO-8601 format in the error message, as the standard format refers to RFC 3339.
 */
public class OffsetDateTimeFormat extends AbstractFormat {

    public static final String FORMAT = "date-time";

    public OffsetDateTimeFormat() {
        super(FORMAT, "must be a valid ISO-8601 date and time with an offset");
    }

    @Override
    public boolean matches(String value) {
        try {
            java.time.OffsetDateTime.parse(value);
            return true;
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }
}
