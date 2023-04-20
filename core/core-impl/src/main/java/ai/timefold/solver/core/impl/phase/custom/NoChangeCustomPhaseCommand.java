package ai.timefold.solver.core.impl.phase.custom;

import ai.timefold.solver.core.api.score.director.ScoreDirector;

/**
 * Makes no changes.
 */
public class NoChangeCustomPhaseCommand implements CustomPhaseCommand<Object> {

    @Override
    public void changeWorkingSolution(ScoreDirector<Object> scoreDirector) {
        // Do nothing
    }

}
