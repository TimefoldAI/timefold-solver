package ai.timefold.solver.model.json.api.schema;

import java.time.DateTimeException;
import java.time.ZoneId;

import com.networknt.schema.format.AbstractFormat;

/**
 * This Format implementation verifies a timezone ID according to {@link ZoneId#of(String)}.
 * The Schema needs to specify the custom format <code>"timezone-id"</code>.
 */
public class ZoneIdFormat extends AbstractFormat {

    public static final String FORMAT = "timezone-id";

    public ZoneIdFormat() {
        super(FORMAT, "must be a valid timezone region-based ID according to IANA TZDB");
    }

    @Override
    public boolean matches(String value) {
        try {
            ZoneId.of(value);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }
}
