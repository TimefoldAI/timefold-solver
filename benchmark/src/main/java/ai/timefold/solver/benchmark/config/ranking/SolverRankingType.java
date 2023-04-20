package ai.timefold.solver.benchmark.config.ranking;

import javax.xml.bind.annotation.XmlEnum;

import ai.timefold.solver.benchmark.impl.ranking.TotalRankSolverRankingWeightFactory;
import ai.timefold.solver.benchmark.impl.ranking.TotalScoreSolverRankingComparator;
import ai.timefold.solver.benchmark.impl.ranking.WorstScoreSolverRankingComparator;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

@XmlEnum
public enum SolverRankingType {
    /**
     * Maximize the overall score, so minimize the overall cost if all {@link PlanningSolution}s would be executed.
     *
     * @see TotalScoreSolverRankingComparator
     */
    TOTAL_SCORE,
    /**
     * Minimize the worst case scenario.
     *
     * @see WorstScoreSolverRankingComparator
     */
    WORST_SCORE,
    /**
     * Maximize the overall ranking.
     *
     * @see TotalRankSolverRankingWeightFactory
     */
    TOTAL_RANKING;

}
