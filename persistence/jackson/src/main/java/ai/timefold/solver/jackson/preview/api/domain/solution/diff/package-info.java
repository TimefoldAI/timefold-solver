/**
 * Includes support for deserialization of
 * {@link ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningSolutionDiff}.
 * The serialization happens automatically,
 * if the user has registered {@link ai.timefold.solver.jackson.api.TimefoldJacksonModule}
 * with their {@link com.fasterxml.jackson.databind.ObjectMapper}.
 *
 * <p>
 * Deserialization is not implemented, on account of losing the information about the type of the solution,
 * its entities and values.
 */
package ai.timefold.solver.jackson.preview.api.domain.solution.diff;