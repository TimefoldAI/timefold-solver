package ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childnotannotated;

import ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childnotannotated.TestdataChildEntity;

public class TestdataMultipleChildEntity extends TestdataChildEntity {

    public TestdataMultipleChildEntity() {
    }

    public TestdataMultipleChildEntity(long id) {
        super(id);
    }
}
