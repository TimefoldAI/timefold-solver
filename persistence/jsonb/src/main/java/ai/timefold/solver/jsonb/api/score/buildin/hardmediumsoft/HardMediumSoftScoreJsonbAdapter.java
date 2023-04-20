package ai.timefold.solver.jsonb.api.score.buildin.hardmediumsoft;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.jsonb.api.score.AbstractScoreJsonbAdapter;

public class HardMediumSoftScoreJsonbAdapter extends AbstractScoreJsonbAdapter<HardMediumSoftScore> {

    @Override
    public HardMediumSoftScore adaptFromJson(String scoreString) {
        return HardMediumSoftScore.parseScore(scoreString);
    }
}
