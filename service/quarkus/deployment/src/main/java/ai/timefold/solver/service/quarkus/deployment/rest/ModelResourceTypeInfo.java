package ai.timefold.solver.service.quarkus.deployment.rest;

import ai.timefold.solver.service.quarkus.deployment.builditem.ModelComponentsBuildItem;

import org.jboss.jandex.DotName;

import io.quarkus.gizmo.Type.ParameterizedType;

public interface ModelResourceTypeInfo {

    DotName WAYPOINT_SERVICE =
            DotName.createSimple("ai.timefold.solver.service.maps.service.integration.impl.WaypointsService");

    DotName SCORE_ANALYSIS_FACADE_BASE =
            DotName.createSimple("ai.timefold.solver.enterprise.service.definition.api.analysis.ScoreAnalysisFacadeBase");

    DotName modelResourceSuperClassName();

    ParameterizedType modelResourceSuperClassType(ModelComponentsBuildItem modelComponents);

    String constructorSignature(ModelComponentsBuildItem modelComponents);

    String[] constructorParameterTypes(ModelComponentsBuildItem modelComponents);

}
