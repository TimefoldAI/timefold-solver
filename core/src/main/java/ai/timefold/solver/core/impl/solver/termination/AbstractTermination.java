package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.api.solver.termination.TerminationId;
import ai.timefold.solver.core.api.solver.termination.TerminationType;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
abstract sealed class AbstractTermination<Solution_>
        implements Termination<Solution_>
        permits AbstractPhaseTermination, AbstractUniversalTermination {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    private @Nullable TerminationId terminationId = null;

    @Override
    public @Nullable TerminationId getTerminationId() {
        return terminationId;
    }

    void setTerminationId(TerminationId terminationId) {
        this.terminationId = terminationId;
    }

    /**
     * @return the type of this leaf; null if this termination is not a leaf
     */
    @Nullable
    TerminationType getTerminationType() {
        return null;
    }

}
