package ai.timefold.solver.jaxb.api.score.buildin.hardmediumsoft;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.jaxb.api.score.AbstractScoreJaxbAdapter;

public class HardMediumSoftScoreJaxbAdapter extends AbstractScoreJaxbAdapter<HardMediumSoftScore> {

    @Override
    public HardMediumSoftScore unmarshal(String scoreString) {
        return HardMediumSoftScore.parseScore(scoreString);
    }

}
