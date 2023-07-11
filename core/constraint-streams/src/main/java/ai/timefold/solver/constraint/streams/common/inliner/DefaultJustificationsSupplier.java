package ai.timefold.solver.constraint.streams.common.inliner;

import java.util.Collection;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;

record DefaultJustificationsSupplier(Function<Score<?>, ConstraintJustification> constraintJustificationSupplier,
        Collection<Object> indictedObjectCollection)
        implements
            JustificationsSupplier {

    @Override
    public ConstraintJustification createConstraintJustification(Score<?> impact) {
        return constraintJustificationSupplier.apply(impact);
    }

}
