/**
 * Contains the built-in {@link Move} implementations,
 * and their associated {@link ai.timefold.solver.core.preview.api.neighborhood.MoveProvider} implementations.
 * Both are expected to be directly used,
 * neither are designed for extensibility.
 * Users can and should implement custom move providers more efficient than these,
 * on account of their knowledge of the problem domain.
 * There is a price we pay for generality,
 * and specialization can lead to better performance.
 *
 * <p>
 * New instances of generic moves should be obtained via {@link Moves}.
 * Moves are designed for performance;
 * for that reason, they do not re-assert validity of inputs,
 * and will corrupt your solution if used incorrectly.
 * Check Javadoc of each move for the contract you are required to follow.
 */
package ai.timefold.solver.core.preview.api.move.builtin;

import ai.timefold.solver.core.preview.api.move.Move;