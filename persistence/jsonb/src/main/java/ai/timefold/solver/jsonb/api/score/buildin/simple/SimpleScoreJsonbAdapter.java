package ai.timefold.solver.jsonb.api.score.buildin.simple;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.jsonb.api.score.AbstractScoreJsonbAdapter;

public class SimpleScoreJsonbAdapter extends AbstractScoreJsonbAdapter<SimpleScore> {

    @Override
    public SimpleScore adaptFromJson(String scoreString) {
        return SimpleScore.parseScore(scoreString);
    }
}
