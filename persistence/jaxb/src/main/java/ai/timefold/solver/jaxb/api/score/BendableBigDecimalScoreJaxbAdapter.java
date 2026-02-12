package ai.timefold.solver.jaxb.api.score;

import ai.timefold.solver.core.api.score.BendableBigDecimalScore;

public class BendableBigDecimalScoreJaxbAdapter extends AbstractScoreJaxbAdapter<BendableBigDecimalScore> {

    @Override
    public BendableBigDecimalScore unmarshal(String scoreString) {
        return BendableBigDecimalScore.parseScore(scoreString);
    }

}
