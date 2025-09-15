package ai.timefold.solver.core.impl.domain.variable.listener.support;

import ai.timefold.solver.core.impl.domain.variable.InnerVariableListener;
import ai.timefold.solver.core.impl.domain.variable.ListVariableChangeEvent;

public interface ListVariableNotification<Solution_>
        extends
        Notification<Solution_, ListVariableChangeEvent<Object, Object>, InnerVariableListener<Solution_, ListVariableChangeEvent<Object, Object>>> {

}
