package ai.timefold.solver.jpa.api.score;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import ai.timefold.solver.core.api.score.BendableScore;

@Converter
public class BendableScoreConverter implements AttributeConverter<BendableScore, String> {

    @Override
    public String convertToDatabaseColumn(BendableScore score) {
        if (score == null) {
            return null;
        }

        return score.toString();
    }

    @Override
    public BendableScore convertToEntityAttribute(String scoreString) {
        if (scoreString == null) {
            return null;
        }

        return BendableScore.parseScore(scoreString);
    }
}
