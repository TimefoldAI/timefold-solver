package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.childnot;

public class TestdataChildNotAnnotatedInterfaceChildEntity implements TestdataChildNotAnnotatedInterfaceBaseEntity {

    private Long id;
    private String value;

    @SuppressWarnings("unused")
    public TestdataChildNotAnnotatedInterfaceChildEntity() {
    }

    public TestdataChildNotAnnotatedInterfaceChildEntity(long id) {
        this.id = id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
