package ai.timefold.solver.core.impl.score.director.stream;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.api.score.stream.ConstraintRef;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

record DefaultConstraintMetaModel(Map<ConstraintRef, Constraint> constraintPerRefMap)
        implements
            ConstraintMetaModel {

    public static ConstraintMetaModel of(List<? extends Constraint> constraints) {
        // Preserve iteration order by using LinkedHashMap.
        var perRefMap = constraints.stream()
                .collect(Collectors.toMap(Constraint::getConstraintRef, v -> v, (a, b) -> a, LinkedHashMap::new));
        return new DefaultConstraintMetaModel(Collections.unmodifiableMap(perRefMap));
    }

    @Override
    public @Nullable Constraint getConstraint(@NonNull ConstraintRef constraintRef) {
        return constraintPerRefMap.get(constraintRef);
    }

    @Override
    public @NonNull Collection<Constraint> getConstraints() {
        return constraintPerRefMap.values();
    }

}
