package ai.timefold.solver.core.impl.heuristic.move;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.AbstractValueRangeDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedSwapMove;
import ai.timefold.solver.core.impl.move.MoveDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Rebaser;
import ai.timefold.solver.core.preview.api.neighborhood.Neighborhood;

import org.jspecify.annotations.NullMarked;

/**
 * Abstract superclass for {@link Move},
 * intended to grandfather in the legacy {@link Move} implementations that are supplied by move selectors.
 * These legacy move selectors are built around the idea
 * of {@link #isMoveDoable(ScoreDirector) non-doable moves} being generated
 * and only later filtered out by the solver.
 * This class exists for the benefit of such move selectors.
 * <p>
 * These days moves are built around the idea of only generating doable moves,
 * and nothing prevents new move selectors from being written in that way as well.
 * In that case, these move implementations can simply extend {@link Move} directly
 * and not bother with {@link AbstractSelectorBasedMove} at all.
 * However, users are encouraged not to implement any new move selectors anymore,
 * and instead use the {@link Neighborhood Neighborhoods} API to implements their custom moves.
 * <p>
 * Moves that extend this class are expected to be selector-based,
 * and should be named with the "SelectorBased" prefix,
 * like {@link SelectorBasedChangeMove} and {@link SelectorBasedSwapMove}.
 * This is to quickly distinguish them in stack traces and IDEs from other moves that do not extend this class
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see Move
 */
@NullMarked
public abstract class AbstractSelectorBasedMove<Solution_> implements Move<Solution_> {

    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return true;
    }

    @Override
    public final void execute(MutableSolutionView<Solution_> solutionView) {
        var scoreDirector = ((MoveDirector<Solution_, ?>) solutionView).getScoreDirector();
        execute(scoreDirector);
        scoreDirector.triggerVariableListeners();
    }

    protected abstract void execute(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector);

    @Override
    public String describe() {
        // Do not include the "SelectorBased" prefix in the description,
        // as that is just an implementation detail that is not relevant to the user.
        var name = getClass().getSimpleName();
        if (name.startsWith("SelectorBased")) {
            return name.substring("SelectorBased".length());
        }
        return name;
    }

    protected <Value_> ValueRange<Value_> extractValueRangeFromEntity(ScoreDirector<Solution_> scoreDirector,
            AbstractValueRangeDescriptor<Solution_> valueRangeDescriptor, Object entity) {
        var castScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        return castScoreDirector.getValueRangeManager()
                .getFromEntity(valueRangeDescriptor, entity);
    }

    // ************************************************************************
    // Util methods
    // ************************************************************************

    public static <E> List<E> rebaseList(List<E> externalObjectList, Rebaser rebaser) {
        var rebasedObjectList = new ArrayList<E>(externalObjectList.size());
        for (var entity : externalObjectList) {
            rebasedObjectList.add(rebaser.rebase(entity));
        }
        return rebasedObjectList;
    }

    public static <E> SequencedSet<E> rebaseSet(Set<E> externalObjectSet, Rebaser rebaser) {
        var rebasedObjectSet = LinkedHashSet.<E> newLinkedHashSet(externalObjectSet.size());
        for (var entity : externalObjectSet) {
            rebasedObjectSet.add(rebaser.rebase(entity));
        }
        return rebasedObjectSet;
    }

}
