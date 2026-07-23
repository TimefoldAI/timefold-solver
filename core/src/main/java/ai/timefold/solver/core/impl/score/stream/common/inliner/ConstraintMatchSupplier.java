package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.indictment.IndictmentSource;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Allows creating {@link ConstraintMatch} instances lazily if and only if they are required by the end user.
 * <p>
 * Lazy behavior is important for constraint matching performance.
 * In order to create {@link ConstraintMatch}, an entire {@link ConstraintJustification} object needs to be created.
 * Creating this structure every time a constraint is matched would be wasteful,
 * as the same constraint match can be undone almost immediately, resulting in a lot of pointless garbage.
 * Therefore, {@link ConstraintMatch} (along with all of its supporting data structures)
 * is only created when actually needed, and that is during score explanation.
 * Until that point, this thin wrapper serves as a placeholder which understands what to create when needed.
 */
@NullMarked
@FunctionalInterface
public interface ConstraintMatchSupplier<Score_ extends Score<Score_>>
        extends BiFunction<Constraint, Score_, ConstraintMatch<Score_>> {

    static @Nullable List<Object> collectIndictments(Constraint constraint, Tuple tuple) {
        if (tuple.getIndictmentSource() == IndictmentSource.DISABLED) {
            return null;
        }
        var out = new LinkedHashSet<>();
        var abstractConstraint = (AbstractConstraint<?, ?, ?>) constraint;
        var involvedNodeIds = Objects.requireNonNull(abstractConstraint.getInvolvedNodeIds());
        tuple.getIndictmentSource().visitSources(involvedNodeIds, out::add);
        return new ArrayList<>(out);
    }

    /**
     * 
     * @return the constraint match returned by the supplier will have its justification set to null.
     *         This is useful when the justifications are disabled, to save memory.
     * @param <Score_>
     */
    static <Score_ extends Score<Score_>> ConstraintMatchSupplier<Score_> empty() {
        return (constraint, impact) -> new ConstraintMatch<>(constraint.getConstraintRef(), null,
                Collections.emptyList(), impact);
    }

    static <A, Score_ extends Score<Score_>> ConstraintMatchSupplier<Score_> of(
            BiFunction<A, Score<?>, ConstraintJustification> justificationMapping,
            UniTuple<A> tuple) {
        return (constraint, impact) -> {
            try {
                var justification = justificationMapping.apply(tuple.getA(), impact);
                return new ConstraintMatch<>(constraint.getConstraintRef(), justification,
                        collectIndictments(constraint, tuple), impact);
            } catch (Exception e) {
                throw createJustificationException(constraint, e, tuple.getA());
            }
        };
    }

    private static RuntimeException createJustificationException(Constraint constraint, Exception cause, Object... facts) {
        throw new IllegalStateException("Consequence of a constraint (" + constraint.getConstraintRef()
                + ") threw an exception creating constraint justification from a tuple (" + factsToString(facts) + ").", cause);
    }

    private static String factsToString(Object... facts) {
        return Arrays.stream(facts)
                .map(Objects::toString)
                .collect(Collectors.joining(", ", "{", "}"));
    }

    static <A, B, Score_ extends Score<Score_>> ConstraintMatchSupplier<Score_> of(
            TriFunction<A, B, Score_, ConstraintJustification> justificationMapping,
            BiTuple<A, B> tuple) {
        return (constraint, impact) -> {
            try {
                var justification = justificationMapping.apply(tuple.getA(), tuple.getB(), impact);
                return new ConstraintMatch<>(constraint.getConstraintRef(), justification,
                        collectIndictments(constraint, tuple), impact);
            } catch (Exception e) {
                throw createJustificationException(constraint, e, tuple.getA(), tuple.getB());
            }
        };
    }

    static <A, B, C, Score_ extends Score<Score_>> ConstraintMatchSupplier<Score_> of(
            QuadFunction<A, B, C, Score_, ConstraintJustification> justificationMapping,
            TriTuple<A, B, C> tuple) {
        return (constraint, impact) -> {
            try {
                var justification = justificationMapping.apply(tuple.getA(), tuple.getB(), tuple.getC(), impact);
                return new ConstraintMatch<>(constraint.getConstraintRef(), justification,
                        collectIndictments(constraint, tuple), impact);
            } catch (Exception e) {
                throw createJustificationException(constraint, e, tuple.getA(), tuple.getB(), tuple.getC());
            }
        };
    }

    static <A, B, C, D, Score_ extends Score<Score_>> ConstraintMatchSupplier<Score_> of(
            PentaFunction<A, B, C, D, Score_, ConstraintJustification> justificationMapping,
            QuadTuple<A, B, C, D> tuple) {
        return (constraint, impact) -> {
            try {
                var justification = justificationMapping.apply(tuple.getA(), tuple.getB(), tuple.getC(), tuple.getD(), impact);
                return new ConstraintMatch<>(constraint.getConstraintRef(), justification,
                        collectIndictments(constraint, tuple), impact);
            } catch (Exception e) {
                throw createJustificationException(constraint, e, tuple.getA(), tuple.getB(), tuple.getC(), tuple.getD());
            }
        };
    }

}
