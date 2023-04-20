package ai.timefold.solver.jaxb.api.score.buildin.simplebigdecimal;

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.jaxb.api.score.AbstractScoreJaxbAdapter;

public class SimpleBigDecimalScoreJaxbAdapter extends AbstractScoreJaxbAdapter<SimpleBigDecimalScore> {

    @Override
    public SimpleBigDecimalScore unmarshal(String scoreString) {
        return SimpleBigDecimalScore.parseScore(scoreString);
    }

}
