package ai.timefold.solver.core.impl.solver.termination;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
abstract sealed class AbstractTermination<Solution_>
        implements Termination<Solution_>
        permits AbstractPhaseTermination, AbstractUniversalTermination {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

}
