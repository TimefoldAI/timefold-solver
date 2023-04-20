package ai.timefold.solver.jpa.api.score.buildin.hardsoft;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

@Converter
public class HardSoftScoreConverter implements AttributeConverter<HardSoftScore, String> {

    @Override
    public String convertToDatabaseColumn(HardSoftScore score) {
        if (score == null) {
            return null;
        }

        return score.toString();
    }

    @Override
    public HardSoftScore convertToEntityAttribute(String scoreString) {
        if (scoreString == null) {
            return null;
        }

        return HardSoftScore.parseScore(scoreString);
    }
}
