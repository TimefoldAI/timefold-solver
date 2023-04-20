package ai.timefold.solver.quarkus.jsonb;

import javax.inject.Singleton;
import javax.json.bind.JsonbConfig;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.jsonb.api.OptaPlannerJsonbConfig;

import io.quarkus.jsonb.JsonbConfigCustomizer;

/**
 * OptaPlanner doesn't use JSON-B, but it does have optional JSON-B support for {@link Score}, etc.
 */
@Singleton
public class OptaPlannerJsonbConfigCustomizer implements JsonbConfigCustomizer {

    @Override
    public void customize(JsonbConfig config) {
        config.withAdapters(OptaPlannerJsonbConfig.getScoreJsonbAdapters());
    }
}
