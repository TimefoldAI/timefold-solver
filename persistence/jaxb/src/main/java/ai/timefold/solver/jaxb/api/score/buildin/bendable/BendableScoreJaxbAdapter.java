package ai.timefold.solver.jaxb.api.score.buildin.bendable;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.jaxb.api.score.AbstractScoreJaxbAdapter;

public class BendableScoreJaxbAdapter extends AbstractScoreJaxbAdapter<BendableScore> {

    @Override
    public BendableScore unmarshal(String scoreString) {
        return BendableScore.parseScore(scoreString);
    }

}
