package ai.timefold.solver.quarkus.bean;

import java.util.Objects;

import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;

public class BeanUtil {

    public static ConstraintMetaModel buildConstraintMetaModel(SolverFactory<?> solverFactory) {
        if (Objects.requireNonNull(solverFactory) instanceof DefaultSolverFactory<?> defaultSolverFactory) {
            var scoreDirectorFactory = defaultSolverFactory.getScoreDirectorFactory();
            if (scoreDirectorFactory instanceof AbstractConstraintStreamScoreDirectorFactory<?, ?, ?> castScoreDirectorFactory) {
                return castScoreDirectorFactory.getConstraintMetaModel();
            } else {
                throw new IllegalStateException(
                        "Cannot provide %s because the score director does not use the Constraint Streams API."
                                .formatted(ConstraintMetaModel.class.getSimpleName()));
            }
        } else {
            throw new IllegalStateException(
                    "%s is not supported by the solver factory (%s)."
                            .formatted(ConstraintMetaModel.class.getSimpleName(), solverFactory.getClass().getName()));
        }
    }

    private BeanUtil() {
        throw new IllegalStateException("Utility class");
    }
}
