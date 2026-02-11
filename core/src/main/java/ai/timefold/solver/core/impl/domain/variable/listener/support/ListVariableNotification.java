package ai.timefold.solver.core.impl.domain.variable.listener.support;

import ai.timefold.solver.core.impl.domain.variable.ListElementsChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.ListVariableListener;

public interface ListVariableNotification<Solution_>
        extends
        Notification<Solution_, ListElementsChangeEvent<Object>, ListVariableListener<Solution_, Object, Object>> {

}
