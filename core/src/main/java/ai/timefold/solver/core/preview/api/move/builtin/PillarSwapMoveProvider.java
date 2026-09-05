package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.move.builtin.MoveProviderUtil;
import ai.timefold.solver.core.impl.move.builtin.SampleValueRanges;
import ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.NeighborhoodsCollectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws two pillars, each a set of entities sharing the same combination of values across every variable given to the
 * constructor, and swaps that combination between the two pillars,
 * provided at least one variable differs and every differing variable is legal on both sides;
 * if any differing variable is out of range, the pair is skipped entirely.
 * A pillar's composite key is a list with one value per variable, in {@link PlanningEntityMetaModel#variables()}
 * declaration order, regardless of the order the constructor was given.
 * Size-1 pillars are legal on both sides of the swap, since we need to be able to swap 1-sized pillar with an n-sized
 * pillar.
 * <p>
 * Draws whole pillars only, unbounded by design: a pillar move is defined as moving every member of the pillar,
 * so its cost is linear in the pillar's size with no cap.
 * <p>
 * Unlike {@code SwapMoveProvider}, this never moves an unassigned value across:
 * a pillar is keyed on a shared value, and unassigned is the absence of a value, not one.
 *
 * @see SubPillarSwapMoveProvider A sampler-driven, size-bounded subset of the pillar.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 */
@NullMarked
public final class PillarSwapMoveProvider<Solution_, Entity_>
        implements MoveProvider<Solution_> {

    private final GenuineEntityMetaModel<Solution_, Entity_> entityMetaModel;
    private final List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList;

    /**
     * As defined by {@link #PillarSwapMoveProvider(List)},
     * but for every basic planning variable of {@code entityMetaModel}.
     */
    public PillarSwapMoveProvider(GenuineEntityMetaModel<Solution_, Entity_> entityMetaModel) {
        this(MoveProviderUtil.basicVariablesOf(entityMetaModel));
    }

    /**
     * As defined by {@link #PillarSwapMoveProvider(List)}, but for a single variable.
     */
    public PillarSwapMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, ?> variableMetaModel) {
        this(List.of(variableMetaModel));
    }

    /**
     * Every listed variable participates in the pillar's composite key.
     * A pair is proposed only when at least one listed variable differs
     * and every differing variable is legal on both sides;
     * if any differing variable is out of range, the pair is skipped entirely.
     * All variables must belong to the same entity class.
     *
     * @param variableMetaModelList must not be empty
     */
    public PillarSwapMoveProvider(List<? extends PlanningVariableMetaModel<Solution_, Entity_, ?>> variableMetaModelList) {
        this.variableMetaModelList = MoveProviderUtil.normalize(variableMetaModelList);
        this.entityMetaModel = variableMetaModelList.getFirst().entity();
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var pillarDataset = moveStreamFactory.forEach(entityMetaModel.type(), false)
                .groupBy((solutionView, entity) -> MoveProviderUtil.compositeKeyOf(entity, variableMetaModelList),
                        NeighborhoodsCollectors.collectAndThen(NeighborhoodsCollectors.toList(), Sample::ofUniqueElements))
                .map((solutionView, key, pillar) -> new PillarWithRanges<>(pillar,
                        MoveProviderUtil.rangesPerVariableOf(pillar, variableMetaModelList, solutionView)))
                .asCachedDataset();
        return moveStreamFactory.buildMoveStream((session, random) -> new PillarSwapMoveIterator<>(session, random,
                variableMetaModelList, pillarDataset));
    }

    /**
     * A pillar bundled with its per-variable {@link SampleValueRanges}, computed once when the {@code .map(...)} step
     * settles for a dirty group (see {@link #build}), not once per candidate probe - the row is stable, and reused,
     * for as long as the pillar's group is unchanged, which can span many local search steps, unlike a cache field
     * owned by a move iterator that gets rebuilt every step.
     */
    private record PillarWithRanges<Entity_>(Sample<Entity_> pillar, List<SampleValueRanges<Object>> rangesPerVariable) {
    }

    /**
     * Draws two independently sampled whole pillars and swaps every listed variable's value between them,
     * producing a {@code PillarSwapMove}.
     * Left and right are both pillars, each independently drawn from the same cached dataset; the two pillars must differ.
     * <p>
     * Legality is checked against the pillars' own {@link ValueRange}s ({@link SampleValueRanges#of})
     * rather than by an {@code isValueInRange} call per member: the candidate partner pillar still has to be searched
     * for (its "value" is the other side's current value, not something drawn from a range),
     * therefore the {@link FilteringIterator} search.
     *
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     */
    @NullMarked
    private static final class PillarSwapMoveIterator<Solution_, Entity_>
            implements Iterator<Move<Solution_>>, RetiringBiWalk<PillarWithRanges<Entity_>, PillarWithRanges<Entity_>> {

        private final List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList;
        private final RandomGenerator random;
        private final SolutionView<Solution_> solutionView;
        private final RetiringRandomIterator<PillarWithRanges<Entity_>> leftPillarIterator;
        private final DefaultUniDatasetInstance<Solution_, PillarWithRanges<Entity_>> pillarInstance;

        private @Nullable Move<Solution_> nextMove = null;

        public PillarSwapMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
                List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList,
                UniDataset<Solution_, PillarWithRanges<Entity_>> pillarDataset) {
            this.variableMetaModelList = Objects.requireNonNull(variableMetaModelList);
            this.random = Objects.requireNonNull(random);
            this.solutionView = session.getSolutionView();
            this.pillarInstance =
                    (DefaultUniDatasetInstance<Solution_, PillarWithRanges<Entity_>>) session.getInstance(pillarDataset);
            this.leftPillarIterator = pillarInstance.retiringRandomIterator(random);
        }

        @Override
        public boolean hasNext() {
            return nextMove != null || RetiringBiWalk.advance(leftPillarIterator, this);
        }

        @Override
        public Move<Solution_> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var move = Objects.requireNonNull(nextMove);
            nextMove = null;
            return move;
        }

        @Override
        public Iterator<PillarWithRanges<Entity_>> createRightIterator(PillarWithRanges<Entity_> leftEntry) {
            // Scaled to the candidate pool's size.
            // Each candidate is read directly off a cached row, ranges included,
            // so isValidSwap is the only cost per candidate - nothing here recomputes anything.

            var candidateIterator = pillarInstance.iterator(random);
            var bailOutSize = pillarInstance.size() * FilteringIterator.BAIL_OUT_SAFETY_MULTIPLIER;
            return new FilteringIterator<>(candidateIterator,
                    // Rows are cached and reused for as long as their group is unchanged (see
                    // PillarWithRanges above), so two draws of the same pillar are the same object;
                    // no need to compare pillar contents to detect a self-swap.
                    candidate -> candidate != leftEntry &&
                            MoveProviderUtil.isValidSwap(solutionView, variableMetaModelList, leftEntry.pillar(),
                                    leftEntry.rangesPerVariable(), candidate.pillar(), candidate.rangesPerVariable()),
                    bailOutSize);
        }

        @Override
        public void accept(PillarWithRanges<Entity_> leftEntry, PillarWithRanges<Entity_> rightEntry) {
            nextMove = Moves.pillarSwap(variableMetaModelList, leftEntry.pillar(), rightEntry.pillar());
        }

    }
}
