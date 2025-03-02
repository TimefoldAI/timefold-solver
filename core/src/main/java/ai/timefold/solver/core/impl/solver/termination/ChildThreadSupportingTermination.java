package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ChildThreadSupportingTermination<Solution_, Scope_> {

    /**
     * Create a {@link Termination} for a child {@link Thread} of the {@link Solver}.
     *
     * @param scope Either {@link SolverScope} or {@link AbstractPhaseScope}
     * @return not null
     */
    Termination<Solution_> createChildThreadTermination(Scope_ scope, ChildThreadType childThreadType);

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static <Solution_, Scope_> ChildThreadSupportingTermination<Solution_, Scope_>
            assertChildThreadSupport(Termination<Solution_> termination) {
        if (termination instanceof ChildThreadSupportingTermination childThreadSupportingTermination) {
            return childThreadSupportingTermination;
        }
        throw new UnsupportedOperationException(
                "This terminationClass (%s) does not yet support being used in child threads."
                        .formatted(termination.getClass().getSimpleName()));
    }

}
