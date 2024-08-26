package ai.timefold.solver.core.impl.score.stream.collector.connected_ranges;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

import org.junit.jupiter.api.Test;

public class ConnectedRangeSolvingTest {
    public record Equipment(int id, int capacity) {
    }

    @PlanningEntity
    public static class Job {
        private int id;
        private int requiredEquipmentId;
        private Integer start;

        public Job() {
        }

        public Job(int id, int requiredEquipmentId) {
            this.id = id;
            this.requiredEquipmentId = requiredEquipmentId;
        }

        @PlanningId
        public int getId() {
            return id;
        }

        public int getRequiredEquipmentId() {
            return requiredEquipmentId;
        }

        @PlanningVariable
        public Integer getStart() {
            return start;
        }

        public void setStart(Integer start) {
            this.start = start;
        }

        public Integer getEnd() {
            return start == null ? null : start + 10;
        }
    }

    @PlanningSolution
    public static class Planner {
        @PlanningScore
        private HardMediumSoftScore score;

        @ProblemFactCollectionProperty
        private final List<Equipment> equipments = new ArrayList<>();

        @PlanningEntityCollectionProperty
        private final List<Job> jobs = new ArrayList<>();

        @ValueRangeProvider
        public CountableValueRange<Integer> getStartOffsetRange() {
            return ValueRangeFactory.createIntValueRange(0, 100);
        }

        public Planner() {
        }

        public Planner(List<Equipment> equipments, List<Job> jobs) {
            this.equipments.addAll(equipments);
            this.jobs.addAll(jobs);
        }
    }

    public static class MyConstraintProvider implements ConstraintProvider {
        public MyConstraintProvider() {
        }

        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
            return new Constraint[] { doNotOverAssignEquipment(constraintFactory) };
        }

        public Constraint doNotOverAssignEquipment(ConstraintFactory constraintFactory) {
            return constraintFactory.forEach(Equipment.class)
                    .join(Job.class, Joiners.equal(Equipment::id, Job::getRequiredEquipmentId))
                    .groupBy((equipment, job) -> equipment, ConstraintCollectors.toConnectedRanges((equipment, job) -> job,
                            Job::getStart,
                            Job::getEnd,
                            (a, b) -> b - a))
                    .flattenLast(ConnectedRangeChain::getConnectedRanges)
                    .filter((equipment, connectedRange) -> connectedRange.getMaximumOverlap() > equipment.capacity())
                    .penalize(HardMediumSoftScore.ONE_HARD)
                    .asConstraint("Concurrent equipment usage over capacity");
        }
    }

    @Test
    public void solveConnectedRanges() {
        var e1 = new Equipment(1, 1);
        var j1 = new Job(1, e1.id());
        var j2 = new Job(2, e1.id());
        var problem = new Planner(List.of(e1), List.of(j1, j2));

        var config = new SolverConfig()
                .withSolutionClass(Planner.class)
                .withEntityClasses(Job.class)
                .withConstraintProviderClass(MyConstraintProvider.class)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withTerminationConfig(
                        new TerminationConfig()
                                .withScoreCalculationCountLimit(1_000L));
        var solver = SolverFactory.create(config).buildSolver();
        solver.solve(problem);
    }
}
