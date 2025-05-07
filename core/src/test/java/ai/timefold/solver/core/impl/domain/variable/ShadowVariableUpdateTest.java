package ai.timefold.solver.core.impl.domain.variable;

import static ai.timefold.solver.core.impl.domain.variable.listener.support.ShadowVariableType.BASIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import java.util.List;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.testdomain.declarative.basic.TestdataBasicVarEntity;
import ai.timefold.solver.core.testdomain.declarative.basic.TestdataBasicVarSolution;
import ai.timefold.solver.core.testdomain.declarative.basic.TestdataBasicVarValue;
import ai.timefold.solver.core.testdomain.declarative.chained.TestdataChainedVarEntity;
import ai.timefold.solver.core.testdomain.declarative.chained.TestdataChainedVarSolution;
import ai.timefold.solver.core.testdomain.declarative.chained.TestdataChainedVarValue;
import ai.timefold.solver.core.testdomain.shadow.full.TestdataShadowedFullEntity;
import ai.timefold.solver.core.testdomain.shadow.full.TestdataShadowedFullSolution;
import ai.timefold.solver.core.testdomain.shadow.full.TestdataShadowedFullValue;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ShadowVariableUpdateTest {

    @Test
    void emptyEntities() {
        Assertions
                .assertThatCode(() -> SolutionManager.updateShadowVariables(TestdataShadowedFullSolution.class, new Object[0]))
                .hasMessageContaining("The entity array cannot be empty.");
    }

    @Test
    void invalidCustomListener() {
        var value = new TestdataShadowedFullValue("v1");
        var entity = new TestdataShadowedFullEntity("e1");
        var solution = new TestdataShadowedFullSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(value));
        Assertions
                .assertThatCode(
                        () -> SolutionManager.updateShadowVariables(TestdataShadowedFullSolution.class, entity, value))
                .hasMessageContaining("Custom shadow variable descriptors are not supported");
    }

    @Test
    void invalidInverseRelation() {
        var value1 = new TestdataBasicVarValue("c1", Duration.ZERO);
        var entity1 = new TestdataBasicVarEntity("v1", value1);
        value1.setEntityList(null);
        var solution = new TestdataBasicVarSolution(List.of(entity1), List.of(value1));
        Assertions
                .assertThatCode(
                        () -> SolutionManager.updateShadowVariables(TestdataBasicVarSolution.class, entity1, value1))
                .hasMessageContaining(
                        "The entity", "has a variable (value) with value",
                        "which has a sourceVariableName variable (entityList) which is null.");
        Assertions
                .assertThatCode(
                        () -> SolutionManager.updateShadowVariables(solution))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void unsupportedShadowVariableType() {
        var shadowVariableHelper = ShadowVariableUpdateHelper.<TestdataBasicVarSolution> create(BASIC);
        var value1 = new TestdataBasicVarValue("v1", Duration.ofSeconds(10));
        var entity1 = new TestdataBasicVarEntity("e1", value1);
        assertThatCode(() -> shadowVariableHelper.updateShadowVariables(TestdataBasicVarSolution.class, entity1, value1))
                .hasMessageContaining(
                        "The following shadow variable types are not currently supported ([CUSTOM_LISTENER, CASCADING_UPDATE, DECLARATIVE])");
    }

    @Test
    void updateBasicShadowVariables() {
        var value1 = new TestdataBasicVarValue("v1", Duration.ofSeconds(10));
        var value2 = new TestdataBasicVarValue("v2", Duration.ofSeconds(20));
        var entity1 = new TestdataBasicVarEntity("e1", value1);
        var entity2 = new TestdataBasicVarEntity("e2", value2);
        var entity3 = new TestdataBasicVarEntity("e3", value1);
        SolutionManager.updateShadowVariables(TestdataBasicVarSolution.class, entity1, entity2, entity3, value1, value2);
        assertThat(value1.getEntityList()).containsExactly(entity1, entity3);
        assertThat(value2.getEntityList()).containsExactly(entity2);
        assertThat(value1.getStartTime()).isEqualTo(TestdataBasicVarValue.DEFAULT_TIME.plusDays(value1.getEntityList().size()));
        assertThat(value1.getEndTime()).isEqualTo(value1.getStartTime().plus(value1.getDuration()));
        assertThat(value2.getStartTime()).isEqualTo(TestdataBasicVarValue.DEFAULT_TIME.plusDays(value2.getEntityList().size()));
        assertThat(value2.getEndTime()).isEqualTo(value2.getStartTime().plus(value2.getDuration()));
    }

    @Test
    void updateBasicShadowVariablesOnlyPlanningEntity() {
        var value1 = new TestdataBasicVarValue("v1", Duration.ofDays(10));
        var value2 = new TestdataBasicVarValue("v2", Duration.ofDays(20));
        var entity1 = new TestdataBasicVarEntity("e1", value1);
        var entity2 = new TestdataBasicVarEntity("e2", value2);
        SolutionManager.updateShadowVariables(TestdataBasicVarSolution.class, entity1, entity2);
        assertThat(entity1.getDurationInDays()).isEqualTo(value1.getDuration().toDays());
        assertThat(entity2.getDurationInDays()).isEqualTo(value2.getDuration().toDays());
    }

    @Test
    void solutionUpdateBasicShadowVariables() {
        var value1 = new TestdataBasicVarValue("v1", Duration.ofSeconds(10));
        var value2 = new TestdataBasicVarValue("v2", Duration.ofSeconds(20));
        var entity1 = new TestdataBasicVarEntity("e1", value1);
        var entity2 = new TestdataBasicVarEntity("e2", value2);
        var entity3 = new TestdataBasicVarEntity("e3", value1);
        var solution = new TestdataBasicVarSolution();
        solution.setEntities(List.of(entity1, entity2, entity3));
        solution.setValues(List.of(value1, value2));
        SolutionManager.updateShadowVariables(solution);
        assertThat(value1.getEntityList()).containsExactly(entity1, entity3);
        assertThat(value2.getEntityList()).containsExactly(entity2);
        assertThat(value1.getStartTime()).isEqualTo(TestdataBasicVarValue.DEFAULT_TIME.plusDays(value1.getEntityList().size()));
        assertThat(value1.getEndTime()).isEqualTo(value1.getStartTime().plus(value1.getDuration()));
        assertThat(value2.getStartTime()).isEqualTo(TestdataBasicVarValue.DEFAULT_TIME.plusDays(value2.getEntityList().size()));
        assertThat(value2.getEndTime()).isEqualTo(value2.getStartTime().plus(value2.getDuration()));
    }

    @Test
    void updateChainedShadowVariables() {
        var value1 = new TestdataChainedVarValue("v1", Duration.ofDays(10));
        var value2 = new TestdataChainedVarValue("v2", Duration.ofDays(20));
        var entity1 = new TestdataChainedVarEntity("e1", value1);
        var entity2 = new TestdataChainedVarEntity("e2", value2);
        SolutionManager.updateShadowVariables(TestdataChainedVarSolution.class, entity1, entity2, value1, value2);
        assertThat(value1.getNext()).isSameAs(entity1);
        assertThat(value2.getNext()).isSameAs(entity2);
        assertThat(value1.getStartTime()).isEqualTo(TestdataBasicVarValue.DEFAULT_TIME.plusDays(1));
        assertThat(value1.getEndTime()).isEqualTo(value1.getStartTime().plus(value1.getDuration()));
        assertThat(value2.getStartTime()).isEqualTo(TestdataBasicVarValue.DEFAULT_TIME.plusDays(1));
        assertThat(value2.getEndTime()).isEqualTo(value2.getStartTime().plus(value2.getDuration()));
        assertThat(entity1.getDurationInDays()).isEqualTo(value1.getDuration().toDays());
        assertThat(entity2.getDurationInDays()).isEqualTo(value2.getDuration().toDays());
    }

    @Test
    void solutionUpdateChainedShadowVariables() {
        var value1 = new TestdataChainedVarValue("v1", Duration.ofSeconds(10));
        var value2 = new TestdataChainedVarValue("v2", Duration.ofSeconds(20));
        var entity1 = new TestdataChainedVarEntity("e1", value1);
        var entity2 = new TestdataChainedVarEntity("e2", value2);
        var solution = new TestdataChainedVarSolution();
        solution.setEntities(List.of(entity1, entity2));
        solution.setValues(List.of(value1, value2));
        SolutionManager.updateShadowVariables(solution);
        assertThat(value1.getNext()).isSameAs(entity1);
        assertThat(value2.getNext()).isSameAs(entity2);
        assertThat(value1.getStartTime()).isEqualTo(TestdataBasicVarValue.DEFAULT_TIME.plusDays(1));
        assertThat(value1.getEndTime()).isEqualTo(value1.getStartTime().plus(value1.getDuration()));
        assertThat(value2.getStartTime()).isEqualTo(TestdataBasicVarValue.DEFAULT_TIME.plusDays(1));
        assertThat(value2.getEndTime()).isEqualTo(value2.getStartTime().plus(value2.getDuration()));
    }
}
