package ai.timefold.solver.core.impl.domain.variable.listener.support;

import ai.timefold.solver.core.impl.domain.variable.ChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.InnerVariableListener;

public interface EntityNotification<Solution_, ChangeEvent_ extends ChangeEvent>
        extends Notification<Solution_, ChangeEvent_, InnerVariableListener<Solution_, ChangeEvent_>> {

}
