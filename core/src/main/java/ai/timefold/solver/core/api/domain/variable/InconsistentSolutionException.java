package ai.timefold.solver.core.api.domain.variable;

import java.util.List;

import ai.timefold.solver.core.api.score.analysis.LoopedVariableInfo;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class InconsistentSolutionException extends RuntimeException {
    private final Object solution;
    private final List<LoopedVariableInfo> inconsistentGroups;

    public InconsistentSolutionException(String feature, Object solution, List<LoopedVariableInfo> inconsistentGroups) {
        super("The solution (%s) is inconsistent. %s requires a consistent solution.".formatted(solution, feature));
        this.solution = solution;
        this.inconsistentGroups = inconsistentGroups;
    }

    @SuppressWarnings("unchecked")
    public <T> T getSolution() {
        return (T) solution;
    }

    public List<LoopedVariableInfo> getInconsistentGroups() {
        return inconsistentGroups;
    }
}
