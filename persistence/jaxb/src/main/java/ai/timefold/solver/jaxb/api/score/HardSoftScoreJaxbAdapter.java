package ai.timefold.solver.jaxb.api.score;

import ai.timefold.solver.core.api.score.HardSoftScore;

public class HardSoftScoreJaxbAdapter extends AbstractScoreJaxbAdapter<HardSoftScore> {

    @Override
    public HardSoftScore unmarshal(String scoreString) {
        return HardSoftScore.parseScore(scoreString);
    }

}
