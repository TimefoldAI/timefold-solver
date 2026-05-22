package ai.timefold.solver.model.quarkus.deployment.rest;

import java.util.Map;
import java.util.function.Consumer;

import ai.timefold.solver.model.definition.internal.events.SolverChannels;
import ai.timefold.solver.model.quarkus.deployment.builditem.ModelComponentsBuildItem;

import org.jboss.jandex.DotName;

import io.quarkus.gizmo.AnnotatedElement;
import io.quarkus.gizmo.AnnotationCreator;
import io.quarkus.gizmo.Type.ParameterizedType;

public interface ModelResourceTypeInfo {

    String EMITTER_CLASS_NAME = "org.eclipse.microprofile.reactive.messaging.Emitter";

    String MUTINY_EMITTER_CLASS_NAME = "io.smallrye.reactive.messaging.MutinyEmitter";

    DotName WAYPOINT_SERVICE = DotName.createSimple("ai.timefold.solver.model.maps.service.integration.impl.WaypointsService");

    DotName SCORE_ANALYSIS_FACADE_BASE =
            DotName.createSimple("ai.timefold.solver.enterprise.model.definition.api.analysis.ScoreAnalysisFacadeBase");

    String CHANNEL_CLASS_NAME = "org.eclipse.microprofile.reactive.messaging.Channel";

    String BROADCAST_ANNOTATION_CLASS_NAME = "io.smallrye.reactive.messaging.annotations.Broadcast";

    DotName modelResourceSuperClassName();

    ParameterizedType modelResourceSuperClassType(ModelComponentsBuildItem modelComponents);

    String constructorSignature(ModelComponentsBuildItem modelComponents);

    String[] constructorParameterTypes(ModelComponentsBuildItem modelComponents);

    Map<String, Integer> channelConstructorParameterIndices();

    default Map<String, Consumer<AnnotatedElement>> channelConstructorParameterAnnotators() {
        return Map.of(
                SolverChannels.DATASET_CREATED, (AnnotatedElement parameter) -> {
                    AnnotationCreator channelAnnotation = parameter.addAnnotation(CHANNEL_CLASS_NAME);
                    channelAnnotation.add("value", SolverChannels.DATASET_CREATED);
                    parameter.addAnnotation(BROADCAST_ANNOTATION_CLASS_NAME);
                },
                SolverChannels.DATASET_VALIDATE_COMPUTE, (AnnotatedElement parameter) -> {
                    AnnotationCreator channelAnnotation = parameter.addAnnotation(CHANNEL_CLASS_NAME);
                    channelAnnotation.add("value", SolverChannels.DATASET_VALIDATE_COMPUTE);
                    parameter.addAnnotation(BROADCAST_ANNOTATION_CLASS_NAME);
                },
                SolverChannels.START, (AnnotatedElement parameter) -> {
                    AnnotationCreator channelAnnotation = parameter.addAnnotation(CHANNEL_CLASS_NAME);
                    channelAnnotation.add("value", SolverChannels.START);
                },
                SolverChannels.TERMINATE, (AnnotatedElement parameter) -> {
                    AnnotationCreator channelAnnotation = parameter.addAnnotation(CHANNEL_CLASS_NAME);
                    channelAnnotation.add("value", SolverChannels.TERMINATE);
                });
    }

}
