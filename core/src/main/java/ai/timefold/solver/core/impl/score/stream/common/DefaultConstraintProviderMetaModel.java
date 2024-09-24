package ai.timefold.solver.core.impl.score.stream.common;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMetaModel;
import ai.timefold.solver.core.api.score.constraint.ConstraintProviderMetaModel;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

public record DefaultConstraintProviderMetaModel<Score_ extends Score<Score_>>(
        Map<ConstraintRef, AbstractConstraint<?, ?, ?>> constraintDescriptorMap)
        implements
            ConstraintProviderMetaModel<Score_> {

    public DefaultConstraintProviderMetaModel(Map<ConstraintRef, AbstractConstraint<?, ?, ?>> constraintDescriptorMap) {
        this.constraintDescriptorMap = Collections.unmodifiableMap(constraintDescriptorMap);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConstraintMetaModel<Score_> getConstraintMetaModel(ConstraintRef constraintRef) {
        var constraint = constraintDescriptorMap.get(constraintRef);
        return new ConstraintMetaModel<>(constraint.getConstraintRef().constraintName(),
                constraint.getDescription(), (Score_) constraint.getDefaultConstraintWeight());
    }

    public Collection<AbstractConstraint<?, ?, ?>> getConstraints() {
        return constraintDescriptorMap.values();
    }

}
