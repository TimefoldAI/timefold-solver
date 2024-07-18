package ai.timefold.solver.core.impl.score.stream.common;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

record DefaultConstraintLibrary<Score_ extends Score<Score_>>(
        Map<ConstraintRef, AbstractConstraint<?, ?, ?>> constraintDescriptorMap)
        implements
            ConstraintLibrary<Score_> {

    public DefaultConstraintLibrary(Map<ConstraintRef, AbstractConstraint<?, ?, ?>> constraintDescriptorMap) {
        this.constraintDescriptorMap = Collections.unmodifiableMap(constraintDescriptorMap);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConstraintDescriptor<Score_> getConstraintDescriptor(ConstraintRef constraintRef) {
        var constraint = constraintDescriptorMap.get(constraintRef);
        return new ConstraintDescriptor<>(constraint.getConstraintRef().constraintName(),
                constraint.getDescription(), (Score_) constraint.getDefaultConstraintWeight());
    }

    @Override
    public Collection<AbstractConstraint<?, ?, ?>> getConstraints() {
        return constraintDescriptorMap.values();
    }

}
