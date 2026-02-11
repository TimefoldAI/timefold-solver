package ai.timefold.solver.core.impl.domain.variable.listener;

import java.util.Collection;
import java.util.Collections;

import ai.timefold.solver.core.impl.domain.variable.ChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.VariableListener;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

/**
 * Holds a variable listener and all its source variable descriptors.
 *
 * @param <Solution_>
 */
public final class VariableListenerWithSources<Solution_, ChangeEvent_ extends ChangeEvent> {

    private final VariableListener<Solution_, ChangeEvent_> variableListener;
    private final Collection<VariableDescriptor<Solution_>> sourceVariableDescriptors;

    public VariableListenerWithSources(
            VariableListener<Solution_, ChangeEvent_> variableListener,
            Collection<VariableDescriptor<Solution_>> sourceVariableDescriptors) {
        this.variableListener = variableListener;
        this.sourceVariableDescriptors = sourceVariableDescriptors;
    }

    public VariableListenerWithSources(
            VariableListener<Solution_, ChangeEvent_> variableListener,
            VariableDescriptor<Solution_> sourceVariableDescriptor) {
        this(variableListener, Collections.singleton(sourceVariableDescriptor));
    }

    public VariableListener<Solution_, ChangeEvent_> getVariableListener() {
        return variableListener;
    }

    public Collection<VariableDescriptor<Solution_>> getSourceVariableDescriptors() {
        return sourceVariableDescriptors;
    }

    public Collection<VariableListenerWithSources> toCollection() {
        return Collections.singleton(this);
    }
}
