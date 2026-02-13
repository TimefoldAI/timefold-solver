package ai.timefold.solver.jpa.api.score;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import ai.timefold.solver.core.api.score.SimpleScore;

@Converter
public class SimpleScoreConverter implements AttributeConverter<SimpleScore, String> {

    @Override
    public String convertToDatabaseColumn(SimpleScore score) {
        if (score == null) {
            return null;
        }

        return score.toString();
    }

    @Override
    public SimpleScore convertToEntityAttribute(String scoreString) {
        if (scoreString == null) {
            return null;
        }

        return SimpleScore.parseScore(scoreString);
    }
}
