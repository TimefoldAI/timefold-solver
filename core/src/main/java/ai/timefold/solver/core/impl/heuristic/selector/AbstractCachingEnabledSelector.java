package ai.timefold.solver.core.impl.heuristic.selector;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This is a utility selector class designed to incorporate a straightforward caching function into the selector.
 */
@NullMarked
public abstract class AbstractCachingEnabledSelector<Solution_, Type_> extends AbstractDemandEnabledSelector<Solution_> {

    private @Nullable Type_ cachedItem = null;
    private long revision;

    // ************************************************************************
    // Life-cycle methods
    // ************************************************************************

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        buildCache(phaseScope.getScoreDirector());
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        this.cachedItem = null;
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        buildCache(stepScope.getScoreDirector());
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    private void buildCache(InnerScoreDirector<Solution_, ?> scoreDirector) {
        if (cachedItem == null || scoreDirector.isWorkingEntityListDirty(revision)) {
            this.cachedItem = buildCacheItem(scoreDirector);
            this.revision = scoreDirector.getWorkingEntityListRevision();
        }
    }

    public @Nullable Type_ getCachedItem() {
        return cachedItem;
    }

    public abstract Type_ buildCacheItem(InnerScoreDirector<Solution_, ?> scoreDirector);

}
