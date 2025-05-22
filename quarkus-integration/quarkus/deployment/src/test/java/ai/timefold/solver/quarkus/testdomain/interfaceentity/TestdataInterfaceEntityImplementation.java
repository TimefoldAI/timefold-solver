package ai.timefold.solver.quarkus.testdomain.interfaceentity;

public class TestdataInterfaceEntityImplementation implements TestdataInterfaceEntity {

    Integer value;

    public TestdataInterfaceEntityImplementation() {
    }

    public TestdataInterfaceEntityImplementation(Integer value) {
        this.value = value;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public void setValue(Integer value) {
        this.value = value;
    }
}
