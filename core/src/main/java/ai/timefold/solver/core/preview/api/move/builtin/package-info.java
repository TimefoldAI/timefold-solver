/**
 * Contains the built-in {@link ai.timefold.solver.core.preview.api.neighborhood.MoveProvider} implementations.
 * They are expected to be directly used and are not designed for extensibility.
 * Users can and should implement custom move providers more efficient than these,
 * on account of their knowledge of the problem domain.
 * There is a price we pay for generality,
 * and specialization can lead to better performance.
 *
 * <p>
 * <strong>Do not use these implementations as a template for your own move providers.</strong>
 * They are generic, so they handle every case that a model can have:
 * value ranges on the solution or on an entity, pinned entities and values, unassigned values, and more.
 * This makes them complex.
 * A move provider written for your domain handles only the cases that your domain has;
 * it is usually much simpler, and it often has little in common with these.
 *
 * <p>
 * New instances of generic moves should be obtained via {@link Moves}.
 */
package ai.timefold.solver.core.preview.api.move.builtin;
