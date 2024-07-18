package ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration.extended;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfiguration;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintWeight;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration.TestdataConstraintConfiguration;

@Deprecated(forRemoval = true, since = "1.13.0")
@ConstraintConfiguration
public class TestdataExtendedConstraintConfiguration extends TestdataConstraintConfiguration {

    private SimpleScore thirdWeight = SimpleScore.of(300);

    public TestdataExtendedConstraintConfiguration() {
        super();
    }

    public TestdataExtendedConstraintConfiguration(String code) {
        super(code);
    }

    @ConstraintWeight("Third weight")
    public SimpleScore getThirdWeight() {
        return thirdWeight;
    }

    public void setThirdWeight(SimpleScore thirdWeight) {
        this.thirdWeight = thirdWeight;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
