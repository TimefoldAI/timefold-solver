package ai.timefold.solver.test.api.score.stream;

import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;

public interface SingleConstraintVerification<Solution_> {

    /**
     * If the code under test uses {@link PlanningListVariable},
     * the facts provided to this method need to meet either one of the following criteria:
     *
     * <ul>
     * <li>It needs to include both the planning entity and the planning value(s),
     * and the planning entity needs to have its list variable correctly filled.</li>
     * <li>The planning values need to have their {@link InverseRelationShadowVariable} set to the entity
     * with the relevant list variable.</li>
     * </ul>
     * 
     * In case none of these are met,
     * the values will be reported as unassigned
     * and therefore will be filtered out by the {@link ConstraintFactory#forEach(Class)} check.
     * {@link ConstraintFactory#forEachIncludingUnassigned(Class)} will include them regardless.
     *
     * @param facts never null, at least one
     * @return never null
     */
    SingleConstraintAssertion given(Object... facts);

    /**
     * @param solution never null
     * @return never null
     */
    SingleConstraintAssertion givenSolution(Solution_ solution);

}
