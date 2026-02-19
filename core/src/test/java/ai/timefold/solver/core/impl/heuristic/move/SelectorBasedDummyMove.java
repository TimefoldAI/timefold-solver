package ai.timefold.solver.core.impl.heuristic.move;

import java.util.Collections;
import java.util.SequencedCollection;

import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testutil.CodeAssertable;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class SelectorBasedDummyMove extends AbstractSelectorBasedMove<TestdataSolution> implements CodeAssertable {

    protected @Nullable String code;

    public SelectorBasedDummyMove() {
    }

    public SelectorBasedDummyMove(String code) {
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
    protected void execute(VariableDescriptorAwareScoreDirector<TestdataSolution> scoreDirector) {
        // do nothing
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
        return code;
    }

}
