package ai.timefold.solver.core.impl.heuristic.move;

import java.util.Collections;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;

/**
 * Makes no changes.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public final class SelectorBasedNoChangeMove<Solution_> extends AbstractSelectorBasedMove<Solution_> {

    public static final SelectorBasedNoChangeMove<?> INSTANCE = new SelectorBasedNoChangeMove<>();

    @SuppressWarnings("unchecked")
    public static <Solution_> SelectorBasedNoChangeMove<Solution_> getInstance() {
        return (SelectorBasedNoChangeMove<Solution_>) INSTANCE;
    }

    private SelectorBasedNoChangeMove() {
        // No external instances allowed.
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return false;
    }

    @Override
    protected void execute(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        // Do nothing.
    }

    @Override
    public Move<Solution_> rebase(Rebaser rebaser) {
        return getInstance();
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        return Collections.emptyList();
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        return Collections.emptyList();
    }

}
