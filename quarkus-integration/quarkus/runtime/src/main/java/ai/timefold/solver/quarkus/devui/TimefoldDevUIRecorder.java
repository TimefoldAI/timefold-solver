package ai.timefold.solver.quarkus.devui;

import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class TimefoldDevUIRecorder {

    public Supplier<SolverConfigText> solverConfigTextSupplier(final Map<String, String> solverConfigs) {
        return () -> new SolverConfigText(solverConfigs);
    }

}
