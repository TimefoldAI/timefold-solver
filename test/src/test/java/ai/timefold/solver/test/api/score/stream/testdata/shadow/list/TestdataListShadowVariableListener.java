package ai.timefold.solver.test.api.score.stream.testdata.shadow.list;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import org.jspecify.annotations.NonNull;

public class TestdataListShadowVariableListener
        implements VariableListener<TestdataListShadowConstraintVerifierSolution, TestdataListShadowConstraintVerifierValue> {

    @Override
    public void beforeVariableChanged(@NonNull ScoreDirector<TestdataListShadowConstraintVerifierSolution> scoreDirector,
            @NonNull TestdataListShadowConstraintVerifierValue value) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(@NonNull ScoreDirector<TestdataListShadowConstraintVerifierSolution> scoreDirector,
            @NonNull TestdataListShadowConstraintVerifierValue value) {
        scoreDirector.beforeVariableChanged(value, "otherCascadingValue");
        value.setListenerValue(value.getIndex() + 20);
        scoreDirector.afterVariableChanged(value, "otherCascadingValue");
    }

    @Override
    public void beforeEntityAdded(@NonNull ScoreDirector<TestdataListShadowConstraintVerifierSolution> scoreDirector,
            @NonNull TestdataListShadowConstraintVerifierValue value) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector<TestdataListShadowConstraintVerifierSolution> scoreDirector,
            @NonNull TestdataListShadowConstraintVerifierValue value) {
        // Do nothing
    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector<TestdataListShadowConstraintVerifierSolution> scoreDirector,
            @NonNull TestdataListShadowConstraintVerifierValue value) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector<TestdataListShadowConstraintVerifierSolution> scoreDirector,
            @NonNull TestdataListShadowConstraintVerifierValue value) {
        // Do nothing
    }
}
