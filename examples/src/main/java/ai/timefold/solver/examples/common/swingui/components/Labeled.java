package ai.timefold.solver.examples.common.swingui.components;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @see LabeledComboBoxRenderer
 */
public interface Labeled {

    @JsonIgnore
    String getLabel();

}
