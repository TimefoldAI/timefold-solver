package ai.timefold.solver.core.impl.domain.solution;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeights;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

public class DefaultConstraintWeights<Score_ extends Score<Score_>>
        implements ConstraintWeights<Score_> {

    public static ConstraintRef determineConstraintRef(String constraintName, ConstraintWeights<?> weights) {
        var packageName = weights.getClass().getPackageName();
        return ConstraintRef.of(packageName, constraintName);
    }

    private final Map<ConstraintRef, Score_> constraintWeightMap = new LinkedHashMap<>();

    public void setConstraintWeight(String constraintName, Score_ constraintWeight) {
        setConstraintWeight(determineConstraintRef(constraintName, this), constraintWeight);
    }

    public void setConstraintWeight(ConstraintRef constraintRef, Score_ constraintWeight) {
        var old = constraintWeightMap.put(constraintRef, constraintWeight);
        if (old != null) {
            throw new IllegalStateException("The weight (%s) for constraint (%s) was already set to (%s)."
                    .formatted(constraintWeight, constraintRef, old));
        }
    }

    @Override
    public Score_ getConstraintWeight(ConstraintRef constraintRef) {
        return constraintWeightMap.get(constraintRef);
    }

    @Override
    public Set<ConstraintRef> getKnownConstraints() {
        return Collections.unmodifiableSet(constraintWeightMap.keySet());
    }

    @Override
    public boolean isKnown(ConstraintRef constraintRef) {
        return constraintWeightMap.containsKey(constraintRef);
    }
}
