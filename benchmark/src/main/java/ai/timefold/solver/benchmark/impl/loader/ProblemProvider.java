package ai.timefold.solver.benchmark.impl.loader;

import javax.xml.bind.annotation.XmlSeeAlso;

import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

/**
 * Subclasses need to implement {@link Object#equals(Object) equals()} and {@link Object#hashCode() hashCode()}
 * which are used by {@link ai.timefold.solver.benchmark.impl.ProblemBenchmarksFactory#buildProblemBenchmarkList}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@XmlSeeAlso({
        InstanceProblemProvider.class,
        FileProblemProvider.class
})
public interface ProblemProvider<Solution_> {

    /**
     * @return never null
     */
    String getProblemName();

    /**
     * @return never null
     */
    Solution_ readProblem();

    /**
     * @param solution never null
     * @param subSingleBenchmarkResult never null
     */
    void writeSolution(Solution_ solution, SubSingleBenchmarkResult subSingleBenchmarkResult);

}
