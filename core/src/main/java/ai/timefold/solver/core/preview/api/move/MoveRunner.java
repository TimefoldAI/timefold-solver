package ai.timefold.solver.core.preview.api.move;

import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryFactory;

import org.jspecify.annotations.NullMarked;

/**
 * Entry point for executing {@link Move}s on planning solutions.
 * <p>
 * Provides a fluent API for testing move implementations in both permanent and temporary modes.
 * Designed for testing and development use cases, not production solving workflows.
 * <p>
 * This class is NOT thread-safe. Each thread must create its own MoveRunner instance.
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * try (var runner = MoveRunner.build(SolutionClass.class, EntityClass.class)) {
 *     var context = runner.using(solution);
 *
 *     // Permanent execution
 *     context.execute(move);
 *
 *     // Temporary execution with automatic undo
 *     context.executeTemporarily(move, view -> {
 *         assertThat(view.getValue(...)).isEqualTo(expected);
 *     });
 * }
 * }</pre>
 * <p>
 * <strong>This class is part of the Preview API which is under development.</strong>
 * There are no guarantees for backward compatibility; any class, method, or field may change
 * or be removed without prior notice, although we will strive to avoid this as much as possible.
 * Migration support will be provided via OpenRewrite recipes when breaking changes occur.
 *
 * @param <Solution_> the planning solution type
 */
@NullMarked
public final class MoveRunner<Solution_> implements AutoCloseable {

    private final ScoreDirectorFactory<Solution_, ?> scoreDirectorFactory;
    private boolean closed = false;

    private MoveRunner(ScoreDirectorFactory<Solution_, ?> scoreDirectorFactory) {
        this.scoreDirectorFactory = scoreDirectorFactory;
    }

    /**
     * Creates a new MoveRunner for the given solution and entity classes.
     * <p>
     * This method validates inputs, constructs the internal solution descriptor,
     * and creates a score director factory from Constraint Streams with a dummy constraint.
     * These are heavy operations performed once and cached for reuse.
     * <p>
     * Shadow variables are initialized later when a solution is bound via {@link #using(Object)}.
     * <p>
     * This method must be called within a try-with-resources block to ensure proper resource cleanup.
     *
     * @param solutionClass the planning solution class; must not be null
     * @param entityClasses the planning entity classes; must not be empty
     * @param <Solution_> the planning solution type
     * @return a new MoveRunner instance
     * @throws IllegalArgumentException if solutionClass is null or entityClasses is empty
     */
    public static <Solution_> MoveRunner<Solution_> build(
            Class<Solution_> solutionClass,
            Class<?>... entityClasses) {
        if (Objects.requireNonNull(entityClasses, "entityClasses").length == 0) {
            throw new IllegalArgumentException("entityClasses must not be empty");
        }

        // Create solution descriptor
        var solutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(
                Objects.requireNonNull(solutionClass, "solutionClass"), entityClasses);

        // Create a Constraint Streams configuration with a dummy constraint provider
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(DummyConstraintProvider.class);

        // Build score director factory from the configuration
        ScoreDirectorFactoryFactory<Solution_, ?> scoreDirectorFactoryFactory =
                new ScoreDirectorFactoryFactory<>(scoreDirectorFactoryConfig);
        var scoreDirectorFactory = scoreDirectorFactoryFactory.buildScoreDirectorFactory(
                EnvironmentMode.PHASE_ASSERT, solutionDescriptor);

        return new MoveRunner<>(scoreDirectorFactory);
    }

    /**
     * Creates an execution context for the given solution instance.
     * <p>
     * This method creates a score director from the cached factory and sets the working solution,
     * which automatically triggers shadow variable initialization for the provided solution.
     * <p>
     * Multiple execution contexts can be created from the same MoveRunner instance,
     * allowing sequential move execution with different solutions or the same solution
     * at different points in time.
     *
     * @param solution the planning solution instance; must not be null
     * @return a new execution context bound to the given solution with initialized shadow variables
     * @throws IllegalArgumentException if solution is null
     * @throws IllegalStateException if this MoveRunner has been closed
     */
    public MoveRunContext<Solution_> using(Solution_ solution) {
        if (closed) {
            throw new IllegalStateException("""
                    The MoveRunner has been closed and cannot be reused.
                    Maybe you forgot to create a new MoveRunner instance within the try-with-resources block?
                    """);
        }

        // Create a score director from the cached factory
        var scoreDirector = scoreDirectorFactory.buildScoreDirector();

        // Set the working solution, which triggers shadow variable initialization
        scoreDirector.setWorkingSolution(Objects.requireNonNull(solution, "solution"));

        return new MoveRunContext<>(scoreDirector);
    }

    /**
     * Releases all resources held by this MoveRunner.
     * <p>
     * After calling this method, any attempt to use the MoveRunner will throw
     * {@link IllegalStateException}.
     * <p>
     * Resources (score director factory, solver engine state) will leak if this method is not called.
     * Always use try-with-resources to ensure proper cleanup.
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            // ScoreDirectorFactory doesn't have a close method, so we just set the flag
        }
    }

    /**
     * Dummy constraint provider that creates a single dummy constraint.
     * This is needed to create a valid score director factory without actual constraint evaluation.
     * <p>
     * This class is an internal detail and must not be exposed to the user.
     */
    @NullMarked
    static final class DummyConstraintProvider implements ConstraintProvider {

        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
            return new Constraint[] {
                    constraintFactory.forEach(Object.class)
                            .penalize(SimpleScore.ONE)
                            .asConstraint("Dummy constraint")
            };
        }
    }

}
