package ai.timefold.solver.quarkus.jackson.it.solver;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.quarkus.jackson.it.domain.ITestdataPlanningEntity;

import org.jspecify.annotations.NonNull;

public class ITestdataPlanningConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(ITestdataPlanningEntity.class)
                        .join(ITestdataPlanningEntity.class, Joiners.equal(ITestdataPlanningEntity::getValue))
                        .filter((a, b) -> a != b)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("Don't assign 2 entities the same value.")
        };
    }

}
