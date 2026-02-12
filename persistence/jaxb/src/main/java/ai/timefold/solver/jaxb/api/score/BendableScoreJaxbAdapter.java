package ai.timefold.solver.jaxb.api.score;

import ai.timefold.solver.core.api.score.BendableScore;

public class BendableScoreJaxbAdapter extends AbstractScoreJaxbAdapter<BendableScore> {

    @Override
    public BendableScore unmarshal(String scoreString) {
        return BendableScore.parseScore(scoreString);
    }

}
