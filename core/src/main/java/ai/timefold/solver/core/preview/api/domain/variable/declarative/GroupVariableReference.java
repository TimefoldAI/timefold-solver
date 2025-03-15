package ai.timefold.solver.core.preview.api.domain.variable.declarative;

import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface GroupVariableReference<Entity_, Value_> extends VariableReference<Entity_, List<Value_>> {
    <Fact_> GroupVariableReference<Entity_, Fact_> facts(
            Class<? extends Fact_> factClass,
            Function<Value_, Fact_> mapper);

    <Variable_> GroupVariableReference<Entity_, Variable_> variables(Class<? extends Variable_> variableClass,
            String variableName);

    <Variable_> GroupVariableReference<Entity_, Variable_> intermediates(
            Class<? extends Variable_> intermediateClass,
            String intermediateName);

    GroupVariableReference<Entity_, Value_> previous();

    GroupVariableReference<Entity_, Value_> next();

    <Inverse_> GroupVariableReference<Entity_, Inverse_> inverses(Class<? extends Inverse_> inverseClass);

    GroupVariableReference<Entity_, Value_> allowNullValues();
}
