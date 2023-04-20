package ai.timefold.solver.core.impl.localsearch.decider.forager;

import ai.timefold.solver.core.impl.localsearch.event.LocalSearchPhaseLifecycleListenerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract superclass for {@link LocalSearchForager}.
 *
 * @see LocalSearchForager
 */
public abstract class AbstractLocalSearchForager<Solution_> extends LocalSearchPhaseLifecycleListenerAdapter<Solution_>
        implements LocalSearchForager<Solution_> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    // ************************************************************************
    // Worker methods
    // ************************************************************************

}
