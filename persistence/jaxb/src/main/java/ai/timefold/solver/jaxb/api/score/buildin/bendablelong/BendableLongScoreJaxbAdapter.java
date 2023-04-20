package ai.timefold.solver.jaxb.api.score.buildin.bendablelong;

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.jaxb.api.score.AbstractScoreJaxbAdapter;

public class BendableLongScoreJaxbAdapter extends AbstractScoreJaxbAdapter<BendableLongScore> {

    @Override
    public BendableLongScore unmarshal(String scoreString) {
        return BendableLongScore.parseScore(scoreString);
    }

}
