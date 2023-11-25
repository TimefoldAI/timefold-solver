/**
 * Includes support for deserialization of constraint collector models,
 * specifically {@link ai.timefold.solver.core.api.score.stream.common.SequenceChain},
 * {@link ai.timefold.solver.core.api.score.stream.common.Sequence}
 * and {@link ai.timefold.solver.core.api.score.stream.common.Break}.
 * These can be exposed in {@link ai.timefold.solver.core.api.score.analysis.ScoreAnalysis}
 * through user-specified {@link ai.timefold.solver.core.api.score.stream.ConstraintJustification}.
 * The serialization and deserialization of these types happens automatically,
 * if the user has registered {@link ai.timefold.solver.jackson.api.TimefoldJacksonModule}
 * with their {@link com.fasterxml.jackson.databind.ObjectMapper}.
 *
 * <p>
 * Sequences carry user-specified types of values and for this to work,
 * these values need to be serializable and deserializable.
 * The user is responsible for ensuring this,
 * as they are user types and we do not know how to serialize or deserialize them.
 * For minimal JSON output to be produced,
 * and for reference integrity to be maintained,
 * it is recommended that the user types handle their identity properly through
 * {@link com.fasterxml.jackson.annotation.JsonIdentityInfo}
 *
 * <p>
 * The implementation converts {@link ai.timefold.solver.core.api.score.stream.common.Break} instances
 * to {@link ai.timefold.solver.jackson.api.score.stream.common.SerializedBreak} instances,
 * which can then be directly serialized.
 * During deserialization,
 * the implementation first reads {@link ai.timefold.solver.jackson.api.score.stream.common.SerializedBreak} instances
 * and then converts them to {@link ai.timefold.solver.jackson.api.score.stream.common.DeserializedBreak} instances,
 * which are returned to the user.
 * It is only the latter that implements {@link ai.timefold.solver.core.api.score.stream.common.Break},
 * as otherwise an endless loop would occur during serialization.
 * We never create instances of the original Break-implementing class from solver-core,
 * as those require much more than just the pure data.
 * {@link ai.timefold.solver.core.api.score.stream.common.Break#getLength() Break length} is not available after
 * deserialization,
 * as it would require the user to specify a custom deserializer for the custom Difference_ type
 * and would have considerably complicated the entire deserialization process.
 * Since the user can easily compute the length from the endpoints,
 * we decided to avoid the overhead.
 *
 * <p>
 * For {@link ai.timefold.solver.core.api.score.stream.common.Sequence} instances
 * and for the entire {@link ai.timefold.solver.core.api.score.stream.common.SequenceChain},
 * we follow the same pattern.
 */
package ai.timefold.solver.jackson.api.score.stream.common;