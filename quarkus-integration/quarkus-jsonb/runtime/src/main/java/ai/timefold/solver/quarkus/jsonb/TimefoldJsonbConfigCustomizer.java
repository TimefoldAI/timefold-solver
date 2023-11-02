package ai.timefold.solver.quarkus.jsonb;

import jakarta.inject.Singleton;
import jakarta.json.bind.JsonbConfig;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.jsonb.api.TimefoldJsonbConfig;

import io.quarkus.jsonb.JsonbConfigCustomizer;

/**
 * Timefold doesn't use JSON-B, but it does have optional JSON-B support for {@link Score}, etc.
 *
 * @deprecated Prefer Jackson integration instead.
 */
@Singleton
@Deprecated(forRemoval = true, since = "1.4.0")
public class TimefoldJsonbConfigCustomizer implements JsonbConfigCustomizer {

    @Override
    public void customize(JsonbConfig config) {
        config.withAdapters(TimefoldJsonbConfig.getScoreJsonbAdapters());
    }
}
