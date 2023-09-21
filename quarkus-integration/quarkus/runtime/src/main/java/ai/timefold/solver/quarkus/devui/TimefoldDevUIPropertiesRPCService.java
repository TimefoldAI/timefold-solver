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
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;

import io.quarkus.arc.Arc;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class TimefoldDevUIPropertiesRPCService {

    private final String effectiveSolverConfigXml;

    private TimefoldDevUIProperties devUIProperties;

    @Inject
    public TimefoldDevUIPropertiesRPCService(SolverConfigText solverConfigText) {
        this.effectiveSolverConfigXml = solverConfigText.getSolverConfigText();
    }

    @PostConstruct
    public void init() {
        if (effectiveSolverConfigXml != null) {
            // SolverConfigIO does not work at runtime,
            // but the build time SolverConfig does not have properties
            // that can be set at runtime (ex: termination), so the
            // effective solver config will be missing some properties
            devUIProperties = new TimefoldDevUIProperties(buildModelInfo(),
                    buildXmlContentWithComment("Properties that can be set at runtime are not included"),
                    buildConstraintList());
        } else {
            devUIProperties = new TimefoldDevUIProperties(buildModelInfo(),
                    "<!-- Plugin execution was skipped " + "because there are no @" + PlanningSolution.class.getSimpleName()
                            + " or @" + PlanningEntity.class.getSimpleName() + " annotated classes. -->\n<solver />",
                    Collections.emptyList());
        }
    }

    public JsonObject getConfig() {
        JsonObject out = new JsonObject();
        out.put("config", devUIProperties.getEffectiveSolverConfig());
        return out;
    }

    public JsonArray getConstraints() {
        return JsonArray.of(devUIProperties.getConstraintList().toArray());
    }

    public JsonObject getModelInfo() {
        TimefoldModelProperties modelProperties = devUIProperties.getTimefoldModelProperties();
        JsonObject out = new JsonObject();
        out.put("solutionClass", modelProperties.solutionClass);
        out.put("entityClassList", JsonArray.of(modelProperties.entityClassList.toArray()));
        out.put("entityClassToGenuineVariableListMap",
                new JsonObject(modelProperties.entityClassToGenuineVariableListMap.entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> JsonArray.of(entry.getValue().toArray())))));
        out.put("entityClassToShadowVariableListMap",
                new JsonObject(modelProperties.entityClassToShadowVariableListMap.entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> JsonArray.of(entry.getValue().toArray())))));
        return out;
    }

    private TimefoldModelProperties buildModelInfo() {
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

    private List<String> buildConstraintList() {
        if (effectiveSolverConfigXml != null) {
            DefaultSolverFactory<?> solverFactory =
                    (DefaultSolverFactory<?>) Arc.container().instance(SolverFactory.class).get();
            if (solverFactory.getScoreDirectorFactory() instanceof AbstractConstraintStreamScoreDirectorFactory) {
                AbstractConstraintStreamScoreDirectorFactory<?, ?> scoreDirectorFactory =
                        (AbstractConstraintStreamScoreDirectorFactory<?, ?>) solverFactory.getScoreDirectorFactory();
                return Arrays.stream(scoreDirectorFactory.getConstraints()).map(Constraint::getConstraintId)
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private String buildXmlContentWithComment(String comment) {
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
