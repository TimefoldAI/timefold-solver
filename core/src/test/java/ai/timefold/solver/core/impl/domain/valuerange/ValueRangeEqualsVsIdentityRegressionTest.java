package ai.timefold.solver.core.impl.domain.valuerange;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.score.director.DefaultScoreDirector;
import ai.timefold.solver.core.impl.solver.DefaultSolver;
import ai.timefold.solver.core.impl.solver.Solver;
import org.junit.jupiter.api.Test;

/**
 * Regression test for issue #2172.
 * 
 * <p>This test verifies that ValueRangeCache uses {@code .equals()} for containment checks,
 * not object identity ({@code ==}). This is critical for JPA entities that implement
 * {@code equals()} by ID field, where different instances with the same ID represent
 * the same logical entity.</p>
 * 
 * <p>The bug manifested when:</p>
 * <ul>
 *   <li>Using filtering value range providers</li>
 *   <li>With JPA entities that have {@code equals()} by ID</li>
 *   <li>Entities are persisted in one transaction and loaded in another (different instances)</li>
 * </ul>
 * 
 * <p>Before PR #2111, {@code ValueRangeCache.Builder.FOR_USER_VALUES} used
 * {@code IdentityHashSet} ({@code ==}) which rejected equal-but-not-identical values,
 * causing {@code IllegalStateException} at solver startup.</p>
 * 
 * @see <a href="https://github.com/TimefoldAI/timefold-solver/issues/2172">issue #2172</a>
 * @see <a href="https://github.com/TimefoldAI/timefold-solver/pull/2111">PR #2111</a>
 */
class ValueRangeEqualsVsIdentityRegressionTest {

    /**
     * Simulates a JPA entity where {@code equals()} compares ID fields.
     * Different instances with the same ID are considered equal.
     */
    static class JpaLikeEntity {
        private final Long id;
        private final String name;

        JpaLikeEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof JpaLikeEntity)) return false;
            return id != null && id.equals(((JpaLikeEntity) o).id);
        }

        @Override
        public int hashCode() {
            return id == null ? 31 : id.hashCode();
        }

        @Override
        public String toString() {
            return "Entity[" + id + "]";
        }
    }

    /**
     * Planning entity with a planning variable that uses a JPA-like value.
     */
    @PlanningEntity
    static class TestEntity {
        private final Long id;
        private JpaLikeEntity value;
        private boolean pinned = false;

        TestEntity(Long id, JpaLikeEntity value) {
            this.id = id;
            this.value = value;
        }

        @PlanningId
        public Long getId() {
            return id;
        }

        public JpaLikeEntity getValue() {
            return value;
        }

        public void setValue(JpaLikeEntity value) {
            this.value = value;
        }

        public boolean isPinned() {
            return pinned;
        }

        public void setPinned(boolean pinned) {
            this.pinned = pinned;
        }
    }

    /**
     * Planning solution with a filtering value range provider.
     */
    @PlanningSolution
    static class TestSolution {
        private List<TestEntity> entities;
        private List<JpaLikeEntity> valueRange;

        TestSolution(List<TestEntity> entities, List<JpaLikeEntity> valueRange) {
            this.entities = entities;
            this.valueRange = valueRange;
        }

        @ValueRangeProvider(id = "valueRange")
        public List<JpaLikeEntity> getValueRange() {
            return valueRange;
        }

        @PlanningEntityCollectionProperty
        public List<TestEntity> getEntities() {
            return entities;
        }

        public Integer getScore() {
            return 0;
        }

        public void setScore(Integer score) {
        }
    }

    @Test
    void valueRangeCacheUsesEqualsNotIdentity() {
        // Create canonical value range
        List<JpaLikeEntity> canonicalRange = IntStream.range(1, 4)
                .mapToObj(i -> new JpaLikeEntity((long) i, "Value" + i))
                .collect(Collectors.toList());

        // Create entity with a DIFFERENT JpaLikeEntity instance (same ID, different object)
        // This simulates what happens with JPA: persisted with one instance, loaded with another
        JpaLikeEntity assignedValue = new JpaLikeEntity(2L, "Value2"); // Different instance, same ID
        TestEntity entity = new TestEntity(1L, assignedValue);
        entity.setPinned(true);

        // Create solution with canonical range and entity with "different" value
        TestSolution solution = new TestSolution(List.of(entity), canonicalRange);

        // Configure solver
        SolverConfig<TestSolution> config = new SolverConfig<>()
                .withSolutionClass(TestSolution.class)
                .withEntityClasses(TestEntity.class);

        // Build solver
        Solver<TestSolution> solver = new DefaultSolver<>(config.buildSolver());

        // Before PR #2111, this would throw:
        // IllegalStateException: The value (Entity[2]) has been assigned to the entity (TestEntity[1]),
        // but it is outside of the related value range [Entity[1], Entity[2], Entity[3]]
        // because IdentityHashSet.contains() uses == instead of .equals()
        
        // After PR #2111, this should succeed because HashSet.contains() uses .equals()
        assertThatCode(() -> solver.solve(solution))
                .doesNotThrowAnyException();
    }

    @Test
    void valueRangeCacheWithMultipleEntitiesSameIdDifferentInstances() {
        // Create canonical value range
        List<JpaLikeEntity> canonicalRange = IntStream.range(1, 6)
                .mapToObj(i -> new JpaLikeEntity((long) i, "Value" + i))
                .collect(Collectors.toList());

        // Create multiple entities, each with a DIFFERENT instance of the same value
        // Simulates multiple JPA entities loaded in different transactions
        List<TestEntity> entities = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            JpaLikeEntity value = new JpaLikeEntity((long) i, "Value" + i); // Different instance
            entities.add(new TestEntity((long) i, value));
        }

        TestSolution solution = new TestSolution(entities, canonicalRange);

        SolverConfig<TestSolution> config = new SolverConfig<>()
                .withSolutionClass(TestSolution.class)
                .withEntityClasses(TestEntity.class);

        Solver<TestSolution> solver = new DefaultSolver<>(config.buildSolver());

        // Should succeed because .equals() is used, not ==
        assertThatCode(() -> solver.solve(solution))
                .doesNotThrowAnyException();
    }
}
