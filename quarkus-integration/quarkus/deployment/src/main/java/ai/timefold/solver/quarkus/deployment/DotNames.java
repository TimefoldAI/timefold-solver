package ai.timefold.solver.quarkus.deployment;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.inject.Named;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.ProblemFactProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;

import org.jboss.jandex.DotName;

public final class DotNames {
    // Jakarta classes
    static final DotName NAMED = DotName.createSimple(Named.class);

    // Timefold classes
    static final DotName PLANNING_SOLUTION = DotName.createSimple(PlanningSolution.class.getName());
    static final DotName PLANNING_ENTITY_COLLECTION_PROPERTY =
            DotName.createSimple(PlanningEntityCollectionProperty.class.getName());
    static final DotName PLANNING_ENTITY_PROPERTY = DotName.createSimple(PlanningEntityProperty.class.getName());
    static final DotName PLANNING_SCORE = DotName.createSimple(PlanningScore.class.getName());
    static final DotName PROBLEM_FACT_COLLECTION_PROPERTY = DotName.createSimple(ProblemFactCollectionProperty.class.getName());
    static final DotName PROBLEM_FACT_PROPERTY = DotName.createSimple(ProblemFactProperty.class.getName());

    static final DotName EASY_SCORE_CALCULATOR = DotName.createSimple(EasyScoreCalculator.class.getName());
    static final DotName CONSTRAINT_PROVIDER = DotName.createSimple(ConstraintProvider.class.getName());
    static final DotName INCREMENTAL_SCORE_CALCULATOR =
            DotName.createSimple(IncrementalScoreCalculator.class.getName());
    static final DotName CONSTRAINT_WEIGHT_OVERRIDES = DotName.createSimple(ConstraintWeightOverrides.class.getName());

    static final DotName PLANNING_ENTITY = DotName.createSimple(PlanningEntity.class.getName());
    static final DotName PLANNING_PIN = DotName.createSimple(PlanningPin.class.getName());
    static final DotName PLANNING_PIN_TO_INDEX = DotName.createSimple(PlanningPinToIndex.class.getName());
    static final DotName PLANNING_ID = DotName.createSimple(PlanningId.class.getName());

    static final DotName PLANNING_VARIABLE = DotName.createSimple(PlanningVariable.class.getName());
    static final DotName PLANNING_LIST_VARIABLE = DotName.createSimple(PlanningListVariable.class.getName());
    static final DotName VALUE_RANGE_PROVIDER = DotName.createSimple(ValueRangeProvider.class.getName());

    static final DotName INDEX_SHADOW_VARIABLE = DotName.createSimple(IndexShadowVariable.class.getName());
    static final DotName INVERSE_RELATION_SHADOW_VARIABLE = DotName.createSimple(InverseRelationShadowVariable.class.getName());
    static final DotName NEXT_ELEMENT_SHADOW_VARIABLE = DotName.createSimple(NextElementShadowVariable.class.getName());
    static final DotName PREVIOUS_ELEMENT_SHADOW_VARIABLE = DotName.createSimple(PreviousElementShadowVariable.class.getName());
    static final DotName SHADOW_VARIABLE = DotName.createSimple(ShadowVariable.class.getName());
    static final DotName SHADOW_VARIABLES_INCONSISTENT = DotName.createSimple(ShadowVariablesInconsistent.class.getName());
    static final DotName CASCADING_UPDATE_SHADOW_VARIABLE =
            DotName.createSimple(CascadingUpdateShadowVariable.class.getName());
    static final DotName SHADOW_SOURCES = DotName.createSimple(ShadowSources.class.getName());

    static final DotName SOLVER_CONFIG = DotName.createSimple(SolverConfig.class.getName());
    static final DotName SOLVER_MANAGER_CONFIG = DotName.createSimple(SolverManagerConfig.class.getName());
    static final DotName SOLVER_FACTORY = DotName.createSimple(SolverFactory.class.getName());
    static final DotName SOLVER_MANAGER = DotName.createSimple(SolverManager.class.getName());
    static final DotName CONSTRAINT_VERIFIER = DotName.createSimple(ConstraintVerifier.class.getName());

    static final DotName[] PLANNING_ENTITY_FIELD_ANNOTATIONS = {
            PLANNING_PIN,
            PLANNING_PIN_TO_INDEX,
            PLANNING_VARIABLE,
            PLANNING_LIST_VARIABLE,
            INDEX_SHADOW_VARIABLE,
            INVERSE_RELATION_SHADOW_VARIABLE,
            NEXT_ELEMENT_SHADOW_VARIABLE,
            PREVIOUS_ELEMENT_SHADOW_VARIABLE,
            SHADOW_VARIABLE,
            SHADOW_VARIABLES_INCONSISTENT,
            CASCADING_UPDATE_SHADOW_VARIABLE
    };

    static final DotName[] GIZMO_MEMBER_ACCESSOR_ANNOTATIONS = {
            PLANNING_ENTITY_COLLECTION_PROPERTY,
            PLANNING_ENTITY_PROPERTY,
            PLANNING_SCORE,
            PROBLEM_FACT_COLLECTION_PROPERTY,
            PROBLEM_FACT_PROPERTY,
            PLANNING_PIN,
            PLANNING_PIN_TO_INDEX,
            PLANNING_ID,
            PLANNING_VARIABLE,
            PLANNING_LIST_VARIABLE,
            VALUE_RANGE_PROVIDER,
            INDEX_SHADOW_VARIABLE,
            INVERSE_RELATION_SHADOW_VARIABLE,
            NEXT_ELEMENT_SHADOW_VARIABLE,
            PREVIOUS_ELEMENT_SHADOW_VARIABLE,
            SHADOW_VARIABLE,
            SHADOW_VARIABLES_INCONSISTENT,
            CASCADING_UPDATE_SHADOW_VARIABLE
    };

    static final Set<DotName> SOLVER_INJECTABLE_TYPES = Set.of(
            SOLVER_CONFIG,
            SOLVER_MANAGER_CONFIG,
            SOLVER_FACTORY,
            SOLVER_MANAGER);

    public enum BeanDefiningAnnotations {
        PLANNING_SCORE(DotNames.PLANNING_SCORE, PlanningScore.class),
        PLANNING_SOLUTION(DotNames.PLANNING_SOLUTION, PlanningSolution.class),
        PLANNING_ENTITY(DotNames.PLANNING_ENTITY, PlanningEntity.class),
        PLANNING_VARIABLE(DotNames.PLANNING_VARIABLE, PlanningVariable.class),
        PLANNING_LIST_VARIABLE(DotNames.PLANNING_LIST_VARIABLE, PlanningListVariable.class),
        SHADOW_VARIABLE(DotNames.SHADOW_VARIABLE, ShadowVariable.class);

        private final DotName annotationDotName;
        private final List<String> beanDefiningMethodNames;

        BeanDefiningAnnotations(DotName annotationDotName, Class<? extends Annotation> annotationClass) {
            this.annotationDotName = annotationDotName;
            this.beanDefiningMethodNames = new ArrayList<>();
            for (var method : annotationClass.getMethods()) {
                if (method.getReturnType().equals(Class.class)) {
                    beanDefiningMethodNames.add(method.getName());
                }
            }
        }

        public DotName getAnnotationDotName() {
            return annotationDotName;
        }

        public List<String> getBeanDefiningMethodNames() {
            return beanDefiningMethodNames;
        }
    }

    private DotNames() {
    }

}
