package ai.timefold.solver.quarkus.devui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;
import ai.timefold.solver.quarkus.config.TimefoldRuntimeConfig;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class TimefoldDevUIPropertiesRPCService {

    private final DevUISolverConfig devUISolverConfig;

    private final Map<String, TimefoldDevUIProperties> devUIProperties;

    @Inject
    public TimefoldDevUIPropertiesRPCService(DevUISolverConfig devUISolverConfig) {
        this.devUISolverConfig = devUISolverConfig;
        this.devUIProperties = new HashMap<>();
    }

    @PostConstruct
    public void init() {
        if (devUISolverConfig != null && !devUISolverConfig.isEmpty()) {
            // SolverConfigIO does not work at runtime,
            // but the build time SolverConfig does not have properties
            // that can be set at runtime (ex: termination), so the
            // effective solver config will be missing some properties
            this.devUISolverConfig.getSolverNames().forEach(key -> this.devUIProperties.put(key,
                    new TimefoldDevUIProperties(buildModelInfo(devUISolverConfig.getFactory(key)),
                            buildXmlContentWithComment(devUISolverConfig.getSolverConfigFile(key),
                                    "Properties that can be set at runtime are not included"),
                            buildConstraintList(devUISolverConfig.getFactory(key)))));
        } else {
            devUIProperties.put(TimefoldRuntimeConfig.DEFAULT_SOLVER_NAME, new TimefoldDevUIProperties(
                    buildModelInfo(null),
                    "<!-- Plugin execution was skipped " + "because there are no @" + PlanningSolution.class.getSimpleName()
                            + " or @" + PlanningEntity.class.getSimpleName() + " annotated classes. -->\n<solver />",
                    Collections.emptyList()));
        }
    }

    public JsonObject getConfig() {
        var out = new JsonObject();
        var config = new JsonObject();
        out.put("config", config);
        devUIProperties.forEach((key, value) -> config.put(key, value.getEffectiveSolverConfig()));
        return out;
    }

    public JsonObject getConstraints() {
        var out = new JsonObject();
        devUIProperties.forEach((key, value) -> out.put(key, JsonArray.of(value.getConstraintList()
                .stream()
                .map(ConstraintRef::constraintId)
                .toArray())));
        return out;
    }

    public JsonObject getModelInfo() {
        JsonObject out = new JsonObject();
        devUIProperties.forEach((key, value) -> {
            JsonObject property = new JsonObject();
            TimefoldModelProperties modelProperties = value.getTimefoldModelProperties();
            property.put("solutionClass", modelProperties.solutionClass);
            property.put("entityClassList", JsonArray.of(modelProperties.entityClassList.toArray()));
            property.put("entityClassToGenuineVariableListMap",
                    new JsonObject(modelProperties.entityClassToGenuineVariableListMap.entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> JsonArray.of(entry.getValue().toArray())))));
            property.put("entityClassToShadowVariableListMap",
                    new JsonObject(modelProperties.entityClassToShadowVariableListMap.entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> JsonArray.of(entry.getValue().toArray())))));
            out.put(key, property);
        });
        return out;
    }

    private TimefoldModelProperties buildModelInfo(SolverFactory<?> solverFactory) {
        if (solverFactory != null) {
            var solutionDescriptor =
                    ((DefaultSolverFactory<?>) solverFactory).getScoreDirectorFactory().getSolutionDescriptor();
            var out = new TimefoldModelProperties();
            out.setSolutionClass(solutionDescriptor.getSolutionClass().getName());
            var entityClassList = new ArrayList<String>();
            var entityClassToGenuineVariableListMap = new HashMap<String, List<String>>();
            var entityClassToShadowVariableListMap = new HashMap<String, List<String>>();
            for (var entityDescriptor : solutionDescriptor.getEntityDescriptors()) {
                entityClassList.add(entityDescriptor.getEntityClass().getName());
                var entityClassToGenuineVariableList = new ArrayList<String>();
                var entityClassToShadowVariableList = new ArrayList<String>();
                for (var variableDescriptor : entityDescriptor.getDeclaredVariableDescriptors()) {
                    if (variableDescriptor instanceof GenuineVariableDescriptor) {
                        entityClassToGenuineVariableList.add(variableDescriptor.getVariableName());
                    } else {
                        entityClassToShadowVariableList.add(variableDescriptor.getVariableName());
                    }
                }
                entityClassToGenuineVariableListMap.put(entityDescriptor.getEntityClass().getName(),
                        entityClassToGenuineVariableList);
                entityClassToShadowVariableListMap.put(entityDescriptor.getEntityClass().getName(),
                        entityClassToShadowVariableList);
            }
            out.setEntityClassList(entityClassList);
            out.setEntityClassToGenuineVariableListMap(entityClassToGenuineVariableListMap);
            out.setEntityClassToShadowVariableListMap(entityClassToShadowVariableListMap);
            return out;
        } else {
            return new TimefoldModelProperties();
        }
    }

    private List<ConstraintRef> buildConstraintList(SolverFactory<?> solverFactory) {
        if (solverFactory != null) {
            var scoreDirectorFactory = ((DefaultSolverFactory<?>) solverFactory).getScoreDirectorFactory();
            if (scoreDirectorFactory instanceof AbstractConstraintStreamScoreDirectorFactory<?, ?> castScoreDirectorFactory) {
                return castScoreDirectorFactory.getConstraintMetaModel().getConstraints()
                        .stream()
                        .map(Constraint::getConstraintRef)
                        .toList();
            }
        }
        return Collections.emptyList();
    }

    private String buildXmlContentWithComment(String effectiveSolverConfigXml, String comment) {
        var indexOfPreambleEnd = effectiveSolverConfigXml.indexOf("?>");
        if (indexOfPreambleEnd != -1) {
            return effectiveSolverConfigXml.substring(0, indexOfPreambleEnd + 2) +
                    "\n<!--" + comment + "-->\n"
                    + effectiveSolverConfigXml.substring(indexOfPreambleEnd + 2);
        } else {
            return "<!--" + comment + "-->\n" + effectiveSolverConfigXml;
        }
    }
}
