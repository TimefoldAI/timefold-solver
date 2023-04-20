package ai.timefold.solver.jsonb.api.score.buildin.bendablelong;

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.jsonb.api.score.AbstractScoreJsonbAdapter;

public class BendableLongScoreJsonbAdapter extends AbstractScoreJsonbAdapter<BendableLongScore> {

    @Override
    public BendableLongScore adaptFromJson(String scoreString) {
        return BendableLongScore.parseScore(scoreString);
    }
}
