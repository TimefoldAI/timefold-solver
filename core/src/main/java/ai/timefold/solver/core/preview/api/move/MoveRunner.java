package ai.timefold.solver.core.preview.api.move;

import java.util.Objects;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.move.DefaultMoveRunner;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

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
 *     var runner = MoveRunner.build(SolutionClass.class, EntityClass.class);
 *     var context = runner.using(solution);
 *
 *     // Permanent execution
 *     context.execute(move);
 *
 *     // Temporary execution with automatic undo
 *     context.executeTemporarily(move, view -> {
 *         assertThat(view.getValue(...)).isEqualTo(expected);
 *     });
 * }</pre>
 * <p>
 * <strong>This type is part of the Preview API which is under development.</strong>
 * There are no guarantees for backward compatibility; any class, method, or field may change
 * or be removed without prior notice, although we will strive to avoid this as much as possible.
 * Migration support will be provided via OpenRewrite recipes when breaking changes occur.
 *
 * @param <Solution_> the planning solution type
 */
@NullMarked
public interface MoveRunner<Solution_> {

    /**
     * Creates a new MoveRunner for the given solution and entity classes.
     * <p>
     * This method validates inputs, constructs the internal solution descriptor,
     * and creates a score director factory from Constraint Streams with a dummy constraint.
     * These are heavy operations performed once and cached for reuse.
     * <p>
     * Shadow variables are initialized later when a solution is bound via {@link #using(Object)}.
     *
     * @param solutionClass the planning solution class; must not be null
     * @param entityClasses the planning entity classes; must not be empty
     * @param <Solution_> the planning solution type
     * @return a new MoveRunner instance
     */
    static <Solution_> MoveRunner<Solution_> build(Class<Solution_> solutionClass, Class<?>... entityClasses) {
        if (Objects.requireNonNull(entityClasses, "entityClasses").length == 0) {
            throw new IllegalArgumentException("entityClasses must not be empty");
        }
        var solutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(
                Objects.requireNonNull(solutionClass, "solutionClass"), entityClasses);
        return new DefaultMoveRunner<>(solutionDescriptor);
    }

    /**
     * As defined by {@link #build(Class, Class[])}, but using the meta-model to extract classes.
     */
    static <Solution_> MoveRunner<Solution_> build(PlanningSolutionMetaModel<Solution_> solutionMetaModel) {
        return build(solutionMetaModel.type(),
                solutionMetaModel.entities().stream()
                        .map(PlanningEntityMetaModel::type)
                        .toArray(Class<?>[]::new));
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
     */
    MoveRunContext<Solution_> using(Solution_ solution);

}
