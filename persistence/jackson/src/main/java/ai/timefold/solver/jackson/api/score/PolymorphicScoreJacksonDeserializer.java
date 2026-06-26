package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.BendableBigDecimalScore;
import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.SimpleScore;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Jackson binding support for a {@link Score} type (but not a subtype).
 * For a {@link Score} subtype field, use {@link HardSoftScoreJacksonDeserializer} or similar instead.
 * <p>
 * Handles both formats:
 * <ul>
 * <li>Object format (from {@link PolymorphicScoreJacksonSerializer}): {@code {"HardSoftScore":"-999hard/-999soft"}}</li>
 * <li>String format (from type-specific serializer): {@code "-999hard/-999soft"}</li>
 * </ul>
 *
 * @see Score
 * @see PolymorphicScoreJacksonSerializer
 */
public class PolymorphicScoreJacksonDeserializer extends JsonDeserializer<Score> {

    @Override
    public Score deserialize(JsonParser parser, DeserializationContext context) throws java.io.IOException {
        if (parser.currentToken() == JsonToken.START_OBJECT) {
            // Object format: {"HardSoftScore":"-999hard/-999soft"}
            parser.nextToken();
            String scoreClassSimpleName = parser.currentName();
            parser.nextToken();
            String scoreString = parser.getValueAsString();
            parser.nextToken(); // consume END_OBJECT
            return parseScoreByClassName(scoreClassSimpleName, scoreString);
        } else {
            // String format: "-999hard/-999soft"
            String scoreString = parser.getValueAsString();
            return parseScore(scoreString);
        }
    }

    private static Score parseScoreByClassName(String scoreClassSimpleName, String scoreString) {
        if (scoreClassSimpleName.equals(SimpleScore.class.getSimpleName())) {
            return SimpleScore.parseScore(scoreString);
        } else if (scoreClassSimpleName.equals(SimpleBigDecimalScore.class.getSimpleName())) {
            return SimpleBigDecimalScore.parseScore(scoreString);
        } else if (scoreClassSimpleName.equals(HardSoftScore.class.getSimpleName())) {
            return HardSoftScore.parseScore(scoreString);
        } else if (scoreClassSimpleName.equals(HardSoftBigDecimalScore.class.getSimpleName())) {
            return HardSoftBigDecimalScore.parseScore(scoreString);
        } else if (scoreClassSimpleName.equals(HardMediumSoftScore.class.getSimpleName())) {
            return HardMediumSoftScore.parseScore(scoreString);
        } else if (scoreClassSimpleName.equals(HardMediumSoftBigDecimalScore.class.getSimpleName())) {
            return HardMediumSoftBigDecimalScore.parseScore(scoreString);
        } else if (scoreClassSimpleName.equals(BendableScore.class.getSimpleName())) {
            return BendableScore.parseScore(scoreString);
        } else if (scoreClassSimpleName.equals(BendableBigDecimalScore.class.getSimpleName())) {
            return BendableBigDecimalScore.parseScore(scoreString);
        } else {
            throw new IllegalArgumentException("Unrecognized scoreClassSimpleName (%s) for scoreString (%s)."
                    .formatted(scoreClassSimpleName, scoreString));
        }
    }

    /**
     * Try to parse a score string by attempting all known score types.
     * The first successful parse wins.
     */
    static Score parseScore(String scoreString) {
        try {
            return SimpleScore.parseScore(scoreString);
        } catch (Exception e) {
            // Not a SimpleScore, try next
        }
        try {
            return SimpleBigDecimalScore.parseScore(scoreString);
        } catch (Exception e) {
            // Not a SimpleBigDecimalScore, try next
        }
        try {
            return HardSoftScore.parseScore(scoreString);
        } catch (Exception e) {
            // Not a HardSoftScore, try next
        }
        try {
            return HardSoftBigDecimalScore.parseScore(scoreString);
        } catch (Exception e) {
            // Not a HardSoftBigDecimalScore, try next
        }
        try {
            return HardMediumSoftScore.parseScore(scoreString);
        } catch (Exception e) {
            // Not a HardMediumSoftScore, try next
        }
        try {
            return HardMediumSoftBigDecimalScore.parseScore(scoreString);
        } catch (Exception e) {
            // Not a HardMediumSoftBigDecimalScore, try next
        }
        try {
            return BendableScore.parseScore(scoreString);
        } catch (Exception e) {
            // Not a BendableScore, try next
        }
        try {
            return BendableBigDecimalScore.parseScore(scoreString);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse score string (%s).".formatted(scoreString), e);
        }
    }

}
