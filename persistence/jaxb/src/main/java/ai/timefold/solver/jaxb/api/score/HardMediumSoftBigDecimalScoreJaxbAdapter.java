package ai.timefold.solver.jaxb.api.score;

import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;

public class HardMediumSoftBigDecimalScoreJaxbAdapter extends AbstractScoreJaxbAdapter<HardMediumSoftBigDecimalScore> {

    @Override
    public HardMediumSoftBigDecimalScore unmarshal(String scoreString) {
        return HardMediumSoftBigDecimalScore.parseScore(scoreString);
    }

}
