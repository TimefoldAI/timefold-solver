package ai.timefold.solver.jaxb.api.score;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;

public class HardMediumSoftScoreJaxbAdapter extends AbstractScoreJaxbAdapter<HardMediumSoftScore> {

    @Override
    public HardMediumSoftScore unmarshal(String scoreString) {
        return HardMediumSoftScore.parseScore(scoreString);
    }

}
