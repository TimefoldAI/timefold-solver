package ai.timefold.solver.core.api.domain.variable;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class InconsistentSolutionException extends RuntimeException {
    private final Object solution;
    private final Collection<Object> involvedEntityCollection;

    public InconsistentSolutionException(String feature, Object solution, Collection<Object> involvedEntityCollection) {
        super("The solution (%s) is inconsistent. %s requires a consistent solution.".formatted(solution, feature));
        this.solution = solution;
        this.involvedEntityCollection = involvedEntityCollection;
    }

    @SuppressWarnings("unchecked")
    public <T> T getSolution() {
        return (T) solution;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getInvolvedEntityCollection() {
        return (List<T>) involvedEntityCollection;
    }
}
