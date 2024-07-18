package ai.timefold.solver.core.impl.domain.solution;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.Score;

public record DefaultConstraintWeightOverrides<Score_ extends Score<Score_>>(Map<String, Score_> constraintWeightMap)
        implements
            ConstraintWeightOverrides<Score_> {

    public DefaultConstraintWeightOverrides(Map<String, Score_> constraintWeightMap) {
        this.constraintWeightMap = new TreeMap<>(constraintWeightMap); // Keep consistent order for reproducibility.
    }

    @Override
    public Score_ getConstraintWeight(String constraintName) {
        return constraintWeightMap.get(constraintName);
    }

    @Override
    public Set<String> getKnownConstraintNames() {
        return Collections.unmodifiableSet(constraintWeightMap.keySet());
    }

}
