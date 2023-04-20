package ai.timefold.solver.jsonb.api.score.buildin.hardmediumsoftbigdecimal;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.jsonb.api.score.AbstractScoreJsonbAdapter;

public class HardMediumSoftBigDecimalScoreJsonbAdapter extends AbstractScoreJsonbAdapter<HardMediumSoftBigDecimalScore> {

    @Override
    public HardMediumSoftBigDecimalScore adaptFromJson(String scoreString) {
        return HardMediumSoftBigDecimalScore.parseScore(scoreString);
    }
}
