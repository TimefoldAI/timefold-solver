package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.constructionheuristic.ConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class RuinRecreateConstructionHeuristicPhase<Solution_>
        extends DefaultConstructionHeuristicPhase<Solution_>
        implements ConstructionHeuristicPhase<Solution_> {

    private final Set<Object> elementsToRuinSet;
    // Store the original value list of elements that are not included in the initial list of ruined elements
    private final Map<Object, List<Object>> missingUpdatedElementsMap;

    RuinRecreateConstructionHeuristicPhase(RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> builder) {
        super(builder);
        this.elementsToRuinSet = Objects.requireNonNullElse(builder.elementsToRuin, Collections.emptySet());
        this.missingUpdatedElementsMap = new IdentityHashMap<>();
    }

    @Override
    protected ConstructionHeuristicPhaseScope<Solution_> buildPhaseScope(SolverScope<Solution_> solverScope, int phaseIndex) {
        return new RuinRecreateConstructionHeuristicPhaseScope<>(solverScope, phaseIndex);
    }

    @Override
    protected boolean isNested() {
        return true;
    }

    @Override
    public String getPhaseTypeString() {
        return "Ruin & Recreate Construction Heuristics";
    }

    @Override
    protected void doStep(ConstructionHeuristicStepScope<Solution_> stepScope) {
        if (!elementsToRuinSet.isEmpty()) {
            var listVariableDescriptor = stepScope.getPhaseScope().getSolverScope().getSolutionDescriptor()
                    .getListVariableDescriptor();
            var entity = stepScope.getStep().extractPlanningEntities().iterator().next();
            if (!elementsToRuinSet.contains(entity)) {
                // Sometimes, the list of elements to be ruined does not include new destinations selected by the CH.
                // In these cases, we need to record the element list before making any move changes
                // so that it can be referenced to restore the solution to its original state when undoing changes.
                missingUpdatedElementsMap.computeIfAbsent(entity,
                        e -> List.copyOf(listVariableDescriptor.getValue(e)));
            }
        }
        super.doStep(stepScope);
    }

    public Map<Object, List<Object>> getMissingUpdatedElementsMap() {
        return missingUpdatedElementsMap;
    }
}
