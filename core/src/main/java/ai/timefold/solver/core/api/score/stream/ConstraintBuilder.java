package ai.timefold.solver.core.api.score.stream;

import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.impl.score.stream.common.DefaultConstraintMetadata;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ConstraintBuilder {

    /**
     * Builds a {@link Constraint} from the constraint stream.
     * Shorthand for {@link #asConstraint(ConstraintMetadata)}.
     *
     * @param id shows up in {@link ScoreAnalysis}
     */
    default Constraint asConstraint(String id) {
        return asConstraint(new DefaultConstraintMetadata(id));
    }

    /**
     * Builds a {@link Constraint} from the constraint stream.
     * {@link ConstraintMetadata#id()} is called exactly once at this point;
     * the returned value is validated and snapshotted as the constraint's permanent identity.
     * Subsequent changes to the description's {@link ConstraintMetadata#id()} return value are ignored.
     *
     * @param metadata identifies and describes the constraint;
     *        {@link ConstraintMetadata#id()} shows up in {@link ScoreAnalysis}
     */
    Constraint asConstraint(ConstraintMetadata metadata);

}
