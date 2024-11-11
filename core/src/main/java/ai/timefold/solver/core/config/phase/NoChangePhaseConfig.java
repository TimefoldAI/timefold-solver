package ai.timefold.solver.core.config.phase;

import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;

public class NoChangePhaseConfig extends PhaseConfig<NoChangePhaseConfig> {

    public static final String XML_ELEMENT_NAME = "noChangePhase";

    @Override
    public @NonNull NoChangePhaseConfig inherit(@NonNull NoChangePhaseConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        return this;
    }

    @Override
    public @NonNull NoChangePhaseConfig copyConfig() {
        return new NoChangePhaseConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        if (terminationConfig != null) {
            terminationConfig.visitReferencedClasses(classVisitor);
        }
    }

}
