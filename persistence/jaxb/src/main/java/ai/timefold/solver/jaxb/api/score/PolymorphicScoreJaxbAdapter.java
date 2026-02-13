package ai.timefold.solver.jaxb.api.score;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import ai.timefold.solver.core.api.score.BendableBigDecimalScore;
import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.SimpleScore;

public class PolymorphicScoreJaxbAdapter extends XmlAdapter<PolymorphicScoreJaxbAdapter.JaxbAdaptedScore, Score> {

    @Override
    public Score unmarshal(JaxbAdaptedScore jaxbAdaptedScore) {
        if (jaxbAdaptedScore == null) {
            return null;
        }
        String scoreClassName = jaxbAdaptedScore.scoreClassName;
        String scoreString = jaxbAdaptedScore.scoreString;
        // TODO Can this delegate to ScoreUtils.parseScore()?
        if (scoreClassName.equals(SimpleScore.class.getName())) {
            return SimpleScore.parseScore(scoreString);
        } else if (scoreClassName.equals(SimpleBigDecimalScore.class.getName())) {
            return SimpleBigDecimalScore.parseScore(scoreString);
        } else if (scoreClassName.equals(HardSoftScore.class.getName())) {
            return HardSoftScore.parseScore(scoreString);
        } else if (scoreClassName.equals(HardSoftBigDecimalScore.class.getName())) {
            return HardSoftBigDecimalScore.parseScore(scoreString);
        } else if (scoreClassName.equals(HardMediumSoftScore.class.getName())) {
            return HardMediumSoftScore.parseScore(scoreString);
        } else if (scoreClassName.equals(HardMediumSoftBigDecimalScore.class.getName())) {
            return HardMediumSoftBigDecimalScore.parseScore(scoreString);
        } else if (scoreClassName.equals(BendableScore.class.getName())) {
            return BendableScore.parseScore(scoreString);
        } else if (scoreClassName.equals(BendableBigDecimalScore.class.getName())) {
            return BendableBigDecimalScore.parseScore(scoreString);
        } else {
            throw new IllegalArgumentException("Unrecognized scoreClassName (%s) for scoreString (%s)."
                    .formatted(scoreClassName, scoreString));
        }
    }

    @Override
    public JaxbAdaptedScore marshal(Score score) {
        if (score == null) {
            return null;
        }
        return new JaxbAdaptedScore(score);
    }

    static class JaxbAdaptedScore {

        @XmlAttribute(name = "class")
        private String scoreClassName;
        @XmlValue
        private String scoreString;

        private JaxbAdaptedScore() {
            // Required by JAXB
        }

        public JaxbAdaptedScore(Score score) {
            this.scoreClassName = score.getClass().getName();
            this.scoreString = score.toString();
        }

        String getScoreClassName() {
            return scoreClassName;
        }

        String getScoreString() {
            return scoreString;
        }
    }
}
