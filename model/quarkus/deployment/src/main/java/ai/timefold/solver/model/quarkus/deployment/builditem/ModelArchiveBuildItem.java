package ai.timefold.solver.model.quarkus.deployment.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ModelArchiveBuildItem extends SimpleBuildItem {

    private Path archiveContentPath;

    public ModelArchiveBuildItem(Path archiveContentPath) {
        this.archiveContentPath = archiveContentPath;
    }

    public Path getArchiveContentPath() {
        return archiveContentPath;
    }

}
