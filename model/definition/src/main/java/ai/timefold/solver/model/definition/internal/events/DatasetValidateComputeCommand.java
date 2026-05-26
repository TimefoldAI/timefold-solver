package ai.timefold.solver.model.definition.internal.events;

public final class DatasetValidateComputeCommand extends AbstractEvent {

    private boolean solve;

    public DatasetValidateComputeCommand(String id, boolean solve) {
        super(id);
        this.solve = solve;
    }

    public boolean solve() {
        return solve;
    }
}
