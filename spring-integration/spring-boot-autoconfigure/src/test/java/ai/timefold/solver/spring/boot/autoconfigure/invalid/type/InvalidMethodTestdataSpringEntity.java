package ai.timefold.solver.spring.boot.autoconfigure.invalid.type;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.variable.CustomShadowVariable;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.spring.boot.autoconfigure.invalid.VariableListener;

public class InvalidMethodTestdataSpringEntity {

    private boolean pin;

    private String value;

    private List<String> values;

    private String custom;

    private int indexShadow;

    private String inverse;

    private String next;

    private String previous;

    private String shadow;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************
    @PlanningPin
    public boolean isPin() {
        return pin;
    }

    public void setPin(boolean pin) {
        this.pin = pin;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @PlanningListVariable
    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    @CustomShadowVariable
    public String getCustom() {
        return custom;
    }

    public void setCustom(String custom) {
        this.custom = custom;
    }

    @IndexShadowVariable(sourceVariableName = "source")
    public int getIndexShadow() {
        return indexShadow;
    }

    public void setIndexShadow(int indexShadow) {
        this.indexShadow = indexShadow;
    }

    @InverseRelationShadowVariable(sourceVariableName = "source")
    public String getInverse() {
        return inverse;
    }

    public void setInverse(String inverse) {
        this.inverse = inverse;
    }

    @NextElementShadowVariable(sourceVariableName = "source")
    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    @PreviousElementShadowVariable(sourceVariableName = "source")
    public String getPrevious() {
        return previous;
    }

    public void setPrevious(String previous) {
        this.previous = previous;
    }

    @ShadowVariable(sourceVariableName = "source", variableListenerClass = VariableListener.class)
    public String getShadow() {
        return shadow;
    }

    public void setShadow(String shadow) {
        this.shadow = shadow;
    }
}
