package ai.timefold.solver.jsonb.api.score.buildin.simplebigdecimal;

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.jsonb.api.score.AbstractScoreJsonbAdapter;

public class SimpleBigDecimalScoreJsonbAdapter extends AbstractScoreJsonbAdapter<SimpleBigDecimalScore> {

    @Override
    public SimpleBigDecimalScore adaptFromJson(String scoreString) {
        return SimpleBigDecimalScore.parseScore(scoreString);
    }
}
