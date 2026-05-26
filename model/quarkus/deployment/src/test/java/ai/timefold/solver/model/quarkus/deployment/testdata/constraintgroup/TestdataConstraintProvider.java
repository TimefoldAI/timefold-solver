package ai.timefold.solver.model.quarkus.deployment.testdata.constraintgroup;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.model.definition.api.description.ConstraintGroupInfo;
import ai.timefold.solver.model.definition.api.description.ConstraintInfo;

import org.jspecify.annotations.NonNull;

public class TestdataConstraintProvider implements ConstraintProvider {

    public static final ConstraintGroupInfo TEST_GROUP =
            new ConstraintGroupInfo("group1", "group 1", "group1 description", "IconSettings", new String[] { "tag1" });

    public static final SimpleScore NO_CONFLICTS_CONSTRAINT_WEIGHT = SimpleScore.ONE;
    public static final String NO_CONFLICTS_CONSTRAINT_ID = "noConflicts";
    public static final String NO_CONFLICTS_CONSTRAINT_NAME = "No conflicts";
    public static final String NO_CONFLICTS_CONSTRAINT_DESCRIPTION = "Don't assign 2 entities the same value.";

    public static final String ALL_PENALIZING_CONSTRAINT_ID = "PenalizeAllEntities";

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataEntity.class)
                        .join(TestdataEntity.class, Joiners.equal(TestdataEntity::getValue))
                        .filter((a, b) -> a != b)
                        .penalize(NO_CONFLICTS_CONSTRAINT_WEIGHT)
                        .asConstraint(new ConstraintInfo(NO_CONFLICTS_CONSTRAINT_ID, NO_CONFLICTS_CONSTRAINT_NAME,
                                NO_CONFLICTS_CONSTRAINT_DESCRIPTION, TEST_GROUP)),

                factory.forEach(TestdataEntity.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(new ConstraintInfo(ALL_PENALIZING_CONSTRAINT_ID, null, null, null))

        };
    }
}
