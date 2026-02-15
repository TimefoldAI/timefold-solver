package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.SequencedSet;

import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.impl.move.VariableChangeRecordingScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class SelectorBasedRuinRecreateMove<Solution_> extends AbstractSelectorBasedMove<Solution_> {

    private final GenuineVariableDescriptor<Solution_> genuineVariableDescriptor;
    private final RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder;
    private final SolverScope<Solution_> solverScope;
    private final List<Object> ruinedEntityList;
    private final SequencedSet<Object> affectedValueSet;

    private Object @Nullable [] recordedNewValues;

    public SelectorBasedRuinRecreateMove(GenuineVariableDescriptor<Solution_> genuineVariableDescriptor,
            RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder,
            SolverScope<Solution_> solverScope, List<Object> ruinedEntityList, SequencedSet<Object> affectedValueSet) {
        this.genuineVariableDescriptor = genuineVariableDescriptor;
        this.ruinedEntityList = ruinedEntityList;
        this.affectedValueSet = affectedValueSet;
        this.constructionHeuristicPhaseBuilder = constructionHeuristicPhaseBuilder;
        this.solverScope = solverScope;
        this.recordedNewValues = null;
    }

    @Override
    protected void execute(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        recordedNewValues = new Object[ruinedEntityList.size()];

        for (var ruinedEntity : ruinedEntityList) {
            scoreDirector.beforeVariableChanged(genuineVariableDescriptor, ruinedEntity);
            genuineVariableDescriptor.setValue(ruinedEntity, null);
            scoreDirector.afterVariableChanged(genuineVariableDescriptor, ruinedEntity);
        }
        scoreDirector.triggerVariableListeners();

        var backingScoreDirector =
                (scoreDirector instanceof VariableChangeRecordingScoreDirector<Solution_, ?> recordingScoreDirector)
                        ? recordingScoreDirector.getBacking()
                        : scoreDirector;
        var innerScoreDirector = (InnerScoreDirector<Solution_, ?>) backingScoreDirector;
        var constructionHeuristicPhase =
                (RuinRecreateConstructionHeuristicPhase<Solution_>) constructionHeuristicPhaseBuilder
                        .ensureThreadSafe(innerScoreDirector)
                        .withElementsToRecreate(ruinedEntityList)
                        .build();

        var nestedSolverScope = new SolverScope<Solution_>(solverScope.getClock());
        nestedSolverScope.setSolver(solverScope.getSolver());
        nestedSolverScope.setScoreDirector(innerScoreDirector);
        constructionHeuristicPhase.solvingStarted(nestedSolverScope);
        constructionHeuristicPhase.solve(nestedSolverScope);
        constructionHeuristicPhase.solvingEnded(nestedSolverScope);
        scoreDirector.triggerVariableListeners();

        for (var i = 0; i < ruinedEntityList.size(); i++) {
            recordedNewValues[i] = genuineVariableDescriptor.getValue(ruinedEntityList.get(i));
        }
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        return ruinedEntityList;
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        return affectedValueSet;
    }

    @Override
    public Move<Solution_> rebase(Rebaser rebaser) {
        var rebasedRuinedEntityList = rebaseList(ruinedEntityList, rebaser);
        var rebasedAffectedValueSet = rebaseSet(affectedValueSet, rebaser);
        return new SelectorBasedRuinRecreateMove<>(genuineVariableDescriptor, constructionHeuristicPhaseBuilder, solverScope,
                rebasedRuinedEntityList, rebasedAffectedValueSet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SelectorBasedRuinRecreateMove<?> that))
            return false;
        return Objects.equals(genuineVariableDescriptor, that.genuineVariableDescriptor)
                && Objects.equals(ruinedEntityList, that.ruinedEntityList)
                && Objects.equals(affectedValueSet, that.affectedValueSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(genuineVariableDescriptor, ruinedEntityList, affectedValueSet);
    }

    @Override
    public String toString() {
        return "RuinMove{" +
                "entities=" + ruinedEntityList +
                ", newValues=" + ((recordedNewValues != null) ? Arrays.toString(recordedNewValues) : "?") +
                '}';
    }
}
