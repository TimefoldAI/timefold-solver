package ai.timefold.solver.core.testdomain.constraintconfiguration;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.testdomain.TestdataEntity;

import org.jspecify.annotations.NonNull;

@Deprecated(forRemoval = true, since = "1.13.0")
public final class TestdataConstraintWeighIncrementalScoreCalculator
        implements IncrementalScoreCalculator<TestdataConstraintConfigurationSolution, SimpleScore> {

    private TestdataConstraintConfigurationSolution workingSolution;
    private List<TestdataEntity> entityList;

    @Override
    public void resetWorkingSolution(@NonNull TestdataConstraintConfigurationSolution workingSolution) {
        this.workingSolution = workingSolution;
        this.entityList = new ArrayList<>(workingSolution.getEntityList());
    }

    @Override
    public void beforeEntityAdded(@NonNull Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityAdded(@NonNull Object entity) {
        entityList.add((TestdataEntity) entity);
    }

    @Override
    public void beforeVariableChanged(@NonNull Object entity, @NonNull String variableName) {
        throw new UnsupportedOperationException(); // Will not be called.
    }

    @Override
    public void afterVariableChanged(@NonNull Object entity, @NonNull String variableName) {
        throw new UnsupportedOperationException(); // Will not be called.
    }

    @Override
    public void beforeEntityRemoved(@NonNull Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityRemoved(@NonNull Object entity) {
        entityList.remove((TestdataEntity) entity);
    }

    @Override
    public @NonNull SimpleScore calculateScore() {
        SimpleScore constraintWeight = workingSolution.getConstraintConfiguration().getFirstWeight();
        return constraintWeight.multiply(entityList.size());
    }
}
