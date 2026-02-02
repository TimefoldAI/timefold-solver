package ai.timefold.solver.jackson3.api.solver;

import java.util.concurrent.atomic.AtomicLong;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.impl.solver.DefaultRecommendedFit;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * @deprecated Prefer to use the {@link AbstractRecommendedAssignmentJacksonDeserializer} instead.
 */
@Deprecated(forRemoval = true, since = "1.15.0")
public abstract class AbstractRecommendedFitJacksonDeserializer<Proposition_, Score_ extends Score<Score_>>
        extends ValueDeserializer<RecommendedFit<Proposition_, Score_>> {

    /**
     * {@link DefaultRecommendedFit} requires ID for purposes of ordering,
     * to break ties if two instances have the same score.
     * This ID has no other effect on the instances.
     *
     * <p>
     * This counter is used to generate an ever-increasing ID.
     */
    private final AtomicLong ID_COUNTER = new AtomicLong(0L);

    @Override
    public final RecommendedFit<Proposition_, Score_> deserialize(JsonParser p, DeserializationContext ctxt)
            throws JacksonException {
        JsonNode node = p.readValueAsTree();
        Proposition_ proposition = ctxt.readTreeAsValue(node.get("proposition"), getPropositionClass());
        ScoreAnalysis<Score_> diff = ctxt.readTreeAsValue(node.get("scoreDiff"), ScoreAnalysis.class);
        return new DefaultRecommendedFit<>(ID_COUNTER.getAndIncrement(), proposition, diff);
    }

    /**
     * Each {@link RecommendedFit} has a proposition, which is a custom object returned by the user.
     * It is therefore the user and only the user who can deserialize the proposition.
     * This type must have a deserializer registered.
     *
     * @return never null
     */
    protected abstract Class<Proposition_> getPropositionClass();

}
