package ai.timefold.solver.core.preview.api.domain.variable.declarative;

import java.util.function.BiFunction;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ShadowCalculationBuilderFactory<Entity_> {
    <Value_, A> ShadowCalculationBuilder<Entity_, Value_> computeIfHasAll(
            VariableReference<Entity_, A> a,
            BiFunction<Entity_, A, Value_> function);

    <Value_, A, B> ShadowCalculationBuilder<Entity_, Value_> computeIfHasAll(
            VariableReference<Entity_, A> a,
            VariableReference<Entity_, B> b,
            TriFunction<Entity_, A, B, Value_> function);

    <Value_, A, B, C> ShadowCalculationBuilder<Entity_, Value_> computeIfHasAll(
            VariableReference<Entity_, A> a,
            VariableReference<Entity_, B> b,
            VariableReference<Entity_, C> c,
            QuadFunction<Entity_, A, B, C, Value_> function);

    <Value_, A, B, C, D> ShadowCalculationBuilder<Entity_, Value_> computeIfHasAll(
            VariableReference<Entity_, A> a,
            VariableReference<Entity_, B> b,
            VariableReference<Entity_, C> c,
            VariableReference<Entity_, D> d,
            PentaFunction<Entity_, A, B, C, D, Value_> function);
}
