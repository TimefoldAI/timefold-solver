package ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.interfaces.childnot;

public class TestdataMultipleChildNotAnnotatedInterfaceChildEntity
        implements TestdataMultipleChildNotAnnotatedInterfaceSecondEntity {

    private Long id;
    private String value;

    public TestdataMultipleChildNotAnnotatedInterfaceChildEntity() {
    }

    public TestdataMultipleChildNotAnnotatedInterfaceChildEntity(long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public void someMethod() {
        // Do nothing
    }
}
