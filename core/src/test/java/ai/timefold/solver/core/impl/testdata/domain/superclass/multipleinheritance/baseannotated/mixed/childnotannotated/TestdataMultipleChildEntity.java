package ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritance.baseannotated.mixed.childnotannotated;

import ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childnotannotated.TestdataChildEntity;

public class TestdataMultipleChildEntity extends TestdataChildEntity {

    public TestdataMultipleChildEntity() {
    }

    public TestdataMultipleChildEntity(long id) {
        super(id);
    }
}
