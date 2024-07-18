package ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;

@Deprecated(forRemoval = true, since = "1.13.0")
public final class TestdataConstraintWeighIncrementalScoreCalculator
        implements IncrementalScoreCalculator<TestdataConstraintConfigurationSolution, SimpleScore> {

    private TestdataConstraintConfigurationSolution workingSolution;
    private List<TestdataEntity> entityList;

    @Override
    public void resetWorkingSolution(TestdataConstraintConfigurationSolution workingSolution) {
        this.workingSolution = workingSolution;
        this.entityList = new ArrayList<>(workingSolution.getEntityList());
    }

    @Override
    public void beforeEntityAdded(Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityAdded(Object entity) {
        entityList.add((TestdataEntity) entity);
    }

    @Override
    public void beforeVariableChanged(Object entity, String variableName) {
        throw new UnsupportedOperationException(); // Will not be called.
    }

    @Override
    public void afterVariableChanged(Object entity, String variableName) {
        throw new UnsupportedOperationException(); // Will not be called.
    }

    @Override
    public void beforeEntityRemoved(Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityRemoved(Object entity) {
        entityList.remove((TestdataEntity) entity);
    }

    @Override
    public SimpleScore calculateScore() {
        SimpleScore constraintWeight = workingSolution.getConstraintConfiguration().getFirstWeight();
        return constraintWeight.multiply(entityList.size());
    }
}
