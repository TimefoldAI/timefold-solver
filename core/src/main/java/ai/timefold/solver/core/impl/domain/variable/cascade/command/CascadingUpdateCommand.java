package ai.timefold.solver.core.impl.domain.variable.cascade.command;

public interface CascadingUpdateCommand<T> {
    T getValue(Object value);
}
