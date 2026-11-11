package ai.timefold.solver.core.impl.move.builtin;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.CachedBiDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDatasetInstance;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws a left pillar, then the right pillar through a member drawn from
 * {@link MoveProviderUtil#swapCandidatesOf}, and swaps every listed variable's value between them.
 * A pillar is reached once per member in the candidate list,
 * so a drawn pillar is kept only with probability {@code 1/size};
 * every member of a valid pillar is in the list, so every valid pillar then has the same chance.
 * If there are no more pillars than candidates, the right pillar is drawn directly from all pillars instead,
 * which gives the same chances at a lower cost.
 * Legality is checked against the pillars' own {@link ValueRange}s ({@link SampleValueRanges#of}).
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 */
@NullMarked
public final class PillarSwapMoveIterator<Solution_, Entity_>
        extends AbstractLeftDrivenMoveIterator<Solution_, PillarWithRanges<Entity_>> {

    private final List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList;
    private final RandomGenerator random;
    private final SolutionView<Solution_> solutionView;
    private final BiDatasetInstance<List<Object>, PillarWithRanges<Entity_>> pillarInstance;
    // With only solution value ranges, the candidates are all movable entities; never fewer than the pillars.
    private final boolean candidatesMayNarrow;
    private @Nullable Iterator<@Nullable PillarWithRanges<Entity_>> anyPillarIterator;

    public PillarSwapMoveIterator(SolutionView<Solution_> solutionView, RandomGenerator random,
            List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList,
            CachedBiDatasetInstance<Solution_, List<Object>, PillarWithRanges<Entity_>> pillarInstance) {
        super(pillarInstance.retiringRandomIteratorOfB(random));
        this.variableMetaModelList = Objects.requireNonNull(variableMetaModelList);
        this.random = Objects.requireNonNull(random);
        this.solutionView = Objects.requireNonNull(solutionView);
        this.pillarInstance = Objects.requireNonNull(pillarInstance);
        this.candidatesMayNarrow = variableMetaModelList.stream().anyMatch(v -> !v.isValueRangeOnSolution());
    }

    private static <Entity_> Iterator<@Nullable PillarWithRanges<Entity_>> rowIterator(
            BiDatasetInstance<List<Object>, PillarWithRanges<Entity_>> pillarInstance, RandomGenerator random) {
        var biIterator = pillarInstance.iterator(random);
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return biIterator.hasNext();
            }

            @Override
            public @Nullable PillarWithRanges<Entity_> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                biIterator.next();
                return biIterator.b();
            }
        };
    }

    @Override
    protected @Nullable Move<Solution_> tryBuildMove(PillarWithRanges<Entity_> leftEntry) {
        var candidateList = candidatesMayNarrow
                ? MoveProviderUtil.swapCandidatesOf(solutionView, variableMetaModelList,
                        Objects.requireNonNull(leftEntry.pillar().representative()))
                : null;
        PillarWithRanges<Entity_> rightEntry;
        // Both ways give every valid pillar the same chance; take the smaller pool.
        // A null list means that no value narrows the candidates.
        if (candidateList == null || pillarInstance.size() <= candidateList.size()) {
            if (anyPillarIterator == null) {
                anyPillarIterator = rowIterator(pillarInstance, random);
            }
            rightEntry = drawMatching(anyPillarIterator, pillarInstance.size(),
                    candidate -> candidate != null && isValidSwap(leftEntry, candidate));
        } else {
            rightEntry = drawPillarOfCandidate(leftEntry, candidateList);
        }
        return rightEntry == null ? null
                : Moves.pillarSwap(variableMetaModelList, leftEntry.pillar(), rightEntry.pillar());
    }

    /**
     * Draws from the candidates with replacement, with the same budget as {@code drawMatching},
     * and returns the pillar of the first accepted candidate.
     *
     * @return null if no pillar was accepted
     */
    private @Nullable PillarWithRanges<Entity_> drawPillarOfCandidate(PillarWithRanges<Entity_> leftEntry,
            List<Entity_> candidateList) {
        var size = candidateList.size();
        var budget = bailOutBudget(size);
        for (var attempt = 0L; attempt < budget; attempt++) {
            var entity = candidateList.get(random.nextInt(size));
            var rowIterator = pillarInstance.iterator(MoveProviderUtil.compositeKeyOf(entity, variableMetaModelList), random);
            if (!rowIterator.hasNext()) {
                continue;
            }
            var candidate = rowIterator.next();
            // Identity first: members of the left pillar are always candidates, and need no random draw.
            if (candidate == null || candidate == leftEntry || random.nextInt(candidate.pillar().size()) != 0) {
                continue;
            }
            if (isValidSwap(leftEntry, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isValidSwap(PillarWithRanges<Entity_> leftEntry, PillarWithRanges<Entity_> rightEntry) {
        // Rows are cached and reused for as long as their group is unchanged, so identity detects a self-swap.
        return rightEntry != leftEntry && MoveProviderUtil.isValidSwap(solutionView, variableMetaModelList,
                leftEntry.pillar(), leftEntry.rangesPerVariable(), rightEntry.pillar(), rightEntry.rangesPerVariable());
    }

}
