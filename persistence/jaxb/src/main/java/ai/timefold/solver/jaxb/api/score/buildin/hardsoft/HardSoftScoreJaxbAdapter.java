package ai.timefold.solver.jaxb.api.score.buildin.hardsoft;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.jaxb.api.score.AbstractScoreJaxbAdapter;

public class HardSoftScoreJaxbAdapter extends AbstractScoreJaxbAdapter<HardSoftScore> {

    @Override
    public HardSoftScore unmarshal(String scoreString) {
        return HardSoftScore.parseScore(scoreString);
    }

}
