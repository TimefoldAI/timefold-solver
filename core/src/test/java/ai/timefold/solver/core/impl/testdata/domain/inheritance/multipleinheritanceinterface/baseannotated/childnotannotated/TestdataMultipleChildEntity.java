package ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritanceinterface.baseannotated.childnotannotated;

public class TestdataMultipleChildEntity implements TestdataMultipleBaseEntity {

    private Long id;
    private String value;

    public TestdataMultipleChildEntity() {
    }

    public TestdataMultipleChildEntity(long id) {
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
