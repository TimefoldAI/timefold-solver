package ai.timefold.solver.core.preview.api.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

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
        Assertions.assertThatCode(() -> SolutionManager.updateShadowVariables(TestdataShadowedFullSolution.class))
                .hasMessageContaining("The entity array cannot be empty.");
    }

    @Test
    void invalidCustomListener() {
        var value1 = new TestdataShadowedFullValue("v1");
        var entity1 = new TestdataShadowedFullEntity("e1");
        Assertions
                .assertThatCode(
                        () -> SolutionManager.updateShadowVariables(TestdataShadowedFullSolution.class, entity1, value1))
                .hasMessageContaining("Custom shadow variable descriptors are not supported");
    }

    @Test
    void invalidInverseRelation() {
        var value1 = new TestdataBasicVarValue("c1", Duration.ZERO);
        var entity1 = new TestdataBasicVarEntity("v1", value1);
        value1.setEntityList(null);
        Assertions
                .assertThatCode(
                        () -> SolutionManager.updateShadowVariables(TestdataBasicVarSolution.class, entity1, value1))
                .hasMessageContaining(
                        "The entity", "has a variable (value) with value",
                        "which has a sourceVariableName variable (entityList) which is null.");
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
    void updateChainedShadowVariables() {
        var value1 = new TestdataChainedVarValue("v1", Duration.ofSeconds(10));
        var value2 = new TestdataChainedVarValue("v2", Duration.ofSeconds(20));
        var entity1 = new TestdataChainedVarEntity("e1", value1);
        var entity2 = new TestdataChainedVarEntity("e2", value2);
        SolutionManager.updateShadowVariables(TestdataChainedVarSolution.class, entity1, entity2, value1, value2);
        assertThat(value1.getNext()).isSameAs(entity1);
        assertThat(value2.getNext()).isSameAs(entity2);
        assertThat(value1.getStartTime()).isEqualTo(TestdataBasicVarValue.DEFAULT_TIME.plusDays(1));
        assertThat(value1.getEndTime()).isEqualTo(value1.getStartTime().plus(value1.getDuration()));
        assertThat(value2.getStartTime()).isEqualTo(TestdataBasicVarValue.DEFAULT_TIME.plusDays(1));
        assertThat(value2.getEndTime()).isEqualTo(value2.getStartTime().plus(value2.getDuration()));
    }
}
