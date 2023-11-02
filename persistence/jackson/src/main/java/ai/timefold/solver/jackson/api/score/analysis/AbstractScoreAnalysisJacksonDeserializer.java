package ai.timefold.solver.jackson.api.score.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ConstraintAnalysis;
import ai.timefold.solver.core.api.score.analysis.MatchAnalysis;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Extend this to implement {@link ScoreAnalysis} deserialization specific for your domain.
 *
 * @param <Score_>
 */
public abstract class AbstractScoreAnalysisJacksonDeserializer<Score_ extends Score<Score_>>
        extends JsonDeserializer<ScoreAnalysis<Score_>> {

    @Override
    public final ScoreAnalysis<Score_> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        var score = parseScore(node.get("score").asText());
        var constraintAnalysisList = new HashMap<ConstraintRef, ConstraintAnalysis<Score_>>();
        node.get("constraints").forEach(constraintNode -> {
            var constraintPackage = constraintNode.get("package").asText();
            var constraintName = constraintNode.get("name").asText();
            var constraintRef = ConstraintRef.of(constraintPackage, constraintName);
            var constraintScore = parseScore(constraintNode.get("score").asText());
            var matchScoreList = new ArrayList<MatchAnalysis<Score_>>();
            JsonNode matchesNode = constraintNode.get("matches");
            if (matchesNode == null) {
                constraintAnalysisList.put(constraintRef, new ConstraintAnalysis<>(constraintScore, null));
            } else {
                constraintNode.get("matches").forEach(matchNode -> {
                    var matchScore = parseScore(matchNode.get("score").asText());
                    var justificationString = matchNode.get("justification").toString();
                    var justification = parseConstraintJustification(constraintRef, justificationString, matchScore);
                    matchScoreList.add(new MatchAnalysis<>(matchScore, justification));
                });
                constraintAnalysisList.put(constraintRef,
                        new ConstraintAnalysis<>(constraintScore, matchScoreList));
            }
        });
        return new ScoreAnalysis<>(score, constraintAnalysisList);
    }

    /**
     * The domain is based on a single {@link Score} subtype.
     * This method is responsible for parsing the score string into that subtype.
     *
     * @param scoreString never null
     * @return never null
     */
    protected abstract Score_ parseScore(String scoreString);

    /**
     * Each {@link Constraint} in the {@link ConstraintProvider} is justified
     * with a custom implementation {@link ConstraintJustification}.
     * This method is responsible for parsing the justification string into that subtype.
     *
     * @param constraintRef never null
     * @param constraintJustificationString never null
     * @param score never null
     * @return never null
     * @param <ConstraintJustification_> Domain-specific custom implementation, typically constraint-specific.
     */
    protected abstract <ConstraintJustification_ extends ConstraintJustification> ConstraintJustification_
            parseConstraintJustification(ConstraintRef constraintRef, String constraintJustificationString, Score_ score);

}
