package ai.timefold.solver.core.impl.domain.variable.listener;

import ai.timefold.solver.core.impl.domain.variable.ChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.VariableListener;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface SourcedVariableListener<Solution_, Event_ extends ChangeEvent>
        extends VariableListener<Solution_, Event_>
        permits SourcedBasicVariableListener, SourcedListVariableListener {
    VariableDescriptor<Solution_> getSourceVariableDescriptor();
}
