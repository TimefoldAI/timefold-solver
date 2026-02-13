package ai.timefold.solver.jaxb.api.score;

import ai.timefold.solver.core.api.score.SimpleBigDecimalScore;

public class SimpleBigDecimalScoreJaxbAdapter extends AbstractScoreJaxbAdapter<SimpleBigDecimalScore> {

    @Override
    public SimpleBigDecimalScore unmarshal(String scoreString) {
        return SimpleBigDecimalScore.parseScore(scoreString);
    }

}
