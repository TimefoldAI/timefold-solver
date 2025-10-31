package ai.timefold.solver.core.impl.neighborhood.maybeapi.move;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningSolutionMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveDefinition;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.EnumeratingJoiners;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.joiner.DefaultBiEnumeratingJoiner;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class SwapMoveDefinition<Solution_, Entity_>
        implements MoveDefinition<Solution_> {

    private final PlanningEntityMetaModel<Solution_, Entity_> entityMetaModel;
    private final List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList;

    @SuppressWarnings("unchecked")
    public SwapMoveDefinition(PlanningEntityMetaModel<Solution_, Entity_> entityMetaModel) {
        this.entityMetaModel = Objects.requireNonNull(entityMetaModel);
        this.variableMetaModelList = entityMetaModel.variables().stream()
                .flatMap(v -> {
                    if (v instanceof PlanningVariableMetaModel<Solution_, Entity_, ?> planningVariableMetaModel) {
                        return Stream.of((PlanningVariableMetaModel<Solution_, Entity_, Object>) planningVariableMetaModel);
                    }
                    return Stream.empty();
                })
                .toList();
        if (variableMetaModelList.isEmpty()) {
            throw new IllegalArgumentException("The entityClass (%s) has no basic planning variables."
                    .formatted(entityMetaModel.type().getCanonicalName()));
        }
    }

    public SwapMoveDefinition(List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList) {
        this.variableMetaModelList = Objects.requireNonNull(variableMetaModelList);
        var entityMetaModels = variableMetaModelList.stream()
                .map(VariableMetaModel::entity)
                .distinct()
                .toList();
        this.entityMetaModel = switch (entityMetaModels.size()) {
            case 0 -> throw new IllegalArgumentException("The variableMetaModelList (%s) is empty."
                    .formatted(variableMetaModelList));
            case 1 -> entityMetaModels.get(0);
            default -> throw new IllegalArgumentException(
                    "The variableMetaModelList (%s) contains variables from multiple entity classes."
                            .formatted(variableMetaModelList));
        };
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var entityType = entityMetaModel.type();
        var entityStream = moveStreamFactory.forEach(entityType, false);
        // TODO this requires everything that is ever swapped to implement @PlanningID; likely not acceptable
        return moveStreamFactory.pick(entityStream)
                .pick(entityStream,
                        buildLessThanId(entityType),
                        EnumeratingJoiners.filtering(this::isValidSwap))
                .asMove((solutionView, leftEntity, rightEntity) -> Moves.swap(leftEntity, rightEntity,
                        variableMetaModelList.toArray(new PlanningVariableMetaModel[0])));
    }

    private <A> DefaultBiEnumeratingJoiner<A, A> buildLessThanId(Class<A> sourceClass) {
        SolutionDescriptor<Solution_> solutionDescriptor =
                ((DefaultPlanningSolutionMetaModel<Solution_>) entityMetaModel.solution()).solutionDescriptor();
        MemberAccessor planningIdMemberAccessor = solutionDescriptor.getPlanningIdAccessor(sourceClass);
        if (planningIdMemberAccessor == null) {
            throw new IllegalArgumentException(
                    "The fromClass (%s) has no member with a @%s annotation, so the pairs cannot be made unique ([A,B] vs [B,A])."
                            .formatted(sourceClass, PlanningId.class.getSimpleName()));
        }
        Function<A, Comparable> planningIdGetter = planningIdMemberAccessor.getGetterFunction();
        return (DefaultBiEnumeratingJoiner<A, A>) EnumeratingJoiners.lessThan(planningIdGetter);
    }

    private boolean isValidSwap(SolutionView<Solution_> solutionView, Entity_ leftEntity, Entity_ rightEntity) {
        if (leftEntity == rightEntity) {
            return false;
        }
        var change = false;
        for (var variableMetaModel : variableMetaModelList) {
            var defaultVariableMetaModel = (DefaultPlanningVariableMetaModel<Solution_, Entity_, Object>) variableMetaModel;
            var variableDescriptor = defaultVariableMetaModel.variableDescriptor();
            var oldLeftValue = variableDescriptor.getValue(leftEntity);
            var oldRightValue = variableDescriptor.getValue(rightEntity);
            if (Objects.equals(oldLeftValue, oldRightValue)) {
                continue;
            }
            if (solutionView.isValueInRange(variableMetaModel, leftEntity, oldRightValue)
                    && solutionView.isValueInRange(variableMetaModel, rightEntity, oldLeftValue)) {
                change = true;
            } else {
                // One of the swaps falls out of range, skip this pair altogether.
                return false;
            }
        }
        return change;
    }

}
