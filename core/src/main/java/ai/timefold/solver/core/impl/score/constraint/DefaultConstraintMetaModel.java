package ai.timefold.solver.core.impl.score.constraint;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.score.constraint.ConstraintMetaModel;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;

public record DefaultConstraintMetaModel(
        Map<ConstraintRef, Constraint> constraintDescriptorMap) implements ConstraintMetaModel {

    public static ConstraintMetaModel of(List<? extends Constraint> constraints) {
        var map = new LinkedHashMap<ConstraintRef, Constraint>(); // Preserve iteration order.
        for (var constraint : constraints) {
            map.put(constraint.getConstraintRef(), constraint);
        }
        return new DefaultConstraintMetaModel(map);
    }

    public DefaultConstraintMetaModel(Map<ConstraintRef, Constraint> constraintDescriptorMap) {
        this.constraintDescriptorMap = Collections.unmodifiableMap(constraintDescriptorMap);
    }

    @Override
    public Constraint getConstraint(ConstraintRef constraintRef) {
        return constraintDescriptorMap.get(constraintRef);
    }

    @Override
    public Collection<Constraint> getConstraintsPerGroup(String constraintGroup) {
        return getConstraints().stream()
                .filter(constraint -> constraint.getConstraintGroup().equals(constraintGroup))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Constraint> getConstraints() {
        return constraintDescriptorMap.values();
    }

}
