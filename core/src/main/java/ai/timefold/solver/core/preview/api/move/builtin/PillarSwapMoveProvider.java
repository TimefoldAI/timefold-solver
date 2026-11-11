package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.List;

import ai.timefold.solver.core.impl.move.builtin.MoveProviderUtil;
import ai.timefold.solver.core.impl.move.builtin.PillarSwapMoveIterator;
import ai.timefold.solver.core.impl.move.builtin.PillarWithRanges;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.CachedBiDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.NeighborhoodsCollectors;

import org.jspecify.annotations.NullMarked;

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
                .map((solutionView, key, pillar) -> key,
                        (solutionView, key, pillar) -> new PillarWithRanges<>(pillar,
                                MoveProviderUtil.rangesPerVariableOf(pillar, variableMetaModelList, solutionView)))
                .asCachedDataset();
        return moveStreamFactory.buildMoveStream((session, random) -> new PillarSwapMoveIterator<>(session.getSolutionView(),
                random, variableMetaModelList,
                (CachedBiDatasetInstance<Solution_, List<Object>, PillarWithRanges<Entity_>>) session
                        .getInstance(pillarDataset)));
    }

}
