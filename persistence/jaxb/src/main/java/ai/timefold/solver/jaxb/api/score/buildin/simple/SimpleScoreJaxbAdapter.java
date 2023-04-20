package ai.timefold.solver.jaxb.api.score.buildin.simple;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.jaxb.api.score.AbstractScoreJaxbAdapter;

public class SimpleScoreJaxbAdapter extends AbstractScoreJaxbAdapter<SimpleScore> {

    @Override
    public SimpleScore unmarshal(String scoreString) {
        return SimpleScore.parseScore(scoreString);
    }

}
