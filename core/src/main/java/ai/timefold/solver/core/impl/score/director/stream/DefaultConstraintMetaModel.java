package ai.timefold.solver.core.impl.score.director.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.impl.util.CollectionUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

record DefaultConstraintMetaModel(
        Map<ConstraintRef, Constraint> constraintPerRefMap,
        Map<String, List<Constraint>> constraintPerGroupMap) implements ConstraintMetaModel {

    public static ConstraintMetaModel of(List<? extends Constraint> constraints) {
        var constraintCount = constraints.size();
        // Preserve iteration order by using LinkedHashMap.
        var perRefMap = CollectionUtils.<ConstraintRef, Constraint> newLinkedHashMap(constraintCount);
        var perGroupMap = new TreeMap<String, List<Constraint>>();
        for (var constraint : constraints) {
            perRefMap.put(constraint.getConstraintRef(), constraint);
            // The list is used to preserve iteration order of the constraints.
            // Constraint groups are an optional feature, therefore most people won't use them,
            // therefore sizing the list assuming all constraints end up in the default group.
            perGroupMap.computeIfAbsent(constraint.getConstraintGroup(), k -> new ArrayList<>(constraintCount))
                    .add(constraint);
        }
        return new DefaultConstraintMetaModel(
                Collections.unmodifiableMap(perRefMap),
                Collections.unmodifiableMap(perGroupMap));
    }

    @Override
    public @Nullable Constraint getConstraint(@NonNull ConstraintRef constraintRef) {
        return constraintPerRefMap.get(constraintRef);
    }

    @Override
    public @NonNull Collection<Constraint> getConstraintsPerGroup(@NonNull String constraintGroup) {
        return constraintPerGroupMap.getOrDefault(constraintGroup, Collections.emptyList());
    }

    @Override
    public @NonNull Set<String> getConstraintGroups() {
        return constraintPerGroupMap.keySet();
    }

    @Override
    public @NonNull Collection<Constraint> getConstraints() {
        return constraintPerRefMap.values();
    }

}
