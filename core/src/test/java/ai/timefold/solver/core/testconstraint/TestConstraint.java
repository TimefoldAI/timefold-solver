package ai.timefold.solver.core.testconstraint;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;

public final class TestConstraint<Solution_, Score_ extends Score<Score_>>
        extends AbstractConstraint<Solution_, TestConstraint<Solution_, Score_>, TestConstraintFactory<Solution_, Score_>> {

    public TestConstraint(TestConstraintFactory<Solution_, Score_> constraintFactory, String constraintName,
            Score_ constraintWeight) {
        this(constraintFactory, constraintName, Constraint.DEFAULT_CONSTRAINT_GROUP, constraintWeight);
    }

    public TestConstraint(TestConstraintFactory<Solution_, Score_> constraintFactory, String constraintName,
            String constraintGroup, Score_ constraintWeight) {
        super(constraintFactory, ConstraintRef.of(constraintFactory.getDefaultConstraintPackage(), constraintName), "",
                constraintGroup, constraintWeight, ScoreImpactType.REWARD, null, null);
    }

}
