package ai.timefold.solver.service.definition.internal.events;

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
