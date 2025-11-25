package ai.timefold.solver.core.impl.neighborhood.maybeapi.move;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningSolutionMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveDefinition;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.EnumeratingJoiners;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ListSwapMoveDefinition<Solution_, Entity_, Value_>
        implements MoveDefinition<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Function<Entity_, Comparable> planningIdGetter;

    public ListSwapMoveDefinition(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.planningIdGetter = getPlanningIdGetter(variableMetaModel.entity().type());
    }

    private <A> Function<A, Comparable> getPlanningIdGetter(Class<A> sourceClass) {
        SolutionDescriptor<Solution_> solutionDescriptor =
                ((DefaultPlanningSolutionMetaModel<Solution_>) variableMetaModel.entity().solution()).solutionDescriptor();
        MemberAccessor planningIdMemberAccessor = solutionDescriptor.getPlanningIdAccessor(sourceClass);
        if (planningIdMemberAccessor == null) {
            throw new IllegalArgumentException(
                    "The fromClass (%s) has no member with a @%s annotation, so the pairs cannot be made unique ([A,B] vs [B,A])."
                            .formatted(sourceClass, PlanningId.class.getSimpleName()));
        }
        return planningIdMemberAccessor.getGetterFunction();
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var assignedValueStream = moveStreamFactory.forEach(variableMetaModel.type(), false)
                .filter((solutionView, value) -> solutionView.getPositionOf(variableMetaModel, value) instanceof PositionInList)
                .map((solutionView, value) -> new FullElementPosition<>(value,
                        solutionView.getPositionOf(variableMetaModel, value).ensureAssigned(), planningIdGetter));
        // TODO this requires everything that is ever swapped to implement @PlanningID; likely not acceptable
        return moveStreamFactory.pick(assignedValueStream)
                .pick(assignedValueStream,
                        EnumeratingJoiners.lessThan(a -> a),
                        EnumeratingJoiners.filtering(this::isValidSwap))
                .asMove((solutionView, leftPosition, rightPosition) -> Moves.swap(leftPosition.elementPosition,
                        rightPosition.elementPosition, variableMetaModel));
    }

    private boolean isValidSwap(SolutionView<Solution_> solutionView,
            FullElementPosition<Entity_, Value_> leftPosition,
            FullElementPosition<Entity_, Value_> rightPosition) {
        if (Objects.equals(leftPosition, rightPosition)) {
            return false;
        }
        return solutionView.isValueInRange(variableMetaModel, rightPosition.entity(), leftPosition.value())
                && solutionView.isValueInRange(variableMetaModel, leftPosition.entity(), rightPosition.value());
    }

    @NullMarked
    private record FullElementPosition<Entity_, Value_>(Value_ value, PositionInList elementPosition,
            Function<Entity_, Comparable> planningIdGetter) implements Comparable<FullElementPosition<Entity_, Value_>> {

        public static <Solution_, Entity_, Value_> FullElementPosition<Entity_, Value_> of(
                PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
                SolutionView<Solution_> solutionView, Value_ value,
                Function<Entity_, Comparable> planningIdGetter) {
            var assignedElement = solutionView.getPositionOf(variableMetaModel, value).ensureAssigned();
            return new FullElementPosition<>(value, assignedElement, planningIdGetter);
        }

        public Entity_ entity() {
            return elementPosition.entity();
        }

        public int index() {
            return elementPosition.index();
        }

        @Override
        public int compareTo(FullElementPosition<Entity_, Value_> o) {
            var entityComparison = planningIdGetter.apply(this.entity()).compareTo(planningIdGetter.apply(o.entity()));
            if (entityComparison != 0) {
                return entityComparison;
            }
            return Integer.compare(this.index(), o.index());
        }

        @Override
        public String toString() {
            return value + "@" + elementPosition;
        }
    }

}
