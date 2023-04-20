package ai.timefold.solver.jaxb.api.score.buildin.hardmediumsoftbigdecimal;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.jaxb.api.score.AbstractScoreJaxbAdapter;

public class HardMediumSoftBigDecimalScoreJaxbAdapter extends AbstractScoreJaxbAdapter<HardMediumSoftBigDecimalScore> {

    @Override
    public HardMediumSoftBigDecimalScore unmarshal(String scoreString) {
        return HardMediumSoftBigDecimalScore.parseScore(scoreString);
    }

}
