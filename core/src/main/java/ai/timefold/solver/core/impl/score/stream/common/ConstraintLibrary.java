package ai.timefold.solver.core.impl.score.stream.common;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

public interface ConstraintLibrary<Score_ extends Score<Score_>> {

    static <Score_ extends Score<Score_>> ConstraintLibrary<Score_>
            of(List<? extends AbstractConstraint<?, ?, ?>> constraints) {
        var map = new LinkedHashMap<ConstraintRef, AbstractConstraint<?, ?, ?>>(); // Preserve iteration order.
        for (var constraint : constraints) {
            map.put(constraint.getConstraintRef(), constraint);
        }
        return new DefaultConstraintLibrary<>(map);
    }

    ConstraintDescriptor<Score_> getConstraintDescriptor(ConstraintRef constraintRef);

    Collection<AbstractConstraint<?, ?, ?>> getConstraints();

    record ConstraintDescriptor<Score_ extends Score<Score_>>(String constraintName, String constraintDescription,
            Score_ weight) {

    }

}
