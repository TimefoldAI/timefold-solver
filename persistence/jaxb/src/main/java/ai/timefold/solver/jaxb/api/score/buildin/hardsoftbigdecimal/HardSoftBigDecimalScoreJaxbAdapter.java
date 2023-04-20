package ai.timefold.solver.jaxb.api.score.buildin.hardsoftbigdecimal;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.jaxb.api.score.AbstractScoreJaxbAdapter;

public class HardSoftBigDecimalScoreJaxbAdapter extends AbstractScoreJaxbAdapter<HardSoftBigDecimalScore> {

    @Override
    public HardSoftBigDecimalScore unmarshal(String scoreString) {
        return HardSoftBigDecimalScore.parseScore(scoreString);
    }

}
