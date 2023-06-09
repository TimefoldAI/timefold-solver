package ai.timefold.solver.quarkus.devui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

public class TimefoldDevUIPropertiesSupplier implements Supplier<TimefoldDevUIProperties> {
    private String effectiveSolverConfigXml;

    public TimefoldDevUIPropertiesSupplier() {
        this.effectiveSolverConfigXml = null;
    }

    public TimefoldDevUIPropertiesSupplier(String effectiveSolverConfigXml) {
        this.effectiveSolverConfigXml = effectiveSolverConfigXml;
    }

    // Needed for Quarkus Dev UI serialization
    public String getEffectiveSolverConfigXml() {
        return effectiveSolverConfigXml;
    }

    public void setEffectiveSolverConfigXml(String effectiveSolverConfigXml) {
        this.effectiveSolverConfigXml = effectiveSolverConfigXml;
    }

    @Override
    public TimefoldDevUIProperties get() {
        if (effectiveSolverConfigXml != null) {
            // SolverConfigIO does not work at runtime,
            // but the build time SolverConfig does not have properties
            // that can be set at runtime (ex: termination), so the
            // effective solver config will be missing some properties
            return new TimefoldDevUIProperties(getModelInfo(),
                    getXmlContentWithComment("Properties that can be set at runtime are not included"),
                    getConstraintList());
        } else {
            return new TimefoldDevUIProperties(getModelInfo(),
                    "<!-- Plugin execution was skipped " + "because there are no @" + PlanningSolution.class.getSimpleName()
                            + " or @" + PlanningEntity.class.getSimpleName() + " annotated classes. -->\n<solver />",
                    Collections.emptyList());
        }
    }

    private TimefoldModelProperties getModelInfo() {
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

    private List<String> getConstraintList() {
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

    private String getXmlContentWithComment(String comment) {
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
