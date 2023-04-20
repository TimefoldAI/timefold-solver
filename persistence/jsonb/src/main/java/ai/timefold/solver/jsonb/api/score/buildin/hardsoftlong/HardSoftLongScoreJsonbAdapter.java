package ai.timefold.solver.jsonb.api.score.buildin.hardsoftlong;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.jsonb.api.score.AbstractScoreJsonbAdapter;

public class HardSoftLongScoreJsonbAdapter extends AbstractScoreJsonbAdapter<HardSoftLongScore> {

    @Override
    public HardSoftLongScore adaptFromJson(String scoreString) {
        return HardSoftLongScore.parseScore(scoreString);
    }
}
