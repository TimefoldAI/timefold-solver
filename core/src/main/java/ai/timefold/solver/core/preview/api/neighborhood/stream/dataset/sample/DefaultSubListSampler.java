package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample;

import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.util.TriangleElementFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class DefaultSubListSampler<Solution_, Entity_, Value_> implements SubListSampler<Solution_, Entity_, Value_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final int minimumSubListSize;
    private final TriangleElementFactory triangleElementFactory;

    DefaultSubListSampler(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            int minimumSubListSize, int maximumSubListSize, RandomGenerator random) {
        this.variableMetaModel = variableMetaModel;
        this.minimumSubListSize = minimumSubListSize;
        this.triangleElementFactory = new TriangleElementFactory(minimumSubListSize, maximumSubListSize, random);
    }

    @Override
    @Nullable
    public Range<Entity_> byValue(SolutionView<Solution_> solutionView, Value_ seedValue) {
        var position = solutionView.getPositionOf(variableMetaModel, seedValue);
        if (!(position instanceof PositionInList assigned)) {
            throw new IllegalArgumentException("The seedValue (%s) is not assigned.".formatted(seedValue));
        }
        return byEntity(solutionView, assigned.entity());
    }

    @Override
    @Nullable
    public Range<Entity_> byEntity(SolutionView<Solution_> solutionView, Entity_ entity) {
        var firstUnpinned = solutionView.getFirstUnpinnedIndex(variableMetaModel, entity);
        var listSize = solutionView.countValues(variableMetaModel, entity) - firstUnpinned;
        if (listSize < minimumSubListSize) {
            return null;
        }
        var element = triangleElementFactory.nextElement(listSize);
        var length = listSize - element.level() + 1;
        var fromIndex = element.indexOnLevel() - 1 + firstUnpinned;
        return new Range<>(entity, fromIndex, fromIndex + length);
    }

}
