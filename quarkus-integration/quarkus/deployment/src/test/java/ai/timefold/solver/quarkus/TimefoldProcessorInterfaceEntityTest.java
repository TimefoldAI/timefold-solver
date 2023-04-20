package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.quarkus.testdata.interfaceentity.domain.TestdataInterfaceEntity;
import ai.timefold.solver.quarkus.testdata.interfaceentity.domain.TestdataInterfaceEntityImplementation;
import ai.timefold.solver.quarkus.testdata.interfaceentity.domain.TestdataInterfaceEntitySolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorInterfaceEntityTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addPackages(true, "ai.timefold.solver.quarkus.testdata.interfaceentity"));

    @Inject
    SolverFactory<TestdataInterfaceEntitySolution> solverFactory;

    @Test
    void buildSolver() {
        TestdataInterfaceEntitySolution problem = new TestdataInterfaceEntitySolution();
        List<TestdataInterfaceEntity> entityList = IntStream.range(1, 3)
                .mapToObj(i -> new TestdataInterfaceEntityImplementation())
                .collect(Collectors.toList());

        problem.setValueList(IntStream.range(0, 3)
                .boxed()
                .collect(Collectors.toList()));
        problem.setEntityList(entityList);

        TestdataInterfaceEntitySolution solution = solverFactory.buildSolver().solve(problem);
        assertNotNull(solution);

        assertEquals(entityList.size(), solution.getEntityList().size());
        for (int i = 0; i < entityList.size(); i++) {
            assertNotSame(entityList.get(i), solution.getEntityList().get(i));
        }
    }
}
