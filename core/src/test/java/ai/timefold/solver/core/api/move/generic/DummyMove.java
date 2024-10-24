package ai.timefold.solver.core.api.move.generic;

import java.util.Collection;
import java.util.Collections;

import ai.timefold.solver.core.api.move.Move;
import ai.timefold.solver.core.api.move.MutableSolutionView;
import ai.timefold.solver.core.api.move.Rebaser;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.util.CodeAssertable;

public final class DummyMove implements Move<TestdataSolution>, CodeAssertable {

    private String code;

    public DummyMove() {
    }

    public DummyMove(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @Override
    public void execute(MutableSolutionView<TestdataSolution> solutionView) {
        // do nothing
    }

    @Override
    public Move<TestdataSolution> rebase(Rebaser rebaser) {
        return null;
    }

    @Override
    public Collection<? extends TestdataSolution> extractPlanningEntities() {
        return Collections.emptyList();
    }

    @Override
    public Collection<? extends TestdataSolution> extractPlanningValues() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return code;
    }

}
