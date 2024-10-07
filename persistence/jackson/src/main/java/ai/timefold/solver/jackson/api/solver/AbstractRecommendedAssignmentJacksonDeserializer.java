package ai.timefold.solver.jackson.api.solver;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.solver.RecommendedAssignment;
import ai.timefold.solver.core.impl.solver.DefaultRecommendedAssignment;
import ai.timefold.solver.jackson.api.score.analysis.AbstractScoreAnalysisJacksonDeserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Extend this to implement {@link RecommendedAssignment} deserialization specific for your domain.
 * Make sure to also:
 *
 * <ul>
 * <li>extend {@link AbstractScoreAnalysisJacksonDeserializer},</li>
 * <li>provide deserializer for {@link AbstractRecommendedAssignmentJacksonDeserializer#getPropositionClass()}</li>
 * <li>and finally add all of these to the Jackson {@link ObjectMapper}.</li>
 * </ul>
 *
 * @param <Score_>
 */
public abstract class AbstractRecommendedAssignmentJacksonDeserializer<Proposition_, Score_ extends Score<Score_>>
        extends JsonDeserializer<RecommendedAssignment<Proposition_, Score_>> {

    /**
     * {@link DefaultRecommendedAssignment} requires ID for purposes of ordering,
     * to break ties if two instances have the same score.
     * This ID has no other effect on the instances.
     *
     * <p>
     * This counter is used to generate an ever-increasing ID.
     */
    private final AtomicLong ID_COUNTER = new AtomicLong(0L);

    @Override
    public final RecommendedAssignment<Proposition_, Score_> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = p.readValueAsTree();
        Proposition_ proposition = ctxt.readTreeAsValue(node.get("proposition"), getPropositionClass());
        ScoreAnalysis<Score_> diff = ctxt.readTreeAsValue(node.get("scoreDiff"), ScoreAnalysis.class);
        return new DefaultRecommendedAssignment<>(ID_COUNTER.getAndIncrement(), proposition, diff);
    }

    /**
     * Each {@link RecommendedAssignment} has a proposition, which is a custom object returned by the user.
     * It is therefore the user and only the user who can deserialize the proposition.
     * This type must have a deserializer registered.
     *
     * @return never null
     */
    protected abstract Class<Proposition_> getPropositionClass();

}
