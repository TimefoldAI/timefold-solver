package ai.timefold.solver.core.preview.api.move.generic;

import java.util.Collection;
import java.util.Collections;

import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.util.CodeAssertable;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NonNull;

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
    public void execute(@NonNull MutableSolutionView<TestdataSolution> solutionView) {
        // do nothing
    }

    @Override
    public @NonNull Move<TestdataSolution> rebase(@NonNull Rebaser rebaser) {
        return null;
    }

    @Override
    public @NonNull Collection<? extends TestdataSolution> extractPlanningEntities() {
        return Collections.emptyList();
    }

    @Override
    public @NonNull Collection<? extends TestdataSolution> extractPlanningValues() {
        return Collections.emptyList();
    }

    @Override
    public @NonNull String toString() {
        return code;
    }

}
