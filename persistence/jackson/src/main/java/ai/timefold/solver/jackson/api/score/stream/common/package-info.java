/**
 * Includes support for deserialization of constraint collector models,
 * specifically {@link ai.timefold.solver.core.api.score.stream.common.SequenceChain},
 * {@link ai.timefold.solver.core.api.score.stream.common.Sequence},
 * {@link ai.timefold.solver.core.api.score.stream.common.Break}
 * and {@link ai.timefold.solver.core.api.score.stream.common.LoadBalance}.
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
 * The implementation converts {@link ai.timefold.solver.core.api.score.stream.common.Sequence} instances
 * to {@link ai.timefold.solver.jackson.api.score.stream.common.SerializableSequence} instances,
 * which can then be directly serialized.
 * During deserialization,
 * the implementation first reads {@link ai.timefold.solver.jackson.api.score.stream.common.SerializableSequence} instances
 * and then converts them to {@link ai.timefold.solver.jackson.api.score.stream.common.DeserializableSequence} instances,
 * which are returned to the user.
 * It is only the latter that implements {@link ai.timefold.solver.core.api.score.stream.common.Sequence},
 * as otherwise an endless loop would occur during serialization.
 * We never create instances of the original Sequence-implementing class from solver-core,
 * as those require much more than just the pure data.
 *
 * <p>
 * For {@link ai.timefold.solver.core.api.score.stream.common.Sequence} instances
 * and for the entire {@link ai.timefold.solver.core.api.score.stream.common.SequenceChain},
 * we follow the same pattern.
 * {@link ai.timefold.solver.core.api.score.stream.common.Break} instances are only serialized
 * when {@link ai.timefold.solver.core.api.score.stream.common.Sequence} is being serialized directly.
 * When {@link ai.timefold.solver.core.api.score.stream.common.SequenceChain} is being serialized,
 * {@link ai.timefold.solver.core.api.score.stream.common.Break} instances between sequences are not included,
 * but the JSON has been designed in such a way that this can later be added in a backwards compatible manner.
 *
 * <p>
 * {@link ai.timefold.solver.core.api.score.stream.common.Break#getLength() Break length} is not available after
 * deserialization,
 * as it would require the user to specify a custom deserializer for the custom Difference_ type
 * and would have considerably complicated the entire deserialization process.
 * Since the user can easily compute the length from the endpoints,
 * we decided to avoid the overhead.
 * The same deserialization treatment is applied to {@link ai.timefold.solver.core.api.score.stream.common.SequenceChain}.
 */
package ai.timefold.solver.jackson.api.score.stream.common;