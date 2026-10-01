package ai.timefold.solver.service.definition.api;

import ai.timefold.solver.core.api.score.stream.ConstraintJustification;

public interface ModelConstraintJustification extends ConstraintJustification {

    default String getDescription() {
        return "";
    }
}
