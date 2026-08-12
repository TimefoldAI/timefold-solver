package ai.timefold.solver.core.impl.neighborhood.bias;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultNeighborhoodSession;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.util.ElementAwareArrayList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Shared root for every selection-bias {@code *IT}: {@code @Execution} is {@code @Inherited}, so
 * annotating this class alone puts every subclass, and every one of its {@code @Test} methods, on
 * the concurrent execution plan (already enabled for this module by
 * {@code core/src/test/resources/junit-platform.properties}). Being abstract, this class matches
 * Failsafe's {@code **&#47;*IT.java} include but contributes no test of its own, the same
 * situation {@code AbstractIndexerTest} is already in for Surefire.
 * <p>
 * These tests assert a <b>statistical</b> property (uniform, weight-proportional, or a
 * deliberately non-uniform ratio) of a random draw, via {@link BiasReport}. A regression test
 * pinning one exact, deterministic outcome does not belong here; it stays next to the production
 * class it protects.
 */
@Execution(ExecutionMode.CONCURRENT)
abstract class AbstractBiasIT {

    /**
     * How many standard deviations of sampling noise a category's observed count may be away from
     * its expected count before {@link BiasReport#assertWithinSigma(double)} fails.
     * <p>
     * These are max-of-many-categories tests, so the multiple-comparison penalty is real: at 5
     * sigma, a test over {@code k} categories has an expected false-failure rate of about
     * {@code k * 5.7e-7} per run (two-sided). At the largest fixture here (310 pairs) that is
     * about {@code 1.8e-4} per run; at 3 sigma it would be about 1 run in 120. Do not lower this
     * to make a specific case pass — a case that only passes below 5 sigma has a real, unexplained
     * bias, and belongs either fixed or documented with a per-case override, not hidden by a
     * looser global constant.
     */
    static final double SIGMA_LIMIT = 5.0;

    /**
     * Builds one trial's seed from a root random, rather than from an incrementing counter
     * ({@code new Random(0)}, {@code new Random(1)}, ...): {@code java.util.Random}'s first
     * {@code nextInt(2)} call is constant across such small, close seeds (an LCG artifact), which
     * would make a first-draw bias undetectable no matter how large it is.
     */
    static Random splitFrom(Random root) {
        return new Random(root.nextLong());
    }

    static <T> ElementAwareArrayList<T> toEntries(List<T> elements) {
        var list = new ElementAwareArrayList<T>();
        list.addAll(elements);
        return list;
    }

    static Iterator<Move<TestdataSolution>> moveIterator(MoveProvider<TestdataSolution> moveProvider,
            TestdataSolution solution) {
        return NeighborhoodTester.build(moveProvider, TestdataSolution.buildMetaModel())
                .using(solution)
                .getMovesAsIterator();
    }

    static <Category_> BiasReport<Category_> tally(String label, int sampleCount, IntFunction<Category_> sampler) {
        return BiasReport.tally(label, sampleCount, sampler);
    }

    /**
     * Builds and settles a {@code DatasetSession} directly (bypassing a real {@code ScoreDirector}
     * and solver), for the cached and just-in-time bi-dataset cases that have no {@code MoveProvider}
     * route of their own.
     */
    static DefaultNeighborhoodSession<TestdataSolution> session(DefaultMoveStreamFactory<TestdataSolution> moveStreamFactory,
            TestdataSolution solution) {
        var scoreDirector = new EasyScoreDirectorFactory<>(moveStreamFactory.getSolutionDescriptor(),
                s -> SimpleScore.ZERO, EnvironmentMode.PHASE_ASSERT).buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var session = moveStreamFactory.createSession(new SessionContext<>(scoreDirector));
        moveStreamFactory.getSolutionDescriptor().visitAll(solution, session::insert);
        session.settle();
        return session;
    }

}
