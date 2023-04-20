package ai.timefold.solver.constraint.drl;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class DrlScoreDirectorFactoryServiceTest {

    private ScoreDirectorFactory<TestdataSolution> buildScoreDirectoryFactory(ScoreDirectorFactoryConfig config) {
        Supplier<AbstractScoreDirectorFactory<TestdataSolution, SimpleScore>> supplier =
                new DrlScoreDirectorFactoryService<TestdataSolution, SimpleScore>()
                        .buildScoreDirectorFactory(getClass().getClassLoader(),
                                TestdataSolution.buildSolutionDescriptor(), config, EnvironmentMode.REPRODUCIBLE);
        if (supplier == null) {
            return null;
        }
        return supplier.get();
    }

    @Test
    void invalidDrlResource_throwsException() {
        ScoreDirectorFactoryConfig config = new ScoreDirectorFactoryConfig()
                .withScoreDrls("ai/timefold/solver/constraint/drl/invalidDroolsConstraints.drl");
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> buildScoreDirectoryFactory(config))
                .withMessageContaining("scoreDrl")
                .withRootCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void nonExistingDrlResource_throwsException() {
        ScoreDirectorFactoryConfig config = new ScoreDirectorFactoryConfig()
                .withScoreDrls("nonExisting.drl");
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> buildScoreDirectoryFactory(config))
                .withMessageContaining("scoreDrl");
    }

    @Test
    void nonExistingDrlFile_throwsException() {
        ScoreDirectorFactoryConfig config = new ScoreDirectorFactoryConfig()
                .withScoreDrlFiles(new File("nonExisting.drl"));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> buildScoreDirectoryFactory(config))
                .withMessageContaining("scoreDrl");
    }

}
