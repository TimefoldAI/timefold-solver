package ai.timefold.solver.jsonb.api.score.buildin.hardsoft;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.jsonb.api.score.AbstractScoreJsonbAdapter;

public class HardSoftScoreJsonbAdapter extends AbstractScoreJsonbAdapter<HardSoftScore> {

    @Override
    public HardSoftScore adaptFromJson(String scoreString) {
        return HardSoftScore.parseScore(scoreString);
    }
}
