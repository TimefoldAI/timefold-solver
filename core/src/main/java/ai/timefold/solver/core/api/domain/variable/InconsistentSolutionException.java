package ai.timefold.solver.core.api.domain.variable;

import java.util.List;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class InconsistentSolutionException extends RuntimeException {
    private final Object solution;
    private final List<Object> involvedEntityList;

    public InconsistentSolutionException(String feature, Object solution, List<Object> involvedEntityList) {
        super("The solution (%s) is inconsistent. %s requires a consistent solution.".formatted(solution, feature));
        this.solution = solution;
        this.involvedEntityList = involvedEntityList;
    }

    @SuppressWarnings("unchecked")
    public <T> T getSolution() {
        return (T) solution;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getInvolvedEntityList() {
        return (List<T>) involvedEntityList;
    }
}
