package ai.timefold.solver.jpa.api.score.buildin.hardsoftlong;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

@Converter
public class HardSoftLongScoreConverter implements AttributeConverter<HardSoftLongScore, String> {

    @Override
    public String convertToDatabaseColumn(HardSoftLongScore score) {
        if (score == null) {
            return null;
        }

        return score.toString();
    }

    @Override
    public HardSoftLongScore convertToEntityAttribute(String scoreString) {
        if (scoreString == null) {
            return null;
        }

        return HardSoftLongScore.parseScore(scoreString);
    }
}
