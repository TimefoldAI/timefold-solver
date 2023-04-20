package ai.timefold.solver.examples.nurserostering.domain.contract;

import ai.timefold.solver.examples.common.domain.AbstractPersistable;
import ai.timefold.solver.examples.nurserostering.domain.pattern.Pattern;

public class PatternContractLine extends AbstractPersistable {

    private Contract contract;
    private Pattern pattern;

    public PatternContractLine() {
    }

    public PatternContractLine(long id) {
        super(id);
    }

    public PatternContractLine(long id, Contract contract, Pattern pattern) {
        super(id);
        this.contract = contract;
        this.pattern = pattern;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public String toString() {
        return contract + "-" + pattern;
    }

}
