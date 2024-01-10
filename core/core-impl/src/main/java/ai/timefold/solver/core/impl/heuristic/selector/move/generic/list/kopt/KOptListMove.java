package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.util.Pair;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class KOptListMove<Solution_> extends AbstractMove<Solution_> {

    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final KOptDescriptor<?> descriptor;
    private final List<FlipSublistAction> equivalent2Opts;
    private final KOptAffectedElements affectedElementsInfo;
    private final MultipleDelegateList<?> combinedList;
    private final int postShiftAmount;
    private final int[] newEndIndices;
    private final Object[] originalEntities;

    KOptListMove(ListVariableDescriptor<Solution_> listVariableDescriptor,
            SingletonInverseVariableSupply inverseVariableSupply,
            KOptDescriptor<?> descriptor,
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
            combinedList = new MultipleDelegateList<>();
        } else if (postShiftAmount != 0) {
            affectedElementsInfo = KOptAffectedElements.forMiddleRange(0, equivalent2Opts.get(0).combinedList().size());
            combinedList = equivalent2Opts.get(0).combinedList();
        } else {
            KOptAffectedElements currentAffectedElements = equivalent2Opts.get(0).getAffectedElements();
            combinedList = equivalent2Opts.get(0).combinedList();
            for (int i = 1; i < equivalent2Opts.size(); i++) {
                currentAffectedElements = currentAffectedElements.merge(equivalent2Opts.get(i).getAffectedElements());
            }
            affectedElementsInfo = currentAffectedElements;
        }

        originalEntities = new Object[combinedList.delegates.length];
        for (int i = 0; i < originalEntities.length; i++) {
            originalEntities[i] = inverseVariableSupply.getInverseSingleton(combinedList.delegates[i].get(0));
        }
    }

    KOptListMove(ListVariableDescriptor<Solution_> listVariableDescriptor,
            KOptDescriptor<?> descriptor,
            List<FlipSublistAction> equivalent2Opts,
            MultipleDelegateList<?> combinedList,
            int postShiftAmount,
            int[] newEndIndices,
            Object[] originalEntities) {
        this.listVariableDescriptor = listVariableDescriptor;
        this.descriptor = descriptor;
        this.equivalent2Opts = equivalent2Opts;
        this.postShiftAmount = postShiftAmount;
        this.newEndIndices = newEndIndices;
        this.combinedList = combinedList;
        if (equivalent2Opts.isEmpty()) {
            affectedElementsInfo = KOptAffectedElements.forMiddleRange(0, 0);
        } else if (postShiftAmount != 0) {
            affectedElementsInfo = KOptAffectedElements.forMiddleRange(0, equivalent2Opts.get(0).combinedList().size());
        } else {
            KOptAffectedElements currentAffectedElements = equivalent2Opts.get(0).getAffectedElements();
            for (int i = 1; i < equivalent2Opts.size(); i++) {
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
    protected AbstractMove<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector) {
        if (equivalent2Opts.isEmpty()) {
            return this;
        } else {
            List<FlipSublistAction> inverse2Opts = new ArrayList<>(equivalent2Opts.size());
            for (int i = equivalent2Opts.size() - 1; i >= 0; i--) {
                inverse2Opts.add(equivalent2Opts.get(i).createUndoMove());
            }

            int[] originalEndIndices = new int[newEndIndices.length];
            for (int i = 0; i < originalEndIndices.length - 1; i++) {
                originalEndIndices[i] = combinedList.offsets[i + 1] - 1;
            }
            originalEndIndices[originalEndIndices.length - 1] = combinedList.size() - 1;

            return new UndoKOptListMove<>(listVariableDescriptor, descriptor, inverse2Opts, -postShiftAmount,
                    originalEndIndices, originalEntities);
        }
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        InnerScoreDirector<Solution_, ?> innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;

        combinedList.actOnAffectedElements(originalEntities,
                (entity, start, end) -> innerScoreDirector.beforeListVariableChanged(listVariableDescriptor, entity,
                        start,
                        end));

        for (FlipSublistAction move : equivalent2Opts) {
            move.doMoveOnGenuineVariables();
        }

        combinedList.moveElementsOfDelegates(newEndIndices);

        Collections.rotate(combinedList, postShiftAmount);

        combinedList.actOnAffectedElements(originalEntities,
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
        List<FlipSublistAction> rebasedEquivalent2Opts = new ArrayList<>(equivalent2Opts.size());
        InnerScoreDirector<?, ?> innerScoreDirector = (InnerScoreDirector<?, ?>) destinationScoreDirector;
        Object[] newEntities = new Object[originalEntities.length];
        @SuppressWarnings("unchecked")
        List<Object>[] newDelegates = new List[originalEntities.length];

        for (int i = 0; i < newEntities.length; i++) {
            newEntities[i] = innerScoreDirector.lookUpWorkingObject(originalEntities[i]);
            newDelegates[i] = listVariableDescriptor.getListVariable(newEntities[i]);
        }
        MultipleDelegateList<Object> rebasedList = new MultipleDelegateList<>(newDelegates);
        for (FlipSublistAction twoOpt : equivalent2Opts) {
            rebasedEquivalent2Opts.add(twoOpt.rebase(rebasedList));
        }

        return new KOptListMove<>(listVariableDescriptor,
                descriptor, rebasedEquivalent2Opts, rebasedList, postShiftAmount, newEndIndices, newEntities);
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
        List<Object> out = new ArrayList<>();

        if (affectedElementsInfo.wrappedStartIndex() != -1) {
            out.addAll(combinedList.subList(affectedElementsInfo.wrappedStartIndex(), combinedList.size()));
            out.addAll(combinedList.subList(0, affectedElementsInfo.wrappedEndIndex()));
        }
        for (Pair<Integer, Integer> affectedInterval : affectedElementsInfo.affectedMiddleRangeList()) {
            out.addAll(combinedList.subList(affectedInterval.key(), affectedInterval.value()));
        }

        return out;
    }

    public String toString() {
        return descriptor.toString();
    }

    /**
     * A K-Opt move that does the list rotation before performing the flips instead of after, allowing
     * it to act as the undo move of a K-Opt move that does the rotation after the flips.
     *
     * @param <Solution_>
     */
    private static final class UndoKOptListMove<Solution_, Node_> extends AbstractMove<Solution_> {
        private final ListVariableDescriptor<Solution_> listVariableDescriptor;
        private final KOptDescriptor<Node_> descriptor;
        private final List<FlipSublistAction> equivalent2Opts;
        private final MultipleDelegateList<?> combinedList;
        private final int preShiftAmount;
        private final int[] newEndIndices;

        private final Object[] originalEntities;

        public UndoKOptListMove(ListVariableDescriptor<Solution_> listVariableDescriptor,
                KOptDescriptor<Node_> descriptor,
                List<FlipSublistAction> equivalent2Opts,
                int preShiftAmount,
                int[] newEndIndices,
                Object[] originalEntities) {
            this.listVariableDescriptor = listVariableDescriptor;
            this.descriptor = descriptor;
            this.equivalent2Opts = equivalent2Opts;
            this.preShiftAmount = preShiftAmount;
            this.combinedList = equivalent2Opts.get(0).combinedList();
            this.newEndIndices = newEndIndices;
            this.originalEntities = originalEntities;
        }

        @Override
        public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
            return true;
        }

        @Override
        protected AbstractMove<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
            InnerScoreDirector<Solution_, ?> innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;

            combinedList.actOnAffectedElements(originalEntities,
                    (entity, start, end) -> innerScoreDirector.beforeListVariableChanged(listVariableDescriptor, entity,
                            start,
                            end));

            Collections.rotate(combinedList, preShiftAmount);
            combinedList.moveElementsOfDelegates(newEndIndices);

            for (FlipSublistAction move : equivalent2Opts) {
                move.doMoveOnGenuineVariables();
            }
            combinedList.actOnAffectedElements(originalEntities,
                    (entity, start, end) -> innerScoreDirector.afterListVariableChanged(listVariableDescriptor, entity,
                            start,
                            end));
        }

        public String toString() {
            return "Undo" + descriptor.toString();
        }
    }

}
