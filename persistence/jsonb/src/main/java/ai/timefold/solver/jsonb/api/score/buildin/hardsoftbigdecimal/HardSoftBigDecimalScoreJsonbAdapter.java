package ai.timefold.solver.jsonb.api.score.buildin.hardsoftbigdecimal;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.jsonb.api.score.AbstractScoreJsonbAdapter;

public class HardSoftBigDecimalScoreJsonbAdapter extends AbstractScoreJsonbAdapter<HardSoftBigDecimalScore> {

    @Override
    public HardSoftBigDecimalScore adaptFromJson(String scoreString) {
        return HardSoftBigDecimalScore.parseScore(scoreString);
    }
}
