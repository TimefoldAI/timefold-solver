package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class KOptListMove<Solution_> extends AbstractMove<Solution_> {

    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final KOptDescriptor<?> descriptor;
    private final List<FlipSublistAction> equivalent2Opts;
    private final KOptAffectedElements affectedElementsInfo;
    private final int postShiftAmount;
    private final int[] newEndIndices;
    private final Object[] originalEntities;

    KOptListMove(ListVariableDescriptor<Solution_> listVariableDescriptor,
            KOptDescriptor<?> descriptor,
            MultipleDelegateList<?> combinedList,
            List<FlipSublistAction> equivalent2Opts,
            int postShiftAmount,
            int[] newEndIndices) {
        this.listVariableDescriptor = listVariableDescriptor;
        this.descriptor = descriptor;
        this.equivalent2Opts = equivalent2Opts;
        this.postShiftAmount = postShiftAmount;
        this.newEndIndices = newEndIndices;
        if (equivalent2Opts.isEmpty()) {
            affectedElementsInfo = KOptAffectedElements.forMiddleRange(0, 0);
        } else if (postShiftAmount != 0) {
            affectedElementsInfo = KOptAffectedElements.forMiddleRange(0, combinedList.size());
        } else {
            var currentAffectedElements = equivalent2Opts.get(0).getAffectedElements();
            for (var i = 1; i < equivalent2Opts.size(); i++) {
                currentAffectedElements = currentAffectedElements.merge(equivalent2Opts.get(i).getAffectedElements());
            }
            affectedElementsInfo = currentAffectedElements;
        }

        originalEntities = combinedList.delegateEntities;
    }

    private KOptListMove(ListVariableDescriptor<Solution_> listVariableDescriptor,
            KOptDescriptor<?> descriptor,
            List<FlipSublistAction> equivalent2Opts,
            int postShiftAmount,
            int[] newEndIndices,
            Object[] originalEntities) {
        this.listVariableDescriptor = listVariableDescriptor;
        this.descriptor = descriptor;
        this.equivalent2Opts = equivalent2Opts;
        this.postShiftAmount = postShiftAmount;
        this.newEndIndices = newEndIndices;
        if (equivalent2Opts.isEmpty()) {
            affectedElementsInfo = KOptAffectedElements.forMiddleRange(0, 0);
        } else if (postShiftAmount != 0) {
            affectedElementsInfo = KOptAffectedElements.forMiddleRange(0,
                    computeCombinedList(listVariableDescriptor, originalEntities).size());
        } else {
            var currentAffectedElements = equivalent2Opts.get(0).getAffectedElements();
            for (var i = 1; i < equivalent2Opts.size(); i++) {
                currentAffectedElements = currentAffectedElements.merge(equivalent2Opts.get(i).getAffectedElements());
            }
            affectedElementsInfo = currentAffectedElements;
        }

        this.originalEntities = originalEntities;
    }

    KOptDescriptor<?> getDescriptor() {
        return descriptor;
    }

    @Override
    protected Move<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector) {
        if (equivalent2Opts.isEmpty()) {
            return this;
        } else {
            List<FlipSublistAction> inverse2Opts = new ArrayList<>(equivalent2Opts.size());
            for (var i = equivalent2Opts.size() - 1; i >= 0; i--) {
                inverse2Opts.add(equivalent2Opts.get(i).createUndoMove());
            }

            var combinedList = computeCombinedList(listVariableDescriptor, originalEntities);
            var originalEndIndices = new int[newEndIndices.length];
            for (var i = 0; i < originalEndIndices.length - 1; i++) {
                originalEndIndices[i] = combinedList.offsets[i + 1] - 1;
            }
            originalEndIndices[originalEndIndices.length - 1] = combinedList.size() - 1;

            return new UndoKOptListMove<>(this, listVariableDescriptor, inverse2Opts, -postShiftAmount,
                    originalEndIndices, originalEntities);
        }
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;

        var combinedList = computeCombinedList(listVariableDescriptor, originalEntities);
        combinedList.actOnAffectedElements(listVariableDescriptor,
                originalEntities,
                (entity, start, end) -> innerScoreDirector.beforeListVariableChanged(listVariableDescriptor, entity,
                        start,
                        end));

        // subLists will get corrupted by ConcurrentModifications, so do the operations
        // on a clone
        var combinedListCopy = combinedList.copy();
        for (var move : equivalent2Opts) {
            move.doMoveOnGenuineVariables(combinedListCopy);
        }

        combinedListCopy.moveElementsOfDelegates(newEndIndices);

        Collections.rotate(combinedListCopy, postShiftAmount);
        combinedList.applyChangesFromCopy(combinedListCopy);

        combinedList.actOnAffectedElements(listVariableDescriptor,
                originalEntities,
                (entity, start, end) -> innerScoreDirector.afterListVariableChanged(listVariableDescriptor, entity,
                        start,
                        end));
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return !equivalent2Opts.isEmpty();
    }

    @Override
    public KOptListMove<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        var rebasedEquivalent2Opts = new ArrayList<FlipSublistAction>(equivalent2Opts.size());
        var innerScoreDirector = (InnerScoreDirector<?, ?>) destinationScoreDirector;
        var newEntities = new Object[originalEntities.length];

        for (var i = 0; i < newEntities.length; i++) {
            newEntities[i] = innerScoreDirector.lookUpWorkingObject(originalEntities[i]);
        }
        for (var twoOpt : equivalent2Opts) {
            rebasedEquivalent2Opts.add(twoOpt.rebase());
        }

        return new KOptListMove<>(listVariableDescriptor, descriptor, rebasedEquivalent2Opts, postShiftAmount, newEndIndices,
                newEntities);
    }

    @Override
    public String getSimpleMoveTypeDescription() {
        return descriptor.k() + "-opt(" + listVariableDescriptor.getSimpleEntityAndVariableName() + ")";
    }

    @Override
    public Collection<?> getPlanningEntities() {
        return List.of(originalEntities);
    }

    @Override
    public Collection<?> getPlanningValues() {
        var out = new ArrayList<>();

        var combinedList = computeCombinedList(listVariableDescriptor, originalEntities);
        if (affectedElementsInfo.wrappedStartIndex() != -1) {
            out.addAll(combinedList.subList(affectedElementsInfo.wrappedStartIndex(), combinedList.size()));
            out.addAll(combinedList.subList(0, affectedElementsInfo.wrappedEndIndex()));
        }
        for (var affectedRange : affectedElementsInfo.affectedMiddleRangeList()) {
            out.addAll(combinedList.subList(affectedRange.startInclusive(), affectedRange.endExclusive()));
        }

        return out;
    }

    public String toString() {
        return descriptor.toString();
    }

    static <Solution_> MultipleDelegateList<?> computeCombinedList(ListVariableDescriptor<Solution_> listVariableDescriptor,
            Object[] entities) {
        @SuppressWarnings("unchecked")
        List<Object>[] delegates = new List[entities.length];

        for (var i = 0; i < entities.length; i++) {
            delegates[i] = listVariableDescriptor.getUnpinnedSubList(entities[i]);
        }
        return new MultipleDelegateList<>(entities, delegates);
    }

}
