package ai.timefold.solver.core.preview.api.domain.variable.declarative;

import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface GroupVariableReference<Entity_, Value_> extends VariableReference<Entity_, List<Value_>> {
    <Fact_> GroupVariableReference<Entity_, Fact_> facts(
            Function<Value_, Fact_> mapper, Class<? extends Fact_> factClass);

    <Variable_> GroupVariableReference<Entity_, Variable_> variables(String variableName,
            Class<? extends Variable_> variableClass);

    <Variable_> GroupVariableReference<Entity_, Variable_> intermediates(
            String intermediateName, Class<? extends Variable_> intermediateClass);

    GroupVariableReference<Entity_, Value_> previous();

    GroupVariableReference<Entity_, Value_> next();

    <Inverse_> GroupVariableReference<Entity_, Inverse_> inverses(Class<? extends Inverse_> inverseClass);

    GroupVariableReference<Entity_, Value_> allowNullValues();
}
