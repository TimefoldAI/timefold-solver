package ai.timefold.solver.service.quarkus.deployment.defaults;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Optional;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.service.definition.api.ModelInput;
import ai.timefold.solver.service.definition.api.ModelOutput;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.api.domain.ModelConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractTrivialModelConvertorTest {

    private AbstractTrivialModelConvertor<SimpleScore, TestdataModel, EmptyModelConfigOverrides, TestdataModel, TestdataModel> convertor;
    private TestdataModel model;
    private ModelConfig<EmptyModelConfigOverrides> modelConfig;

    @BeforeEach
    void setUp() {
        convertor = new AbstractTrivialModelConvertor<>() {
        };
        model = new TestdataModel();
        modelConfig = ModelConfig.empty();
    }

    @Test
    void toSolverModelRejectsNullArguments() {
        assertThatNullPointerException()
                .isThrownBy(() -> convertor.toSolverModel(null, modelConfig, Optional.empty()))
                .withMessage("modelInput");
        assertThatNullPointerException()
                .isThrownBy(() -> convertor.toSolverModel(model, null, Optional.empty()))
                .withMessage("modelConfig");
        assertThatNullPointerException()
                .isThrownBy(() -> convertor.toSolverModel(model, modelConfig, null))
                .withMessage("lastModelOutput");
    }

    @Test
    void toModelOutputRejectsNullSolverModel() {
        assertThatNullPointerException()
                .isThrownBy(() -> convertor.toModelOutput(null))
                .withMessage("solverModel");
    }

    @Test
    void applyOutputToInputRejectsNullArguments() {
        assertThatNullPointerException()
                .isThrownBy(() -> convertor.applyOutputToInput(null, model))
                .withMessage("modelInput");
        assertThatNullPointerException()
                .isThrownBy(() -> convertor.applyOutputToInput(model, null))
                .withMessage("modelOutput");
    }

    @Test
    void applyOutputToInputReturnsModelOutputWhenTypesMatch() {
        TestdataModel modelOutput = new TestdataModel();

        assertThat(convertor.applyOutputToInput(model, modelOutput)).isSameAs(modelOutput);
    }

    @Test
    void toSolverModelUsesEmptyOptionalWhenNoPreviousOutput() {
        assertThat(convertor.toSolverModel(model, modelConfig, Optional.empty())).isSameAs(model);
    }

    private static final class TestdataModel implements ModelInput, ModelOutput, SolverModel<SimpleScore> {

        @Override
        public SimpleScore getScore() {
            return null;
        }

        @Override
        public ConstraintWeightOverrides<SimpleScore> getConstraintWeightOverrides() {
            return null;
        }
    }
}
