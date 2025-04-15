package ai.timefold.solver.core.impl.testdata.domain.immutable.record;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public record TestdataRecordEntity(@PlanningVariable String code) {
}
