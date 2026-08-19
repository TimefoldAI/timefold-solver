package ai.timefold.solver.core.preview.api.neighborhood.example;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;

import org.jspecify.annotations.NullMarked;

/**
 * Relocates a run of {@code length} consecutive values,
 * starting at {@code start} in {@code sourceEntity}'s list,
 * into {@code destinationEntity}'s list starting at {@code destinationIndex}.
 */
@NullMarked
record RelocateValueBlockMove(
        PlanningListVariableMetaModel<TestdataListEntityProvidingSolution, TestdataListEntityProvidingEntity, TestdataListEntityProvidingValue> variableMetaModel,
        TestdataListEntityProvidingEntity sourceEntity, int start, int length,
        TestdataListEntityProvidingEntity destinationEntity, int destinationIndex)
        implements
            Move<TestdataListEntityProvidingSolution> {

    @Override
    public void execute(MutableSolutionView<TestdataListEntityProvidingSolution> solutionView) {
        for (var offset = 0; offset < length; offset++) {
            solutionView.moveValueBetweenLists(variableMetaModel, sourceEntity, start, destinationEntity,
                    destinationIndex + offset);
        }
    }

    @Override
    public String describe() {
        return "RelocateValueBlock(%s[%d..%d) -> %s@%d)".formatted(sourceEntity, start, start + length,
                destinationEntity, destinationIndex);
    }

}
