package ai.timefold.solver.jpa.api.score.buildin.simplebigdecimal;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;

@Converter
public class SimpleBigDecimalScoreConverter implements AttributeConverter<SimpleBigDecimalScore, String> {

    @Override
    public String convertToDatabaseColumn(SimpleBigDecimalScore score) {
        if (score == null) {
            return null;
        }

        return score.toString();
    }

    @Override
    public SimpleBigDecimalScore convertToEntityAttribute(String scoreString) {
        if (scoreString == null) {
            return null;
        }

        return SimpleBigDecimalScore.parseScore(scoreString);
    }
}
