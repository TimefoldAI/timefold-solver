package ai.timefold.solver.jackson.impl.domain.solution;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfIterator;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCode;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.jackson.impl.testdata.domain.JacksonTestdataEntity;
import ai.timefold.solver.jackson.impl.testdata.domain.JacksonTestdataSolution;
import ai.timefold.solver.jackson.impl.testdata.domain.JacksonTestdataValue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JacksonSolutionFileIOTest {

    private static File solutionTestDir;

    @BeforeAll
    static void setup() {
        solutionTestDir = new File("target/solutionTest/");
        solutionTestDir.mkdirs();
    }

    @Test
    void readAndWrite() {
        JacksonSolutionFileIO<JacksonTestdataSolution> solutionFileIO =
                new JacksonSolutionFileIO<>(JacksonTestdataSolution.class);
        File file = new File(solutionTestDir, "testdataSolution.json");

        JacksonTestdataSolution original = new JacksonTestdataSolution("s1");
        JacksonTestdataValue originalV1 = new JacksonTestdataValue("v1");
        original.setValueList(Arrays.asList(originalV1, new JacksonTestdataValue("v2")));
        original.setEntityList(Arrays.asList(
                new JacksonTestdataEntity("e1"), new JacksonTestdataEntity("e2", originalV1), new JacksonTestdataEntity("e3")));
        original.setScore(SimpleScore.of(-321));
        solutionFileIO.write(original, file);
        JacksonTestdataSolution copy = solutionFileIO.read(file);

        assertThat(copy).isNotSameAs(original);
        assertCode("s1", copy);
        assertAllCodesOfIterator(copy.getValueList().iterator(), "v1", "v2");
        assertAllCodesOfIterator(copy.getEntityList().iterator(), "e1", "e2", "e3");
        JacksonTestdataValue copyV1 = copy.getValueList().get(0);
        JacksonTestdataEntity copyE2 = copy.getEntityList().get(1);
        assertCode("v1", copyE2.getValue());
        assertThat(copyE2.getValue()).isSameAs(copyV1);
        assertThat(copy.getScore()).isEqualTo(SimpleScore.of(-321));
    }

}
