package ai.timefold.solver.service.definition.spi.dto;

import ai.timefold.solver.core.api.score.stream.ConstraintJustification;

public interface ModelConstraintJustification extends ConstraintJustification {

    default String getDescription() {
        return "";
    }
}
