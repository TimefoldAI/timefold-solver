package ai.timefold.solver.core.api.domain.variable;

import java.util.List;

import ai.timefold.solver.core.api.score.analysis.VariableLoop;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class InconsistentSolutionException extends RuntimeException {
    private final Object solution;
    private final List<VariableLoop> variableLoops;

    public InconsistentSolutionException(String feature, Object solution, List<VariableLoop> variableLoops) {
        super("The solution (%s) is inconsistent. %s requires a consistent solution.".formatted(solution, feature));
        this.solution = solution;
        this.variableLoops = variableLoops;
    }

    @SuppressWarnings("unchecked")
    public <T> T getSolution() {
        return (T) solution;
    }

    public List<VariableLoop> getVariableLoops() {
        return variableLoops;
    }
}
