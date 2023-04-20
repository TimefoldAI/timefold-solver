package ai.timefold.solver.constraint.streams.drools;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.constraint.streams.common.AbstractConstraint;
import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.constraint.streams.common.inliner.WeightedScoreImpacter;
import ai.timefold.solver.constraint.streams.drools.common.RuleBuilder;
import ai.timefold.solver.core.api.score.Score;

import org.drools.model.Global;
import org.drools.model.Rule;

public final class DroolsConstraint<Solution_>
        extends AbstractConstraint<Solution_, DroolsConstraint<Solution_>, DroolsConstraintFactory<Solution_>> {

    private final RuleBuilder<Solution_> ruleBuilder;

    public DroolsConstraint(DroolsConstraintFactory<Solution_> constraintFactory, String constraintPackage,
            String constraintName, Function<Solution_, Score<?>> constraintWeightExtractor,
            ScoreImpactType scoreImpactType, boolean isConstraintWeightConfigurable,
            RuleBuilder<Solution_> ruleBuilder, Object justificationMapping, Object indictedObjectsMapping) {
        super(constraintFactory, constraintPackage, constraintName, constraintWeightExtractor, scoreImpactType,
                isConstraintWeightConfigurable, justificationMapping, indictedObjectsMapping);
        this.ruleBuilder = Objects.requireNonNull(ruleBuilder);
    }

    public Rule buildRule(Global<WeightedScoreImpacter> scoreImpacterGlobal) {
        return ruleBuilder.apply(this, scoreImpacterGlobal);
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public String toString() {
        return "DroolsConstraint(" + getConstraintId() + ")";
    }
}
