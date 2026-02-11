package ai.timefold.solver.core.impl.domain.variable.listener.support;

import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.BasicVariableListener;

public interface BasicVariableNotification<Solution_> extends
        Notification<Solution_, BasicVariableChangeEvent<Object>, BasicVariableListener<Solution_, Object>> {

}
