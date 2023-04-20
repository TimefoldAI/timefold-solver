package ai.timefold.solver.jaxb.api.score.buildin.simplelong;

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.jaxb.api.score.AbstractScoreJaxbAdapter;

public class SimpleLongScoreJaxbAdapter extends AbstractScoreJaxbAdapter<SimpleLongScore> {

    @Override
    public SimpleLongScore unmarshal(String scoreString) {
        return SimpleLongScore.parseScore(scoreString);
    }

}
