package ai.timefold.solver.jpa.api.score;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;

@Converter
public class HardMediumSoftBigDecimalScoreConverter implements AttributeConverter<HardMediumSoftBigDecimalScore, String> {

    @Override
    public String convertToDatabaseColumn(HardMediumSoftBigDecimalScore score) {
        if (score == null) {
            return null;
        }

        return score.toString();
    }

    @Override
    public HardMediumSoftBigDecimalScore convertToEntityAttribute(String scoreString) {
        if (scoreString == null) {
            return null;
        }

        return HardMediumSoftBigDecimalScore.parseScore(scoreString);
    }
}
