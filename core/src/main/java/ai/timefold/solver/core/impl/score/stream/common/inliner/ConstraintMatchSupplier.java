package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;

/**
 * Allows creating {@link ConstraintMatch} instances lazily if and only if they are required by the end user.
 * <p>
 * Lazy behavior is important for constraint matching performance.
 * In order to create {@link ConstraintMatch}, an entire {@link ConstraintJustification} object needs to be created,
 * along with the collection of indicted objects.
 * Creating these structures every time a constraint is matched would be wasteful,
 * as the same constraint match can be undone almost immediately, resulting in a lot of pointless garbage.
 * Therefore, {@link ConstraintMatch} (along with all of its supporting data structures)
 * is only created when actually needed, and that is during score explanation.
 * Until that point, this thin wrapper serves as a placeholder which understands what to create when needed.
 */
@FunctionalInterface
public interface ConstraintMatchSupplier<Score_ extends Score<Score_>>
        extends BiFunction<Constraint, Score_, ConstraintMatch<Score_>> {

    static <Score_ extends Score<Score_>> ConstraintMatchSupplier<Score_> empty() {
        return (constraint, impact) -> new ConstraintMatch<>(constraint.getConstraintRef(),
                DefaultConstraintJustification.of(impact),
                Collections.emptyList(), impact);
    }

    static <A, Score_ extends Score<Score_>> ConstraintMatchSupplier<Score_> of(
            BiFunction<A, Score<?>, ConstraintJustification> justificationMapping,
            Function<A, Collection<Object>> indictedObjectsMapping,
            A a) {
        return (constraint, impact) -> {
            ConstraintJustification justification;
            try {
                justification = justificationMapping.apply(a, impact);
            } catch (Exception e) {
                throw createJustificationException(constraint, e, a);
            }
            Collection<Object> indictedObjectCollection;
            try {
                indictedObjectCollection = indictedObjectsMapping.apply(a);
            } catch (Exception e) {
                throw createIndictmentException(constraint, e, a);
            }
            return new ConstraintMatch<>(constraint.getConstraintRef(), justification, indictedObjectCollection, impact);
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

    private static RuntimeException createIndictmentException(Constraint constraint, Exception cause, Object... facts) {
        throw new IllegalStateException("Consequence of a constraint (" + constraint.getConstraintRef()
                + ") threw an exception collecting indicted objects from a tuple (" + factsToString(facts) + ").", cause);
    }

    static <A, B, Score_ extends Score<Score_>> ConstraintMatchSupplier<Score_> of(
            TriFunction<A, B, Score_, ConstraintJustification> justificationMapping,
            BiFunction<A, B, Collection<Object>> indictedObjectsMapping,
            A a, B b) {
        return (constraint, impact) -> {
            ConstraintJustification justification;
            try {
                justification = justificationMapping.apply(a, b, impact);
            } catch (Exception e) {
                throw createJustificationException(constraint, e, a, b);
            }
            Collection<Object> indictedObjectCollection;
            try {
                indictedObjectCollection = indictedObjectsMapping.apply(a, b);
            } catch (Exception e) {
                throw createIndictmentException(constraint, e, a, b);
            }
            return new ConstraintMatch<>(constraint.getConstraintRef(), justification, indictedObjectCollection, impact);
        };
    }

    static <A, B, C, Score_ extends Score<Score_>> ConstraintMatchSupplier<Score_> of(
            QuadFunction<A, B, C, Score_, ConstraintJustification> justificationMapping,
            TriFunction<A, B, C, Collection<Object>> indictedObjectsMapping,
            A a, B b, C c) {
        return (constraint, impact) -> {
            ConstraintJustification justification;
            try {
                justification = justificationMapping.apply(a, b, c, impact);
            } catch (Exception e) {
                throw createJustificationException(constraint, e, a, b, c);
            }
            Collection<Object> indictedObjectCollection;
            try {
                indictedObjectCollection = indictedObjectsMapping.apply(a, b, c);
            } catch (Exception e) {
                throw createIndictmentException(constraint, e, a, b, c);
            }
            return new ConstraintMatch<>(constraint.getConstraintRef(), justification, indictedObjectCollection, impact);
        };
    }

    static <A, B, C, D, Score_ extends Score<Score_>> ConstraintMatchSupplier<Score_> of(
            PentaFunction<A, B, C, D, Score_, ConstraintJustification> justificationMapping,
            QuadFunction<A, B, C, D, Collection<Object>> indictedObjectsMapping,
            A a, B b, C c, D d) {
        return (constraint, impact) -> {
            ConstraintJustification justification;
            try {
                justification = justificationMapping.apply(a, b, c, d, impact);
            } catch (Exception e) {
                throw createJustificationException(constraint, e, a, b, c, d);
            }
            Collection<Object> indictedObjectCollection;
            try {
                indictedObjectCollection = indictedObjectsMapping.apply(a, b, c, d);
            } catch (Exception e) {
                throw createIndictmentException(constraint, e, a, b, c, d);
            }
            return new ConstraintMatch<>(constraint.getConstraintRef(), justification, indictedObjectCollection, impact);
        };
    }

}
