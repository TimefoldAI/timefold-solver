package ai.timefold.solver.core.preview.api.domain.variable.declarative;

import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface SingleVariableReference<Entity_, Value_> extends VariableReference<Entity_, Value_> {
    <Fact_> SingleVariableReference<Entity_, Fact_> fact(
            Class<? extends Fact_> factClass,
            Function<Value_, Fact_> mapper);

    <Variable_> SingleVariableReference<Entity_, Variable_> variable(
            Class<? extends Variable_> variableClass,
            String variableName);

    <Variable_> SingleVariableReference<Entity_, Variable_> intermediate(
            Class<? extends Variable_> intermediateClass,
            String intermediateName);

    SingleVariableReference<Entity_, Value_> previous();

    SingleVariableReference<Entity_, Value_> next();

    <Inverse_> SingleVariableReference<Entity_, Inverse_> inverse(Class<? extends Inverse_> inverseClass);

    <Element_> GroupVariableReference<Entity_, Element_> group(Class<? extends Element_> element,
            Function<Value_, List<Element_>> groupFunction);

    SingleVariableReference<Entity_, Value_> allowNullValue();
}
