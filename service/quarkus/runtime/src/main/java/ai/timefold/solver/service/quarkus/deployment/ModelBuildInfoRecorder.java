package ai.timefold.solver.service.quarkus.deployment;

import java.util.function.Supplier;

import ai.timefold.solver.service.definition.internal.descriptor.ModelBuildInfo;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ModelBuildInfoRecorder {

    public Supplier<ModelBuildInfo> create(ModelBuildInfo modelBuildInfo) {
        return () -> modelBuildInfo;
    }

}
