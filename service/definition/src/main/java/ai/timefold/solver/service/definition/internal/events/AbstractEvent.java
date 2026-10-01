package ai.timefold.solver.service.definition.internal.events;

public abstract sealed class AbstractEvent
        permits AbstractDatasetEvent, ItemCompleted, SolveStartCommand, SolveTerminateCommand,
        DatasetValidateComputeCommand, ItemTerminated {

    private final String id;

    protected AbstractEvent(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id='" + id + '\'' +
                '}';
    }

}
