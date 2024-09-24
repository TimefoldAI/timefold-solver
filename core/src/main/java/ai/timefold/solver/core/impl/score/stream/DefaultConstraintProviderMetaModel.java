package ai.timefold.solver.core.impl.score.stream;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintProviderMetaModel;

public record DefaultConstraintProviderMetaModel(
        Map<ConstraintRef, Constraint> constraintDescriptorMap) implements ConstraintProviderMetaModel {

    public static ConstraintProviderMetaModel of(List<? extends Constraint> constraints) {
        var map = new LinkedHashMap<ConstraintRef, Constraint>(); // Preserve iteration order.
        for (var constraint : constraints) {
            map.put(constraint.getConstraintRef(), constraint);
        }
        return new DefaultConstraintProviderMetaModel(map);
    }

    public DefaultConstraintProviderMetaModel(Map<ConstraintRef, Constraint> constraintDescriptorMap) {
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
