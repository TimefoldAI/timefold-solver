package ai.timefold.solver.core.api.score.constraint;

import java.util.LinkedHashMap;
import java.util.List;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;
import ai.timefold.solver.core.impl.score.stream.common.DefaultConstraintProviderMetaModel;

public interface ConstraintProviderMetaModel<Score_ extends Score<Score_>> {

    static <Score_ extends Score<Score_>> ConstraintProviderMetaModel<Score_>
            of(List<? extends AbstractConstraint<?, ?, ?>> constraints) {
        var map = new LinkedHashMap<ConstraintRef, AbstractConstraint<?, ?, ?>>(); // Preserve iteration order.
        for (var constraint : constraints) {
            map.put(constraint.getConstraintRef(), constraint);
        }
        return new DefaultConstraintProviderMetaModel<>(map);
    }

    ConstraintMetaModel<Score_> getConstraintMetaModel(ConstraintRef constraintRef);

}
