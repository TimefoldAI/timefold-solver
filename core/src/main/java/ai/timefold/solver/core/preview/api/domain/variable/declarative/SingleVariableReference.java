package ai.timefold.solver.core.preview.api.domain.variable.declarative;

import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface SingleVariableReference<Entity_, Value_> extends VariableReference<Entity_, Value_> {
    <Fact_> SingleVariableReference<Entity_, Fact_> fact(
            Function<Value_, Fact_> mapper, Class<? extends Fact_> factClass);

    <Variable_> SingleVariableReference<Entity_, Variable_> variable(
            String variableName, Class<? extends Variable_> variableClass);

    <Variable_> SingleVariableReference<Entity_, Variable_> intermediate(
            String intermediateName, Class<? extends Variable_> intermediateClass);

    SingleVariableReference<Entity_, Value_> previous();

    SingleVariableReference<Entity_, Value_> next();

    <Inverse_> SingleVariableReference<Entity_, Inverse_> inverse(Class<? extends Inverse_> inverseClass);

    <Element_> GroupVariableReference<Entity_, Element_> group(Function<Value_, List<Element_>> groupFunction,
            Class<? extends Element_> elementClass);

    SingleVariableReference<Entity_, Value_> allowNullValue();
}
