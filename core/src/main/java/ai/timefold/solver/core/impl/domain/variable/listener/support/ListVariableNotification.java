package ai.timefold.solver.core.impl.domain.variable.listener.support;

import ai.timefold.solver.core.impl.domain.variable.InnerListVariableListener;
import ai.timefold.solver.core.impl.domain.variable.ListElementsChangeEvent;

public interface ListVariableNotification<Solution_>
        extends
        Notification<Solution_, ListElementsChangeEvent<Object>, InnerListVariableListener<Solution_, Object, Object>> {

}
