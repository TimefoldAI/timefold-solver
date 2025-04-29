package ai.timefold.solver.core.testdomain.shadow.full;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class TestdataShadowedFullConsistencyListener
        implements VariableListener<TestdataShadowedFullSolution, TestdataShadowedFullValue> {

    @Override
    public void beforeVariableChanged(@NonNull ScoreDirector<TestdataShadowedFullSolution> scoreDirector,
            @NonNull TestdataShadowedFullValue testdataShadowedFullValue) {
        // Ignore
    }

    @Override
    public void afterVariableChanged(@NonNull ScoreDirector<TestdataShadowedFullSolution> scoreDirector,
            @NonNull TestdataShadowedFullValue testdataShadowedFullValue) {
        verifyConsistency(scoreDirector);
    }

    @Override
    public void beforeEntityAdded(@NonNull ScoreDirector<TestdataShadowedFullSolution> scoreDirector,
            @NonNull TestdataShadowedFullValue testdataShadowedFullValue) {
        // Ignore
    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector<TestdataShadowedFullSolution> scoreDirector,
            @NonNull TestdataShadowedFullValue testdataShadowedFullValue) {
        verifyConsistency(scoreDirector);
    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector<TestdataShadowedFullSolution> scoreDirector,
            @NonNull TestdataShadowedFullValue testdataShadowedFullValue) {
        // Ignore
    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector<TestdataShadowedFullSolution> scoreDirector,
            @NonNull TestdataShadowedFullValue testdataShadowedFullValue) {
        verifyConsistency(scoreDirector);
    }

    public static void verifyConsistency(ScoreDirector<TestdataShadowedFullSolution> scoreDirector) {
        var workingSolution = scoreDirector.getWorkingSolution();
        Set<TestdataShadowedFullValue> visitedValues = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var entity : workingSolution.getEntityList()) {
            var currentSize = visitedValues.size();
            visitedValues.addAll(entity.valueList);
            if (visitedValues.size() != currentSize + entity.valueList.size()) {
                throw new IllegalStateException("Inconsistent solution: values appear in multiple lists");
            }
            for (int i = 0; i < entity.valueList.size(); i++) {
                var currentValue = entity.valueList.get(i);
                var previousValue = (i != 0) ? entity.valueList.get(i - 1) : null;
                var nextValue = (i < entity.valueList.size() - 1) ? entity.valueList.get(i + 1) : null;

                if (currentValue.entity != entity) {
                    throw new IllegalStateException(
                            """
                                    Inconsistent solution: incorrect inverse for (%s).
                                    Expected (%s) but was (%s).
                                    Found on entity (%s)'s valueList (%s).
                                    """
                                    .formatted(currentValue, entity, currentValue.entity, entity, entity.valueList));
                }
                if (currentValue.previousValue != previousValue) {
                    throw new IllegalStateException(
                            """
                                    Inconsistent solution: incorrect previous value for (%s).
                                    Expected (%s) but was (%s).
                                    Found on entity (%s)'s valueList (%s).
                                    """
                                    .formatted(currentValue, previousValue, currentValue.previousValue, entity,
                                            entity.valueList));
                }
                if (currentValue.nextValue != nextValue) {
                    throw new IllegalStateException(
                            """
                                    Inconsistent solution: incorrect next value for (%s).
                                    Expected (%s) but got (%s).
                                    Found on entity (%s)'s valueList (%s).
                                    """
                                    .formatted(currentValue, nextValue, currentValue.nextValue, entity, entity.valueList));
                }
                if (!Objects.equals(currentValue.index, i)) {
                    throw new IllegalStateException(
                            """
                                    Inconsistent solution: incorrect index for (%s).
                                    Expected (%s) but got (%s).
                                    Found on entity (%s)'s valueList (%s).
                                    """
                                    .formatted(currentValue, i, currentValue.index, entity, entity.valueList));
                }
            }
        }
    }
}
