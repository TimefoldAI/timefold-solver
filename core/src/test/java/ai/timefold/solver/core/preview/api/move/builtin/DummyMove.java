package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Collections;
import java.util.Objects;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testutil.CodeAssertable;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class DummyMove implements Move<TestdataSolution>, CodeAssertable {

    private @Nullable String code;

    public DummyMove() {
    }

    public DummyMove(String code) {
        this.code = code;
    }

    @Override
    public @Nullable String getCode() {
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
    public Move<TestdataSolution> rebase(Lookup lookup) {
        return this;
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        return Collections.emptyList();
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return Objects.requireNonNull(code, "null");
    }

}
