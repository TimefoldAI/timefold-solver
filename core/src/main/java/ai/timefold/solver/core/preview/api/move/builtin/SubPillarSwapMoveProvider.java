package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.move.builtin.MoveProviderUtil;
import ai.timefold.solver.core.impl.move.builtin.SampleValueRanges;
import ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.PillarSampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws two subpillars, each governed by its own {@link PillarSampler} -
 * see {@link Samplers#pillar(Sampler) Samplers.pillar} to lift a size-only policy,
 * since an unbounded sampler makes this provider's move cost linear in the pillars' combined size -
 * out of entities sharing the same combination of values across every variable given to the constructor,
 * and swaps that combination between the two subpillars,
 * provided at least one variable differs and every differing variable is legal on both sides;
 * if any differing variable is out of range, the pair is skipped entirely.
 * A pillar's composite key is a list with one value per variable, in {@link GenuineEntityMetaModel#variables()}
 * declaration order, regardless of the order the constructor was given.
 * <p>
 * For the whole pillar on both sides at once, see {@code PillarSwapMoveProvider}.
 * <p>
 * There is no single-sampler overload: the two sides may want different policies (a tighter cap on one side, say),
 * and passing two parameters says so explicitly.
 * Sharing one instance between both sides is safe - see {@link PillarSampler}'s class documentation for why -
 * but is rarely what a caller wants.
 * <p>
 * Unlike {@code SwapMoveProvider}, this never moves an unassigned value across:
 * a pillar is keyed on a shared value, and unassigned is the absence of a value, not one.
 *
 * @see PillarSwapMoveProvider The whole pillar on both sides at once.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 */
@NullMarked
public final class SubPillarSwapMoveProvider<Solution_, Entity_>
        implements MoveProvider<Solution_> {

    private final GenuineEntityMetaModel<Solution_, Entity_> entityMetaModel;
    private final List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList;
    private final PillarSampler<List<Object>, Entity_> leftSampler;
    private final PillarSampler<List<Object>, Entity_> rightSampler;

    /**
     * As defined by {@link #SubPillarSwapMoveProvider(List, PillarSampler, PillarSampler)},
     * but for every basic planning variable of {@code entityMetaModel}.
     */
    public SubPillarSwapMoveProvider(GenuineEntityMetaModel<Solution_, Entity_> entityMetaModel,
            PillarSampler<List<Object>, Entity_> leftSampler, PillarSampler<List<Object>, Entity_> rightSampler) {
        this(MoveProviderUtil.basicVariablesOf(entityMetaModel), leftSampler, rightSampler);
    }

    /**
     * As defined by {@link #SubPillarSwapMoveProvider(List, PillarSampler, PillarSampler)},
     * but for a single variable.
     */
    public SubPillarSwapMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, ?> variableMetaModel,
            PillarSampler<List<Object>, Entity_> leftSampler, PillarSampler<List<Object>, Entity_> rightSampler) {
        this(List.of(variableMetaModel), leftSampler, rightSampler);
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
    public SubPillarSwapMoveProvider(List<? extends PlanningVariableMetaModel<Solution_, Entity_, ?>> variableMetaModelList,
            PillarSampler<List<Object>, Entity_> leftSampler, PillarSampler<List<Object>, Entity_> rightSampler) {
        this.variableMetaModelList = MoveProviderUtil.normalize(variableMetaModelList);
        this.entityMetaModel = variableMetaModelList.getFirst().entity();
        this.leftSampler = Objects.requireNonNull(leftSampler);
        this.rightSampler = Objects.requireNonNull(rightSampler);
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var entityStream = moveStreamFactory.forEach(entityMetaModel.type(), false);
        // groupBy yields one element per group, which is exactly the set of distinct composite keys;
        // no joiner can express "the distinct set of keys", so groupBy is required here.
        var distinctKeys = entityStream
                .groupBy((solutionView, entity) -> MoveProviderUtil.compositeKeyOf(entity, variableMetaModelList))
                .asCachedDataset();
        // A real equal-join: the joiner's plain Function reads every variable directly, bypassing SolutionView,
        // so UniDataset.join(...) resolves to an indexed JustInTimeBiDataset lookup.
        // TODO possible performance improvement: list whole pillars, caching their ranges as PillarSwap does,
        //  and then subsample them, carrying that information over.
        var pillarSourceDataset = distinctKeys.join(entityStream,
                NeighborhoodsJoiners.equal(Function.identity(),
                        entity -> MoveProviderUtil.compositeKeyOf(entity, variableMetaModelList)));
        return moveStreamFactory.buildMoveStream((session, random) -> new SubPillarSwapMoveIterator<>(session, random,
                variableMetaModelList, distinctKeys, pillarSourceDataset, leftSampler, rightSampler));
    }

    /**
     * Draws two independently sampled pillar-slice keys and swaps every listed variable's value between a subpillar drawn
     * from each, producing a {@code PillarSwapMove}.
     * Left and right are both composite keys, each independently drawn from the same {@code distinctKeys} dataset;
     * the two keys must differ.
     * <p>
     * Unlike the whole-pillar variant, a fresh left subpillar is assembled on <strong>every</strong>
     * {@link #createRightIterator} call, never cached across probes: a real {@link PillarSampler} can legitimately draw
     * a different subpillar for the same left key on each attempt (and a different subpillar can have different legal
     * ranges, since fewer members mean fewer constraints), so caching the first one would turn
     * {@link RetiringBiWalk}'s remaining probes into deterministic no-ops for the left side.
     * The right side is drawn fresh per candidate probed, same as the whole-pillar variant.
     * <p>
     * Legality is checked against the subpillars' own {@link ValueRange}s ({@link SampleValueRanges#of})
     * rather than by an {@code isValueInRange} call per member: the candidate partner subpillar still has to be searched
     * for (its "value" is the other side's current value, not something drawn from a range),
     * therefore the {@link FilteringIterator} search.
     *
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     */
    @NullMarked
    private static final class SubPillarSwapMoveIterator<Solution_, Entity_>
            implements Iterator<Move<Solution_>>, RetiringBiWalk<List<Object>, List<Object>> {

        private final List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList;
        private final PillarSampler<List<Object>, Entity_> leftSampler;
        private final PillarSampler<List<Object>, Entity_> rightSampler;
        private final RandomGenerator random;
        private final SolutionView<Solution_> solutionView;
        private final RetiringRandomIterator<List<Object>> leftKeyIterator;
        private final DefaultUniDatasetInstance<Solution_, List<Object>> distinctKeysInstance;
        private final BiDatasetInstance<List<Object>, Entity_> pillarSourceInstance;

        private @Nullable Move<Solution_> nextMove = null;
        private @Nullable Sample<Entity_> pendingLeftPillar = null;
        private @Nullable Sample<Entity_> pendingRightPillar = null;

        public SubPillarSwapMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
                List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList,
                UniDataset<Solution_, List<Object>> distinctKeys,
                BiDataset<Solution_, List<Object>, Entity_> pillarSourceDataset,
                PillarSampler<List<Object>, Entity_> leftSampler, PillarSampler<List<Object>, Entity_> rightSampler) {
            this.variableMetaModelList = Objects.requireNonNull(variableMetaModelList);
            this.leftSampler = Objects.requireNonNull(leftSampler);
            this.rightSampler = Objects.requireNonNull(rightSampler);
            this.random = Objects.requireNonNull(random);
            this.solutionView = session.getSolutionView();
            this.distinctKeysInstance = (DefaultUniDatasetInstance<Solution_, List<Object>>) session.getInstance(distinctKeys);
            this.leftKeyIterator = distinctKeysInstance.retiringRandomIterator(random);
            this.pillarSourceInstance = session.getInstance(pillarSourceDataset);
        }

        @Override
        public boolean hasNext() {
            return nextMove != null || RetiringBiWalk.advance(leftKeyIterator, this);
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
        public Iterator<List<Object>> createRightIterator(List<Object> leftKey) {
            // Fresh left subpillar and fresh ranges on every call.

            var leftPillars = pillarSourceInstance.samplingIterator(leftKey, leftSampler, random);
            var leftPillar = leftPillars.hasNext() ? leftPillars.next() : null;
            if (leftPillar == null) {
                pendingLeftPillar = null;
                return Collections.emptyIterator();
            }
            var leftRangesPerVariable = MoveProviderUtil.rangesPerVariableOf(leftPillar, variableMetaModelList, solutionView);
            // Scaled to the candidate pool's size.
            // Each candidate key is checked cheaply first (it must differ from leftKey) before paying to assemble the
            // right subpillar and run isValidSwap; a match sets pendingLeftPillar/pendingRightPillar as a side effect
            // for accept() to consume.
            var keyIterator = distinctKeysInstance.iterator(random);
            var bailOutSize = distinctKeysInstance.size() * FilteringIterator.BAIL_OUT_SAFETY_MULTIPLIER;
            return new FilteringIterator<>(keyIterator, candidateKey -> {
                if (Objects.equals(candidateKey, leftKey)) {
                    return false;
                }
                var rightPillars = pillarSourceInstance.samplingIterator(candidateKey, rightSampler, random);
                var rightPillar = rightPillars.hasNext() ? rightPillars.next() : null;
                if (rightPillar == null) {
                    return false;
                }
                var rightRangesPerVariable =
                        MoveProviderUtil.rangesPerVariableOf(rightPillar, variableMetaModelList, solutionView);
                if (MoveProviderUtil.isValidSwap(solutionView, variableMetaModelList, leftPillar, leftRangesPerVariable,
                        rightPillar, rightRangesPerVariable)) {
                    pendingLeftPillar = leftPillar;
                    pendingRightPillar = rightPillar;
                    return true;
                }
                return false;
            }, bailOutSize);
        }

        @Override
        public void accept(List<Object> leftKey, List<Object> rightKey) {
            nextMove = Moves.pillarSwap(variableMetaModelList, Objects.requireNonNull(pendingLeftPillar),
                    Objects.requireNonNull(pendingRightPillar));
            pendingLeftPillar = null;
            pendingRightPillar = null;
        }

    }
}
