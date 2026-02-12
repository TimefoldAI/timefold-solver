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
import ai.timefold.solver.jackson.api.score.buildin.HardSoftScoreJacksonDeserializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Jackson binding support for a {@link Score} type (but not a subtype).
 * For a {@link Score} subtype field, use {@link HardSoftScoreJacksonDeserializer} or similar instead.
 * <p>
 * For example: use
 * {@code @JsonSerialize(using = PolymorphicScoreJacksonSerializer.class) @JsonDeserialize(using = PolymorphicScoreJacksonDeserializer.class)}
 * on a {@code Score score} field which contains a {@link HardSoftScore} instance
 * and it will marshalled to JSON as {@code "score":{"type":"HARD_SOFT",score:"-999hard/-999soft"}}.
 *
 * @see Score
 * @see PolymorphicScoreJacksonDeserializer
 */
public class PolymorphicScoreJacksonDeserializer extends ValueDeserializer<Score> {

    @Override
    public Score deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        parser.nextToken();
        String scoreClassSimpleName = parser.currentName();
        parser.nextToken();
        String scoreString = parser.getValueAsString();
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

}
