package ai.timefold.solver.examples.nurserostering.domain.solver;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.examples.nurserostering.domain.NurseRoster;
import ai.timefold.solver.examples.nurserostering.domain.ShiftAssignment;

public class MovableShiftAssignmentSelectionFilter implements SelectionFilter<NurseRoster, ShiftAssignment> {

    private final PinningFilter<NurseRoster, ShiftAssignment> pinningFilter =
            new ShiftAssignmentPinningFilter();

    @Override
    public boolean accept(ScoreDirector<NurseRoster> scoreDirector, ShiftAssignment selection) {
        return !pinningFilter.accept(scoreDirector.getWorkingSolution(), selection);
    }

}
