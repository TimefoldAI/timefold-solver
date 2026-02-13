package ai.timefold.solver.jaxb.api.score;

import ai.timefold.solver.core.api.score.SimpleScore;

public class SimpleScoreJaxbAdapter extends AbstractScoreJaxbAdapter<SimpleScore> {

    @Override
    public SimpleScore unmarshal(String scoreString) {
        return SimpleScore.parseScore(scoreString);
    }

}
