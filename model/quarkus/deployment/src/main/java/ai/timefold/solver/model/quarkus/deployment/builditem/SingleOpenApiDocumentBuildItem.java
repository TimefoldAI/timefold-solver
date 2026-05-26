package ai.timefold.solver.model.quarkus.deployment.builditem;

import java.util.List;
import java.util.Objects;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.smallrye.openapi.deployment.spi.OpenApiDocumentBuildItem;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.api.SmallRyeOpenAPI;

/**
 * OpenAPI Extension emits {@link OpenApiDocumentBuildItem} as a {@link io.quarkus.builder.item.MultiBuildItem},
 * but we expect only a single OpenAPI document to exist.
 */
public final class SingleOpenApiDocumentBuildItem extends SimpleBuildItem {

    public static SingleOpenApiDocumentBuildItem fromMultiple(List<OpenApiDocumentBuildItem> openApiDocumentBuildItem) {
        Objects.requireNonNull(openApiDocumentBuildItem, "openApiDocumentBuildItem must not be null");
        if (openApiDocumentBuildItem.isEmpty()) {
            throw new IllegalStateException("openApiDocumentBuildItem must not be empty");
        }

        if (openApiDocumentBuildItem.size() > 1) {
            throw new IllegalStateException("More than one OpenApiDocument detected by OpenApiDocumentBuildItem");
        }

        return new SingleOpenApiDocumentBuildItem(openApiDocumentBuildItem.getFirst());
    }

    private final OpenApiDocument openApiDocument;
    private final SmallRyeOpenAPI smallRyeOpenAPI;

    public SingleOpenApiDocumentBuildItem(OpenApiDocument openApiDocument, SmallRyeOpenAPI smallRyeOpenAPI) {
        this.openApiDocument = openApiDocument;
        this.smallRyeOpenAPI = smallRyeOpenAPI;
    }

    private SingleOpenApiDocumentBuildItem(OpenApiDocumentBuildItem openApiDocumentBuildItem) {
        this(openApiDocumentBuildItem.getOpenApiDocument(), openApiDocumentBuildItem.getSmallRyeOpenAPI());
    }

    public OpenApiDocument getOpenApiDocument() {
        return openApiDocument;
    }

    public SmallRyeOpenAPI getSmallRyeOpenAPI() {
        return smallRyeOpenAPI;
    }
}
