package ai.timefold.solver.core.api.score.analysis;

import java.util.List;

import org.jspecify.annotations.NullMarked;

/**
 * Represents a breakdown of the structural flaws of a solution.
 */
@NullMarked
public interface StructuralFlawAnalysis {
    /**
     * Return a list of independent {@link VariableLoop}
     * that form cycles and thus cause inconsistencies in the solution.
     */
    List<VariableLoop> getVariableLoops();
}
