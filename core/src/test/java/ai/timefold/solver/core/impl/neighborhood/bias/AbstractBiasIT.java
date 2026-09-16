package ai.timefold.solver.core.impl.neighborhood.bias;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.BiNeighborhoodsJoiner;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Shared root for every selection-bias {@code *IT}:
 * {@code @Execution} is {@code @Inherited},
 * so annotating this class alone puts every subclass, and every one of its {@code @Test} methods,
 * on the concurrent execution plan.
 * <p>
 * These tests assert a <b>statistical</b> property
 * (uniform, weight-proportional, or a deliberately non-uniform ratio)
 * of a random draw,
 * via {@link BiasReport}.
 * A regression test pinning one exact, deterministic outcome does not belong here;
 * it stays next to the production class it protects.
 */
@Execution(ExecutionMode.CONCURRENT)
public abstract class AbstractBiasIT {

    /**
     * How many standard deviations of sampling noise a category's observed count may be away from its expected count
     * before {@link BiasReport#assertWithinSigma(double)} fails.
     * <p>
     * These are max-of-many-categories tests, so the multiple-comparison penalty is real:
     * at 5 sigma, a test over {@code k} categories has an expected false-failure rate of about {@code k * 5.7e-7} per run
     * (two-sided).
     * At the largest fixture here (310 pairs) that is about {@code 1.8e-4} per run;
     * at 3 sigma it would be about 1 run in 120.
     * Do not lower this to make a specific case pass;
     * a case that only passes below 5 sigma has a real unexplained bias,
     * and belongs either fixed or documented with a per-case override,
     * not hidden by a looser global constant.
     */
    public static final double SIGMA_LIMIT = 5.0;

    /**
     * Builds one trial's seed from a root random,
     * rather than from an incrementing counter ({@code new Random(0)}, {@code new Random(1)}, ...):
     * {@link Random}'s first {@code nextInt(2)} call is constant across such small, close seeds (an LCG artifact),
     * which would make a first-draw bias undetectable no matter how large it is.
     */
    public static Random splitFrom(Random root) {
        return new Random(root.nextLong());
    }

    static <T> ElementAwareArrayList<T> toEntries(List<T> elements) {
        var list = new ElementAwareArrayList<T>();
        list.addAll(elements);
        return list;
    }

    /**
     * How a bias fixture lays its elements out in {@link ElementAwareArrayList}'s physical slots.
     * The random iterators draw over those slots, not over logical indexes,
     * so a fixture built only by {@link #toEntries(List)} never exercises the gap-rejection path at all.
     */
    public enum Layout {

        /**
         * Every slot holds an element, so no draw is ever rejected.
         */
        GAPLESS,
        /**
         * Interior gaps, including one at the first slot and one at the last,
         * so that a draw must reject and redraw, and so that the boundaries are covered.
         */
        GAPPED;

        <T> ElementAwareArrayList<T> build(List<T> elements) {
            return this == GAPLESS ? toEntries(elements) : toEntriesWithGaps(elements);
        }

    }

    /**
     * Builds a list holding exactly {@code elements}, in order, but separated by gaps.
     * Filler entries are interleaved and then removed,
     * leaving the list logically equal to {@link #toEntries(List)} but physically gappy.
     * <p>
     * The list compacts itself once its gaps pass a quarter of its size,
     * so at most {@code elements.size() / 4} fillers can survive;
     * asking for more would make the list compact them all away
     * and silently hand back a gapless fixture which tests nothing.
     * The first and the last slot are gaps whenever the budget allows two fillers,
     * so that the boundaries are covered too.
     */
    static <T> ElementAwareArrayList<T> toEntriesWithGaps(List<T> elements) {
        var elementCount = elements.size();
        var fillerBudget = elementCount / 4;
        if (fillerBudget == 0) { // Too small to hold a gap; the list would just compact it away.
            return toEntries(elements);
        }
        var list = new ElementAwareArrayList<T>();
        var fillerList = new ArrayList<ElementAwareArrayList<T>.Entry>(fillerBudget);
        var filler = elements.get(0);
        fillerList.add(list.addEntry(filler)); // Leading filler, so slot 0 ends up a gap.
        // Spread whatever budget is left over the body, keeping one back for the trailing filler.
        var bodyFillers = Math.max(0, fillerBudget - 2);
        var spacing = bodyFillers == 0 ? Integer.MAX_VALUE : elementCount / (bodyFillers + 1);
        for (var i = 0; i < elementCount; i++) {
            list.addEntry(elements.get(i));
            if (fillerList.size() < fillerBudget - 1 && i > 0 && i % spacing == 0) {
                fillerList.add(list.addEntry(filler));
            }
        }
        if (fillerList.size() < fillerBudget) {
            fillerList.add(list.addEntry(filler)); // Trailing filler, so the last slot ends up a gap.
        }
        for (var entry : fillerList) {
            entry.remove();
        }
        if (list.slotCount() <= list.size()) {
            throw new IllegalStateException(
                    "The gapped fixture of (%d) elements kept no gaps (slotCount %d, size %d)."
                            .formatted(elementCount, list.slotCount(), list.size()));
        }
        return list;
    }

    static Iterator<Move<TestdataSolution>> moveIterator(MoveProvider<TestdataSolution> moveProvider,
            TestdataSolution solution) {
        return NeighborhoodTester.build(moveProvider, TestdataSolution.buildMetaModel())
                .using(solution)
                .getMovesAsIterator();
    }

    /**
     * Picks (entity, value) pairs matched by the given joiner;
     * reused for both the indexing {@code equal} and the {@code filtering()} shape.
     */
    @NullMarked
    record PickPair(PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variable,
            BiNeighborhoodsJoiner<TestdataEntity, TestdataValue> joiner) implements MoveProvider<TestdataSolution> {

        @Override
        public MoveStream<TestdataSolution> build(MoveStreamFactory<TestdataSolution> moveStreamFactory) {
            var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
            var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
            return moveStreamFactory.pick(entityStream)
                    .pick(valueStream, joiner)
                    .asMove((solutionView, entity, value) -> Moves.change(variable, entity, value));
        }

    }

}
