package ai.timefold.solver.core.config.phase;

import java.util.function.Consumer;

import ai.timefold.solver.core.impl.phase.NoChangePhase;

import org.jspecify.annotations.NonNull;

/**
 * @deprecated Deprecated on account of deprecating {@link NoChangePhase}.
 */
@Deprecated(forRemoval = true, since = "1.20.0")
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
