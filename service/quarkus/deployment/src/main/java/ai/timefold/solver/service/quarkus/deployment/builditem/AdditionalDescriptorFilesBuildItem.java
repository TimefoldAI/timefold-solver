package ai.timefold.solver.service.quarkus.deployment.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * {@link io.quarkus.builder.BuildStep build steps} can produce this item to inform about additional files added
 * to the model descriptor directory structure during build.
 */
public final class AdditionalDescriptorFilesBuildItem extends MultiBuildItem {

    private Path[] additionalPaths;

    public AdditionalDescriptorFilesBuildItem(Path... additionalPaths) {
        this.additionalPaths = additionalPaths;
    }

    public Path[] getAdditionalPaths() {
        return additionalPaths;
    }
}
