package ai.timefold.solver.core.preview.api.variable.provided;

import java.util.List;
import java.util.function.Function;

public interface SingleVariableReference<Entity_, Value_> extends VariableReference<Entity_, Value_> {
    <Fact_> SingleVariableReference<Entity_, Fact_> fact(
            Class<? extends Fact_> factClass,
            Function<Value_, Fact_> mapper);

    <Variable_> SingleVariableReference<Entity_, Variable_> variable(
            Class<? extends Variable_> variableClass,
            String variableName);

    SingleVariableReference<Entity_, Value_> previous();

    SingleVariableReference<Entity_, Value_> next();

    <Inverse_> SingleVariableReference<Entity_, Inverse_> inverse(Class<? extends Inverse_> inverseClass);

    <Element_> GroupVariableReference<Entity_, Element_> group(Class<? extends Element_> element,
            Function<Value_, List<Element_>> groupFunction);
}
