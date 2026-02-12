package ai.timefold.solver.jaxb.api.score;

import ai.timefold.solver.core.api.score.HardSoftBigDecimalScore;

public class HardSoftBigDecimalScoreJaxbAdapter extends AbstractScoreJaxbAdapter<HardSoftBigDecimalScore> {

    @Override
    public HardSoftBigDecimalScore unmarshal(String scoreString) {
        return HardSoftBigDecimalScore.parseScore(scoreString);
    }

}
