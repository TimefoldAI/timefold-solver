package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var;

import java.util.List;

public interface TestdataCascadingBaseSolution<E, V> {

    List<E> getEntityList();

    List<V> getValueList();
}
