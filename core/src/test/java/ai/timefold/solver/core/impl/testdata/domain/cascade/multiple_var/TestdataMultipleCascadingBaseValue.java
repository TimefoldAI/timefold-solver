package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var;

public interface TestdataMultipleCascadingBaseValue<E> {

    void setEntity(E entity);

    void reset();

    Integer getValue();

    Integer getCascadeValue();

    void setCascadeValue(Integer cascadeValue);

    Integer getSecondCascadeValue();

    void setSecondCascadeValue(Integer secondCascadeValue);

    int getNumberOfCalls();
}
