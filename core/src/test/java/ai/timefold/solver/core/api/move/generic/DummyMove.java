package ai.timefold.solver.core.api.move.generic;

import java.util.Collection;
import java.util.Collections;

import ai.timefold.solver.core.api.move.Move;
import ai.timefold.solver.core.api.move.MutableSolutionState;
import ai.timefold.solver.core.api.move.SolutionState;
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
    public void run(MutableSolutionState<TestdataSolution> mutableSolutionState) {
        // do nothing
    }

    @Override
    public Move<TestdataSolution> rebase(SolutionState<TestdataSolution> solutionState) {
        return null;
    }

    @Override
    public Collection<? extends TestdataSolution> getPlanningEntities() {
        return Collections.emptyList();
    }

    @Override
    public Collection<? extends TestdataSolution> getPlanningValues() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return code;
    }

}
