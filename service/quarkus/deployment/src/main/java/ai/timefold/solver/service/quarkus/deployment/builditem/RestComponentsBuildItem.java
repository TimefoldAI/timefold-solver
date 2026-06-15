package ai.timefold.solver.service.quarkus.deployment.builditem;

import java.util.Optional;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.SimpleBuildItem;

public final class RestComponentsBuildItem extends SimpleBuildItem {

    private final Optional<ClassInfo> restResource;

    public RestComponentsBuildItem(Optional<ClassInfo> restResource) {
        this.restResource = restResource;
    }

    public Optional<ClassInfo> getRestResource() {
        return restResource;
    }
}
