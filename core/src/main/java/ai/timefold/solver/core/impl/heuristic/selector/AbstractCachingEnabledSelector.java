package ai.timefold.solver.core.impl.heuristic.selector;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.heuristic.selector.common.SelectionCacheLifecycleBridge;
import ai.timefold.solver.core.impl.heuristic.selector.common.SelectionCacheLifecycleListener;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This is a utility selector class designed to incorporate a straightforward caching function into the selector.
 */
@NullMarked
public abstract class AbstractCachingEnabledSelector<Solution_, Type_> extends AbstractDemandEnabledSelector<Solution_>
        implements SelectionCacheLifecycleListener<Solution_> {

    private @Nullable Type_ cachedItem = null;
    private long revision;

    protected AbstractCachingEnabledSelector(SelectionCacheType... selectionCacheType) {
        for (var type : selectionCacheType) {
            phaseLifecycleSupport.addEventListener(new SelectionCacheLifecycleBridge<>(type, this));
        }
    }

    @Override
    public void constructCache(SolverScope<Solution_> solverScope) {
        var scoreDirector = solverScope.getScoreDirector();
        if (cachedItem == null || scoreDirector.isWorkingEntityListDirty(revision)) {
            this.cachedItem = buildCacheItem(scoreDirector);
            this.revision = scoreDirector.getWorkingEntityListRevision();
        }
    }

    @Override
    public void disposeCache(SolverScope<Solution_> solverScope) {
        // Delegated to the child classes
    }

    public @Nullable Type_ getCachedItem() {
        return cachedItem;
    }

    public void resetCacheItem() {
        this.cachedItem = null;
    }

    public abstract Type_ buildCacheItem(InnerScoreDirector<Solution_, ?> scoreDirector);

}
