package ai.timefold.solver.quarkus.devui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.constraint.streams.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;
import ai.timefold.solver.quarkus.config.TimefoldRuntimeConfig;

import io.quarkus.arc.Arc;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class TimefoldDevUIPropertiesRPCService {

    private final Map<String, String> effectiveSolverConfig;

    private final Map<String, TimefoldDevUIProperties> devUIProperties;

    @Inject
    public TimefoldDevUIPropertiesRPCService(SolverConfigText solverConfigText) {
        this.effectiveSolverConfig = solverConfigText.getSolverConfigurations();
        this.devUIProperties = new HashMap<>();
    }

    @PostConstruct
    public void init() {
        if (effectiveSolverConfig != null && !effectiveSolverConfig.isEmpty()) {
            // SolverConfigIO does not work at runtime,
            // but the build time SolverConfig does not have properties
            // that can be set at runtime (ex: termination), so the
            // effective solver config will be missing some properties
            this.effectiveSolverConfig.forEach((key, config) -> this.devUIProperties.put(key,
                    new TimefoldDevUIProperties(buildModelInfo(config),
                            buildXmlContentWithComment(config, "Properties that can be set at runtime are not included"),
                            buildConstraintList(config))));
        } else {
            devUIProperties.put(TimefoldRuntimeConfig.DEFAULT_SOLVER_NAME, new TimefoldDevUIProperties(
                    buildModelInfo(null),
                    "<!-- Plugin execution was skipped " + "because there are no @" + PlanningSolution.class.getSimpleName()
                            + " or @" + PlanningEntity.class.getSimpleName() + " annotated classes. -->\n<solver />",
                    Collections.emptyList()));
        }
    }

    public JsonObject getConfig() {
        JsonObject out = new JsonObject();
        JsonObject config = new JsonObject();
        out.put("config", config);
        devUIProperties.forEach((key, value) -> config.put(key, value.getEffectiveSolverConfig()));
        return out;
    }

    public JsonObject getConstraints() {
        JsonObject out = new JsonObject();
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

    private TimefoldModelProperties buildModelInfo(String effectiveSolverConfigXml) {
        if (effectiveSolverConfigXml != null) {
            DefaultSolverFactory<?> solverFactory =
                    (DefaultSolverFactory<?>) Arc.container().instance(SolverFactory.class).get();
            SolutionDescriptor<?> solutionDescriptor = solverFactory.getScoreDirectorFactory().getSolutionDescriptor();
            TimefoldModelProperties out = new TimefoldModelProperties();
            out.setSolutionClass(solutionDescriptor.getSolutionClass().getName());
            List<String> entityClassList = new ArrayList<>();
            Map<String, List<String>> entityClassToGenuineVariableListMap = new HashMap<>();
            Map<String, List<String>> entityClassToShadowVariableListMap = new HashMap<>();
            for (EntityDescriptor<?> entityDescriptor : solutionDescriptor.getEntityDescriptors()) {
                entityClassList.add(entityDescriptor.getEntityClass().getName());
                List<String> entityClassToGenuineVariableList = new ArrayList<>();
                List<String> entityClassToShadowVariableList = new ArrayList<>();
                for (VariableDescriptor<?> variableDescriptor : entityDescriptor.getDeclaredVariableDescriptors()) {
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

    private List<ConstraintRef> buildConstraintList(String effectiveSolverConfigXml) {
        if (effectiveSolverConfigXml != null) {
            DefaultSolverFactory<?> solverFactory =
                    (DefaultSolverFactory<?>) Arc.container().instance(SolverFactory.class).get();
            if (solverFactory.getScoreDirectorFactory() instanceof AbstractConstraintStreamScoreDirectorFactory) {
                AbstractConstraintStreamScoreDirectorFactory<?, ?> scoreDirectorFactory =
                        (AbstractConstraintStreamScoreDirectorFactory<?, ?>) solverFactory.getScoreDirectorFactory();
                return Arrays.stream(scoreDirectorFactory.getConstraints())
                        .map(Constraint::getConstraintRef)
                        .toList();
            }
        }
        return Collections.emptyList();
    }

    private String buildXmlContentWithComment(String effectiveSolverConfigXml, String comment) {
        int indexOfPreambleEnd = effectiveSolverConfigXml.indexOf("?>");
        if (indexOfPreambleEnd != -1) {
            return effectiveSolverConfigXml.substring(0, indexOfPreambleEnd + 2) +
                    "\n<!--" + comment + "-->\n"
                    + effectiveSolverConfigXml.substring(indexOfPreambleEnd + 2);
        } else {
            return "<!--" + comment + "-->\n" + effectiveSolverConfigXml;
        }
    }
}
