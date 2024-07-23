package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var;

import java.util.List;

public interface TestdataCascadingBaseEntity<V> {

    @SuppressWarnings("rawtypes")
    void setValueList(List valueList);

    List<V> getValueList();
}
