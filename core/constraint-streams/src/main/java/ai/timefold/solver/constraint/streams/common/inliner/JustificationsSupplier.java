package ai.timefold.solver.constraint.streams.common.inliner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;

/**
 * Allows to create justifications and indictments lazily if and only if constraint matches are enabled.
 */
public interface JustificationsSupplier {

    static JustificationsSupplier empty() {
        return new DefaultJustificationsSupplier(DefaultConstraintJustification::of, Collections.emptyList());
    }

    static <A> JustificationsSupplier of(Constraint constraint,
            BiFunction<A, Score<?>, ConstraintJustification> justificationMapping,
            Function<A, Collection<Object>> indictedObjectsMapping,
            A a) {
        Function<Score<?>, ConstraintJustification> explainingJustificationMapping = impact -> {
            try {
                return justificationMapping.apply(a, impact);
            } catch (Exception e) {
                throw createJustificationException(constraint, e, a);
            }
        };
        try {
            return new DefaultJustificationsSupplier(explainingJustificationMapping, indictedObjectsMapping.apply(a));
        } catch (Exception e) {
            throw createIndictmentException(constraint, e, a);
        }
    }

    private static RuntimeException createJustificationException(Constraint constraint, Exception cause, Object... facts) {
        throw new IllegalStateException("Consequence of a constraint (" + constraint.getConstraintId()
                + ") threw an exception creating constraint justification from a tuple (" + factsToString(facts) + ").", cause);
    }

    private static String factsToString(Object... facts) {
        return Arrays.stream(facts)
                .map(Object::toString)
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private static RuntimeException createIndictmentException(Constraint constraint, Exception cause, Object... facts) {
        throw new IllegalStateException("Consequence of a constraint (" + constraint.getConstraintId()
                + ") threw an exception collecting indicted objects from a tuple (" + factsToString(facts) + ").", cause);
    }

    static <A, B> JustificationsSupplier of(Constraint constraint,
            TriFunction<A, B, Score<?>, ConstraintJustification> justificationMapping,
            BiFunction<A, B, Collection<Object>> indictedObjectsMapping,
            A a, B b) {
        Function<Score<?>, ConstraintJustification> explainingJustificationMapping = impact -> {
            try {
                return justificationMapping.apply(a, b, impact);
            } catch (Exception e) {
                throw createJustificationException(constraint, e, a, b);
            }
        };
        try {
            return new DefaultJustificationsSupplier(explainingJustificationMapping, indictedObjectsMapping.apply(a, b));
        } catch (Exception e) {
            throw createIndictmentException(constraint, e, a, b);
        }
    }

    static <A, B, C> JustificationsSupplier of(Constraint constraint,
            QuadFunction<A, B, C, Score<?>, ConstraintJustification> justificationMapping,
            TriFunction<A, B, C, Collection<Object>> indictedObjectsMapping,
            A a, B b, C c) {
        Function<Score<?>, ConstraintJustification> explainingJustificationMapping = impact -> {
            try {
                return justificationMapping.apply(a, b, c, impact);
            } catch (Exception e) {
                throw createJustificationException(constraint, e, a, b, c);
            }
        };
        try {
            return new DefaultJustificationsSupplier(explainingJustificationMapping, indictedObjectsMapping.apply(a, b, c));
        } catch (Exception e) {
            throw createIndictmentException(constraint, e, a, b, c);
        }
    }

    static <A, B, C, D> JustificationsSupplier of(Constraint constraint,
            PentaFunction<A, B, C, D, Score<?>, ConstraintJustification> justificationMapping,
            QuadFunction<A, B, C, D, Collection<Object>> indictedObjectsMapping,
            A a, B b, C c, D d) {
        Function<Score<?>, ConstraintJustification> explainingJustificationMapping = impact -> {
            try {
                return justificationMapping.apply(a, b, c, d, impact);
            } catch (Exception e) {
                throw createJustificationException(constraint, e, a, b, c, d);
            }
        };
        try {
            return new DefaultJustificationsSupplier(explainingJustificationMapping, indictedObjectsMapping.apply(a, b, c, d));
        } catch (Exception e) {
            throw createIndictmentException(constraint, e, a, b, c, d);
        }
    }

    ConstraintJustification createConstraintJustification(Score<?> impact);

    Collection<Object> indictedObjectCollection();

}
