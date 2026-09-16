package ai.timefold.solver.core.impl.localsearch.decider.forager.finalist;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.localsearch.event.LocalSearchPhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

/**
 * Abstract superclass for {@link FinalistPodium}.
 *
 * @see FinalistPodium
 */
public abstract class AbstractFinalistPodium<Solution_> extends LocalSearchPhaseLifecycleListenerAdapter<Solution_>
        implements FinalistPodium<Solution_> {

    protected static final int FINALIST_LIST_MAX_SIZE = 1_024_000;

    protected boolean finalistIsAccepted;
    protected List<LocalSearchMoveScope<Solution_>> finalistList = new ArrayList<>(1024);

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        finalistIsAccepted = false;
        finalistList.clear();
    }

    protected void clearAndAddFinalist(LocalSearchMoveScope<Solution_> moveScope) {
        finalistList.clear();
        if (moveScope.getScore().isStructurallyFlawed()) {
            throw new IllegalStateException("Impossible state: Finalist (%s) is structurally flawed."
                    .formatted(moveScope));
        }
        finalistList.add(moveScope);
    }

    protected void addFinalist(LocalSearchMoveScope<Solution_> moveScope) {
        if (finalistList.size() >= FINALIST_LIST_MAX_SIZE) {
            // Avoid unbounded growth and OutOfMemoryException
            return;
        }
        if (moveScope.getScore().isStructurallyFlawed()) {
            throw new IllegalStateException("Impossible state: Finalist (%s) is structurally flawed."
                    .formatted(moveScope));
        }
        finalistList.add(moveScope);
    }

    @Override
    public List<LocalSearchMoveScope<Solution_>> getFinalistList() {
        return finalistList;
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        finalistIsAccepted = false;
        finalistList.clear();
    }

}
