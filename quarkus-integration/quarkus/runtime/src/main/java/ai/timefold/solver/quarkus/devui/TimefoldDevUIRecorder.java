package ai.timefold.solver.quarkus.devui;

import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class TimefoldDevUIRecorder {

    public Supplier<SolverConfigText> solverConfigTextSupplier(final String solverConfigText) {
        return () -> {
            return new SolverConfigText(solverConfigText);
        };
    }

}
