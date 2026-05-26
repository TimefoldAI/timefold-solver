package ai.timefold.solver.model.quarkus.deployment;

import java.util.function.Supplier;

import ai.timefold.solver.model.definition.internal.descriptor.ModelBuildInfo;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ModelBuildInfoRecorder {

    public Supplier<ModelBuildInfo> create(ModelBuildInfo modelBuildInfo) {
        return () -> modelBuildInfo;
    }

}
