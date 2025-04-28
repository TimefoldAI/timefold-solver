package ai.timefold.solver.core.testconstraint;

import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class DummyConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {};
    }

}
