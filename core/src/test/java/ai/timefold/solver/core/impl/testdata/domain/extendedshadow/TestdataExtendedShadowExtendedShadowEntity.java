package ai.timefold.solver.core.impl.testdata.domain.extendedshadow;

public class TestdataExtendedShadowExtendedShadowEntity extends TestdataExtendedShadowShadowEntity {

    public TestdataExtendedShadowExtendedShadowEntity() {
        super();
    }

    public TestdataExtendedShadowExtendedShadowEntity(TestdataExtendedShadowEntity myPlanningEntity) {
        super();
        this.planningEntityList.add(myPlanningEntity);
    }

}
