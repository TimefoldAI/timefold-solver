/**
 * Contains the built-in {@link ai.timefold.solver.core.preview.api.neighborhood.MoveProvider} implementations.
 * They are expected to be directly used and are not designed for extensibility.
 * Users can and should implement custom move providers more efficient than these,
 * on account of their knowledge of the problem domain.
 * There is a price we pay for generality,
 * and specialization can lead to better performance.
 *
 * <p>
 * New instances of generic moves should be obtained via {@link Moves}.
 */
package ai.timefold.solver.core.preview.api.move.builtin;
