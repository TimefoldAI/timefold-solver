package ai.timefold.solver.examples.machinereassignment.domain;

import ai.timefold.solver.examples.common.domain.AbstractPersistable;
import ai.timefold.solver.examples.common.persistence.jackson.JacksonUniqueIdGenerator;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;

@JsonIdentityInfo(generator = JacksonUniqueIdGenerator.class)
public class MrResource extends AbstractPersistable {

    private int index;
    private boolean transientlyConsumed;
    private int loadCostWeight;

    @SuppressWarnings("unused")
    MrResource() {
    }

    public MrResource(long id) {
        super(id);
    }

    public MrResource(int index, boolean transientlyConsumed, int loadCostWeight) {
        this.index = index;
        this.transientlyConsumed = transientlyConsumed;
        this.loadCostWeight = loadCostWeight;
    }

    public MrResource(long id, int index, boolean transientlyConsumed, int loadCostWeight) {
        super(id);
        this.index = index;
        this.transientlyConsumed = transientlyConsumed;
        this.loadCostWeight = loadCostWeight;
    }

    public int getIndex() {
        return index;
    }

    public boolean isTransientlyConsumed() {
        return transientlyConsumed;
    }

    public int getLoadCostWeight() {
        return loadCostWeight;
    }

}
