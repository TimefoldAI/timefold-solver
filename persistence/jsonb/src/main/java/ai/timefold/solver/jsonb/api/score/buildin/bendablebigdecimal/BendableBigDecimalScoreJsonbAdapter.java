package ai.timefold.solver.jsonb.api.score.buildin.bendablebigdecimal;

import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import ai.timefold.solver.jsonb.api.score.AbstractScoreJsonbAdapter;

public class BendableBigDecimalScoreJsonbAdapter extends AbstractScoreJsonbAdapter<BendableBigDecimalScore> {

    @Override
    public BendableBigDecimalScore adaptFromJson(String scoreString) {
        return BendableBigDecimalScore.parseScore(scoreString);
    }
}
