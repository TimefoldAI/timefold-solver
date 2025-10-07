package ai.timefold.solver.core.api.score.stream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface StaticDataSupplier<Stream_ extends ConstraintStream> {
    Stream_ get(StaticDataFactory dataFactory);
}
