package ai.timefold.solver.core.impl.io.jaxb;

import java.time.Duration;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class JaxbDurationAdapter extends XmlAdapter<String, Duration> {

    @Override
    public @Nullable Duration unmarshal(@Nullable String durationString) {
        if (durationString == null) {
            return null;
        }
        return Duration.parse(durationString);
    }

    @Override
    public @Nullable String marshal(@Nullable Duration duration) {
        if (duration == null) {
            return null;
        }
        return duration.toString();
    }
}
